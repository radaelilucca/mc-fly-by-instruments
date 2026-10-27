// Bootstrap of the v2.1 lamp module 1x1. Writes lamp_module_1x1.bbmodel.
// Inspired by Create's nixie tube and train signal lamp: opaque socket, translucent glass tube
// without a bottom face, and a filament mesh seen through the glass.
// Modelled with the table facing up: rests on Y 0, centred on X/Z 8, 4 x 4 footprint (guide §9.2).
// Refine in Blockbench afterwards; do NOT rerun over a refined source.
import { canvas, blit, checkMap, texture, face, cube, group, writeProject, px } from '../_tools/bbtools.mjs';

// --- Pixel maps (keys: bbtools PALETTE) ------------------------------------------------------
// Socket: dark metal like the nixie base (symmetric stripes); the brass rim on top shows through the glass.
const SOCKET_SIDE = checkMap('socket side', ['xzzx'], 4, 1);
const SOCKET_TOP = checkMap('socket top', ['6553', '5wx2', '5xw2', '3221'], 4, 4);   // seen through the glass
const SOCKET_BOTTOM = checkMap('socket bottom', ['wxxw', 'xyyx', 'xyyx', 'wxxw'], 4, 4);
// Glass: redstone tint. As in the nixie tube, the core stays clear (low alpha) so the filament shows,
// while the edges and the highlight column next to the lit edge are denser. Alpha keys: h, m, l.
const GLASS_SIDE = checkMap('glass side', ['SUTS', 'SUTR', 'RTSR'], 4, 3);
const GLASS_SIDE_ALPHA = checkMap('glass side alpha', ['mhlm', 'mhlm', 'mmlm'], 4, 3);
const GLASS_TOP = checkMap('glass top', ['SUTS', 'UTTR', 'TTSR', 'SRRR'], 4, 4);
const GLASS_TOP_ALPHA = checkMap('glass top alpha', ['mhmm', 'hllm', 'mllm', 'mmmm'], 4, 4);
const ALPHA = { h: 0xb0, m: 0x80, l: 0x48 };
// Filament: dark-metal mesh when off; redstone checker when on (train signal lamp).
const FILAMENT_OFF_SIDE = checkMap('filament off side', ['zx', 'xz'], 2, 2);
const FILAMENT_OFF_TOP = checkMap('filament off top', ['zz', 'zy'], 2, 2);
const FILAMENT_ON_SIDE = checkMap('filament on side', ['UT', 'TS'], 2, 2);
const FILAMENT_ON_TOP = checkMap('filament on top', ['UT', 'TU'], 2, 2);

// --- Textures (16 x 16, pixels = model units) ------------------------------------------------
const SO = { side: [0, 0, 4, 1], top: [4, 0, 8, 4], bottom: [8, 0, 12, 4] };
const GL = { side: [0, 0, 4, 3], top: [4, 0, 8, 4] };
const FI = { side: [0, 0, 2, 2], top: [2, 0, 4, 2] };
function socket() {
  const img = canvas(16, 16, 'w');
  blit(img, SOCKET_SIDE, 0, 0); blit(img, SOCKET_TOP, 4, 0); blit(img, SOCKET_BOTTOM, 8, 0);
  return img;
}
function glass() {
  const img = canvas(16, 16, 'R');
  blit(img, GLASS_SIDE, 0, 0); blit(img, GLASS_TOP, 4, 0);
  const alpha = Array.from({ length: 16 }, () => Array(16).fill(ALPHA.m));
  for (const [map, x0] of [[GLASS_SIDE_ALPHA, 0], [GLASS_TOP_ALPHA, 4]])
    map.forEach((row, y) => [...row].forEach((key, x) => { alpha[y][x0 + x] = ALPHA[key]; }));
  return img.map((row, y) => row.map((hex, x) => hex + alpha[y][x].toString(16).padStart(2, '0')));
}
function filament(lit) {
  const img = canvas(16, 16, lit ? 'T' : 'x');
  blit(img, lit ? FILAMENT_ON_SIDE : FILAMENT_OFF_SIDE, 0, 0);
  blit(img, lit ? FILAMENT_ON_TOP : FILAMENT_OFF_TOP, 2, 0);
  return img;
}

// --- Geometry --------------------------------------------------------------------------------
// Texture names are a contract with LampModuleRenderer, which sorts quads by sprite.
const textures = [
  texture('lamp_socket', 16, { source: socket(), particle: true }),    // 0
  texture('lamp_glass_red', 16, { source: glass() }),                 // 1
  texture('lamp_filament_off', 16, { source: filament(false) }),      // 2
  texture('lamp_filament_on', 16, { source: filament(true) }),        // 3 (variant only)
];
const uv = region => px(16, region);
const sides = (region, tex) => Object.fromEntries(['north', 'south', 'east', 'west'].map(s => [s, face(uv(region), tex)]));
const elements = [
  cube('socket', [6, 0, 6], [10, 1, 10],
    { ...sides(SO.side, 0), up: face(uv(SO.top), 0), down: face(uv(SO.bottom), 0) }, 1),
  // No down faces: they would sit on the socket top, which stays visible through the glass.
  cube('glass', [6, 1, 6], [10, 4, 10], { ...sides(GL.side, 1), up: face(uv(GL.top), 1) }, 3),
  cube('filament', [7, 1, 7], [9, 3, 9], { ...sides(FI.side, 2), up: face(uv(FI.top), 2) }, 6),
];
const groups = [group('socket', [8, 0, 8], elements.slice(0, 1)), group('tube', [8, 1, 8], elements.slice(1), 3)];
writeProject(new URL('lamp_module_1x1.bbmodel', import.meta.url),
  { name: 'lamp_module_1x1', elements, groups, textures });
console.log(`Wrote lamp_module_1x1.bbmodel: ${elements.length} cubes, ${textures.length} textures.`);
