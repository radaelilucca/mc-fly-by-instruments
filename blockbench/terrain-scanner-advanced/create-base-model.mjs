// Bootstrap of the v2 Advanced Terrain Scanner: Create brass ("smart") tier over industrial iron.
// Writes terrain_scanner_advanced.bbmodel. Model faces NORTH (front = z 0). Refine in Blockbench;
// do NOT rerun over a refined source. Guide: docs/visual-design-guide.md; differentiation: README.md.
import { canvas, blit, checkMap, mirror, texture, face, cube, group, writeProject } from '../_tools/bbtools.mjs';

// --- Pixel maps --------------------------------------------------------------------------
// Side (east/west = LEFT/RIGHT output channels): brass lid band, iron body between brass posts,
// framed horizontal redstone slot, brass foot rim. 12 (z) x 16 (y).
const side = (top, bottom) => checkMap('side', [
  '123333333321',
  '258888877752',
  '357777777653',
  '356666666543',
  '233333333332',
  '1nnnnnnnnnn1',
  '32pqppqppq12',
  '32nnnnnnnn12',
  '32n' + top + 'n12',
  '32n' + bottom + 'n12',
  '32nnnnnnnn12',
  '32pqppqppq12',
  '32oooooooo12',
  '32nnnnnnnn12',
  '256666666532',
  '122222222221',
], 12, 16);
const SIDE_OFF = side('tstsst', 'rrsrrs');
const SIDE_ON = side('TUTTUT', 'RRSRRS');
// Rear (no output): brass band, iron body, central data socket, brass rim. 16 x 16.
const REAR = checkMap('rear', [
  '1233333333333321',
  '2588888888777752',
  '3577777777777653',
  '3566666666666543',
  '2333333333333332',
  '1nnnnnnnnnnnnnn1',
  '32pqppqxxxxpqp12',
  '32ooooxwwzxooo12',
  '32ppqpxwwzxqpp12',
  '32ooooxzzzxooo12',
  '32pqppqxxxxpqp12',
  '32oooooooooooo12',
  '32pqppqppqppqp12',
  '32nnnnnnnnnnnnn2'.slice(0, 15) + '2',
  '2566666666666532',
  '1222222222222221',
], 16, 16);
// Front (FRONT output channel): the lens bar covers rows 4..11; a slot sits between bar and rim.
const front = (slot) => checkMap('front', [
  '1233333333333321',
  '2588888888777752',
  '3577777777777653',
  '3566666666666543',
  '3nnnnnnnnnnnnnn2',
  '3nnnnnnnnnnnnnn2',
  '3nnnnnnnnnnnnnn2',
  '3nnnnnnnnnnnnnn2',
  '3nnnnnnnnnnnnnn2',
  '3nnnnnnnnnnnnnn2',
  '3nnnnnnnnnnnnnn2',
  '3nnnnnnnnnnnnnn2',
  '32pqnnnnnnnnqp12',
  '32pqn' + slot + 'nqp12',
  '2566666666666532',
  '1222222222222221',
], 16, 16);
const FRONT_OFF = front('tsstst');
const FRONT_ON = front('TUTTUT');
// Cap (top/bottom): brass plate with concentric rectangles. 16 (x) x 12 (z).
const CAP = checkMap('cap', [
  '1233333333333321',
  '2777777777777752',
  '3755555555555643',
  '3753333333335643',
  '3753777777635643',
  '3753756665635643',
  '3753756665635643',
  '3753766666635643',
  '3753333333335643',
  '3766666666666643',
  '3444444444444443',
  '1222222222222221',
], 16, 12);

// Optics texture: brass lens bar + three dark lenses (centre protrudes most, sides step back).
// Full-width brass bar; brass dividers at x4 and x11 separate the LEFT / FRONT / RIGHT lenses.
const BAR_FACE = checkMap('bar face', ['5777777777777773', ...Array(6).fill('7nnn6nnnnnn4nnn3'), '5333333333333332'], 16, 8);
const BAR_TOP = checkMap('bar top', ['7888888888888887', '5666666666666665'], 16, 2); // up/down: x16 x z2
const BAR_SIDE = checkMap('bar side', Array(8).fill('75'), 2, 8);                  // east/west: z2 x y8
const CENTRE_FACE = checkMap('centre face', ['OOOOOO', 'OVMMMO', 'OMQQMO', 'OQZZQO', 'OMMMMO', 'OOOOOO'], 6, 6);
const CENTRE_TOP = checkMap('centre top', ['NNNNNN', 'MMMMMM'], 6, 2);               // up/down: x6 x z2
const CENTRE_SIDE = checkMap('centre side', Array(6).fill('NM'), 2, 6);              // east/west: z2 x y6
const WING_FACE = checkMap('wing face', ['OOO', 'OVM', 'OMM', 'OZQ', 'OMM', 'OOO'], 3, 6);
const WING_TOP = checkMap('wing top', ['NNN'], 3, 1);
const WING_SIDE = checkMap('wing side', Array(6).fill('N'), 1, 6);

