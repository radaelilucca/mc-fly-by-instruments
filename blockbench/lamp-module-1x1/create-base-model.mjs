// Bootstrap of the v2.1 lamp module 1x1. Writes lamp_module_1x1.bbmodel.
// Inspired by Create's nixie tube and train signal lamp: opaque socket, translucent glass tube
// without a bottom face, and a filament mesh seen through the glass. Textures: lamp-textures.mjs.
// Modelled with the table facing up: rests on Y 0, centred on X/Z 8, 4 x 4 footprint (guide §9.2).
// Refine in Blockbench afterwards; do NOT rerun over a refined source.
import { texture, face, cube, group, writeProject, px } from '../_tools/bbtools.mjs';
import { socket, glass, filament, SOCKET_UV, GLASS_UV, FILAMENT_UV } from './lamp-textures.mjs';

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
    { ...sides(SOCKET_UV.side, 0), up: face(uv(SOCKET_UV.top), 0), down: face(uv(SOCKET_UV.bottom), 0) }, 1),
  // No down faces: they would sit on the socket top, which stays visible through the glass.
  cube('glass', [6, 1, 6], [10, 4, 10], { ...sides(GLASS_UV.side, 1), up: face(uv(GLASS_UV.top), 1) }, 3),
  cube('filament', [7, 1, 7], [9, 3, 9], { ...sides(FILAMENT_UV.side, 2), up: face(uv(FILAMENT_UV.top), 2) }, 6),
];
const groups = [group('socket', [8, 0, 8], elements.slice(0, 1)), group('tube', [8, 1, 8], elements.slice(1), 3)];
writeProject(new URL('lamp_module_1x1.bbmodel', import.meta.url),
  { name: 'lamp_module_1x1', elements, groups, textures });
console.log(`Wrote lamp_module_1x1.bbmodel: ${elements.length} cubes, ${textures.length} textures.`);
