// Bootstrap of the v2 Terrain Scanner (Basic): Create andesite tier. Writes terrain_scanner_basic.bbmodel.
// Model faces NORTH (front = z 0). Refine in Blockbench afterwards; do NOT rerun over a refined source.
// Guide: docs/visual-design-guide.md. Differentiation vs Advanced/Altimeter: see README.md.
import { canvas, blit, checkMap, texture, face, cube, group, writeProject, px } from '../_tools/bbtools.mjs';

// --- Pixel maps --------------------------------------------------------------------------
// Andesite casing frame (outer dark, inner bevel lit top/left) around vertical medium-wood planks.
function frame(w, h, interior) {
  const rows = [];
  for (let y = 0; y < h; y++) {
    let row = '';
    for (let x = 0; x < w; x++) {
      const edgeX = x === 0 || x === w - 1, edgeY = y === 0 || y === h - 1;
      if (edgeX && edgeY) row += 'A';
      else if (edgeX || edgeY) row += 'B';
      else if (y === 1 || x === 1) row += (x === w - 2 || y === h - 2) ? 'D' : 'F';
      else if (y === h - 2 || x === w - 2) row += 'C';
      else row += interior(x, y);
    }
    rows.push(row);
  }
  return rows;
}
// Vertical planks: 3 px plank + 1 px seam, a darker knot mark every few rows.
const planks = (x, y) => (x % 4 === 1 ? 'h' : ((x * 7 + y * 3) % 11 === 0 ? 'j' : (x % 4 === 2 ? 'l' : 'k')));
// Vertical redstone slot (2 x 6) in a dark frame, centred horizontally.
function withSlot(map, cx, top, lit) {
  const rows = map.map(r => [...r]);
  for (let y = top; y < top + 8; y++) for (let x = cx - 2; x < cx + 2; x++) rows[y][x] = 'g';
  for (let y = top + 1; y < top + 7; y++) {
    rows[y][cx - 1] = lit ? (y % 3 === 0 ? 'U' : 'T') : (y % 2 ? 't' : 's');
    rows[y][cx] = lit ? 'R' : 'r';
  }
  return rows.map(r => r.join(''));
}
const SIDE = lit => checkMap('side', withSlot(frame(12, 16, planks), 6, 4, lit), 12, 16);     // east/west: z12 x y16
// Rear: data socket (dark metal ring) at the centre, no slot.
const REAR = (() => {
  const rows = frame(16, 16, planks).map(r => [...r]);
  const socket = ['xxxx', 'xwwz', 'xwwz', 'zzzz'];
  socket.forEach((r, y) => [...r].forEach((k, x) => { rows[6 + y][6 + x] = k; }));
  return checkMap('rear', rows.map(r => r.join('')), 16, 16);
})();
// Front: frame + corner rivets; the centre 10 x 10 is covered by the nozzle.
const FRONT = (() => {
  const rows = frame(16, 16, planks).map(r => [...r]);
  for (const [x, y] of [[2, 2], [13, 2], [2, 13], [13, 13]]) rows[y][x] = 'E';
  return checkMap('front', rows.map(r => r.join('')), 16, 16);
})();
const CAP = checkMap('cap', frame(16, 12, planks), 16, 12);                                     // up/down: x16 x z12

// Optics texture (32 x 32): stepped nozzle ring (andesite), tip (dark metal), lens (dark glass).
// Stepped telescope read: mid-andesite outer ring, light andesite inner ring, dark lens.
const RING_FACE = checkMap('ring face', ['ADDDDDDDDA', ...Array(8).fill('DxxxxxxxxB'), 'ABBBBBBBBA'], 10, 10);
const RING_TOP = checkMap('ring top', ['DDDDDDDDDD', 'CCCCCCCCCC'], 10, 2);     // up/down: x10 x z2
const RING_SIDE = checkMap('ring side', Array(10).fill('DC'), 2, 10);            // east/west: z2 x y10
const TIP_FACE = checkMap('tip face', ['FFFFFFFE', ...Array(6).fill('FxxxxxxC'), 'ECCCCCCC'], 8, 8);
const TIP_TOP = checkMap('tip top', ['FFFFFFFF'], 8, 1);
const TIP_SIDE = checkMap('tip side', Array(8).fill('E'), 1, 8);
const LENS_FACE = checkMap('lens face', ['OOOOOO', 'OVVMMO', 'OVLYMO', 'OMLLMO', 'OMMMMO', 'OOOOOO'], 6, 6);
const LENS_TOP = checkMap('lens top', ['NNNNNN'], 6, 1);
const LENS_SIDE = checkMap('lens side', Array(6).fill('N'), 1, 6);