// --- Texture assembly (1:1) ---------------------------------------------------------------
const AT = { side: [0, 0, 12, 16], rear: [12, 0, 28, 16], front: [0, 16, 16, 32], cap: [16, 16, 32, 28] };
function atlas(lit) {
  const img = canvas(32, 32, '2');
  blit(img, lit ? SIDE_ON : SIDE_OFF, 0, 0); blit(img, REAR, 12, 0);
  blit(img, lit ? FRONT_ON : FRONT_OFF, 0, 16); blit(img, CAP, 16, 16);
  return img;
}
const OP = {
  barFace: [0, 0, 16, 8], barTop: [0, 8, 16, 10], barSide: [16, 0, 18, 8],
  centreFace: [18, 0, 24, 6], centreTop: [18, 6, 24, 8], centreSide: [24, 0, 26, 6],
  wingL: [26, 0, 29, 6], wingR: [29, 0, 32, 6], wingTop: [26, 6, 29, 7], wingSide: [18, 8, 19, 14],
};
function optics() {
  const img = canvas(32, 32, 'n');
  blit(img, BAR_FACE, 0, 0); blit(img, BAR_TOP, 0, 8); blit(img, BAR_SIDE, 16, 0);
  blit(img, CENTRE_FACE, 18, 0); blit(img, CENTRE_TOP, 18, 6); blit(img, CENTRE_SIDE, 24, 0);
  blit(img, WING_FACE, 26, 0); blit(img, mirror(WING_FACE), 29, 0); blit(img, WING_TOP, 26, 6); blit(img, WING_SIDE, 18, 8);
  return img;
}

// --- Geometry (front = NORTH) -------------------------------------------------------------
const textures = [
  texture('terrain_scanner_advanced', 32, { source: atlas(false), particle: true }),   // 0
  texture('terrain_scanner_advanced_powered', 32, { source: atlas(true) }),           // 1 (variant only)
  texture('terrain_scanner_advanced_optics', 32, { source: optics() }),               // 2
];
const box = (n, s, e, w, u, d, t) => ({ north: face(n, t), south: face(s, t), east: face(e, t),
  west: face(w, t), up: face(u, t), down: face(d, t) });
const elements = [
  cube('body', [0, 0, 4], [16, 16, 16], box(AT.front, AT.rear, AT.side, AT.side, AT.cap, AT.cap, 0), 1),
  cube('lens_bar', [0, 4, 2], [16, 12, 4], box(OP.barFace, OP.barFace, OP.barSide, OP.barSide, OP.barTop, OP.barTop, 2), 2),
  // Scanner's own left (facing NORTH) is WEST = low x, matching the LEFT output channel.
  cube('lens_left', [1, 5, 1], [4, 11, 2], box(OP.wingR, OP.wingR, OP.wingSide, OP.wingSide, OP.wingTop, OP.wingTop, 2), 4),
  cube('lens_right', [12, 5, 1], [15, 11, 2], box(OP.wingL, OP.wingL, OP.wingSide, OP.wingSide, OP.wingTop, OP.wingTop, 2), 4),
  // Name kept for code/tests: front centre (8, 8, 0) faces the cast origin.
  cube('forward_antenna', [5, 5, 0], [11, 11, 2],
    box(OP.centreFace, OP.centreFace, OP.centreSide, OP.centreSide, OP.centreTop, OP.centreTop, 2), 6),
];
const groups = [group('body', [8, 8, 8], elements.slice(0, 1)), group('lens_array', [8, 8, 0], elements.slice(1), 6)];
writeProject(new URL('terrain_scanner_advanced.bbmodel', import.meta.url),
  { name: 'terrain_scanner_advanced', elements, groups, textures });
console.log(`Wrote terrain_scanner_advanced.bbmodel: ${elements.length} cubes, ${textures.length} textures.`);