// --- Texture assembly (pixels = model units, 1:1) ----------------------------------------
const AT = { side: [0, 0, 12, 16], rear: [12, 0, 28, 16], front: [0, 16, 16, 32], cap: [16, 16, 32, 28] };
function atlas(lit) {
  const img = canvas(32, 32, 'B');
  blit(img, SIDE(lit), 0, 0); blit(img, REAR, 12, 0); blit(img, FRONT, 0, 16); blit(img, CAP, 16, 16);
  return img;
}
const OP = {
  ringFace: [0, 0, 10, 10], ringTop: [0, 10, 10, 12], ringSide: [10, 0, 12, 10],
  tipFace: [12, 0, 20, 8], tipTop: [12, 8, 20, 9], tipSide: [20, 0, 21, 8],
  lensFace: [21, 0, 27, 6], lensTop: [21, 6, 27, 7], lensSide: [27, 0, 28, 6],
};
function optics() {
  const img = canvas(32, 32, 'x');
  blit(img, RING_FACE, 0, 0); blit(img, RING_TOP, 0, 10); blit(img, RING_SIDE, 10, 0);
  blit(img, TIP_FACE, 12, 0); blit(img, TIP_TOP, 12, 8); blit(img, TIP_SIDE, 20, 0);
  blit(img, LENS_FACE, 21, 0); blit(img, LENS_TOP, 21, 6); blit(img, LENS_SIDE, 27, 0);
  return img;
}

// --- Geometry (front = NORTH) -------------------------------------------------------------
const textures = [
  texture('terrain_scanner', 32, { source: atlas(false), particle: true }),   // 0
  texture('terrain_scanner_powered', 32, { source: atlas(true) }),           // 1 (variant only)
  texture('terrain_scanner_optics', 32, { source: optics() }),               // 2
];
const box = (n, s, e, w, u, d, t) => ({ north: face(n, t), south: face(s, t), east: face(e, t),
  west: face(w, t), up: face(u, t), down: face(d, t) });
const elements = [
  cube('body', [0, 0, 4], [16, 16, 16], box(AT.front, AT.rear, AT.side, AT.side, AT.cap, AT.cap, 0), 1),
  cube('nozzle_ring', [3, 3, 2], [13, 13, 4],
    box(OP.ringFace, OP.ringFace, OP.ringSide, OP.ringSide, OP.ringTop, OP.ringTop, 2), 2),
  cube('nozzle_tip', [4, 4, 1], [12, 12, 2],
    box(OP.tipFace, OP.tipFace, OP.tipSide, OP.tipSide, OP.tipTop, OP.tipTop, 2), 4),
  // Name kept for code/tests: front centre (8, 8, 0) faces the cast origin.
  cube('forward_antenna', [5, 5, 0], [11, 11, 1],
    box(OP.lensFace, OP.lensFace, OP.lensSide, OP.lensSide, OP.lensTop, OP.lensTop, 2), 6),
];
const groups = [group('body', [8, 8, 8], elements.slice(0, 1)), group('nozzle', [8, 8, 0], elements.slice(1), 6)];
writeProject(new URL('terrain_scanner_basic.bbmodel', import.meta.url),
  { name: 'terrain_scanner', elements, groups, textures });
console.log(`Wrote terrain_scanner_basic.bbmodel: ${elements.length} cubes, ${textures.length} textures.`);
