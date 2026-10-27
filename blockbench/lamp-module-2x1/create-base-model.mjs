// Bootstrap of the v2.1 lamp module 2x1. Writes lamp_module_2x1.bbmodel.
// Same nixie-style language as the 1x1 (socket, glass tube without a bottom, filament), as one long
// horizontal tube along local X; the renderer turns it 90° for a 1x2 footprint.
// Textures are shared with the 1x1 (../lamp-module-1x1/lamp-textures.mjs, `long` regions).
// Modelled with the table facing up: rests on Y 0, centred on X/Z 8, 10 x 4 footprint (guide §9.2).
// Refine in Blockbench afterwards; do NOT rerun over a refined source.
import { texture, face, cube, group, writeProject, px } from '../_tools/bbtools.mjs';
import { socket, glass, filament, SOCKET_UV, GLASS_UV, FILAMENT_UV } from '../lamp-module-1x1/lamp-textures.mjs';

const textures = [
  texture('lamp_socket', 16, { source: socket(), particle: true }),    // 0
  texture('lamp_glass_red', 16, { source: glass() }),                 // 1
  texture('lamp_filament_off', 16, { source: filament(false) }),      // 2
  texture('lamp_filament_on', 16, { source: filament(true) }),        // 3 (variant only)
];
const uv = region => px(16, region);
// Long faces (north/south) run along X; the ends (east/west) reuse the 1x1 regions.
const sides = (long, end, tex) => ({
  north: face(uv(long), tex), south: face(uv(long), tex), east: face(uv(end), tex), west: face(uv(end), tex),
});
const elements = [
  cube('socket', [3, 0, 6], [13, 1, 10], { ...sides(SOCKET_UV.longSide, SOCKET_UV.side, 0),
    up: face(uv(SOCKET_UV.longTop), 0), down: face(uv(SOCKET_UV.longBottom), 0) }, 1),
  // No down faces: they would sit on the socket top, which stays visible through the glass.
  cube('glass', [3, 1, 6], [13, 4, 10],
    { ...sides(GLASS_UV.longSide, GLASS_UV.side, 1), up: face(uv(GLASS_UV.longTop), 1) }, 3),
  cube('filament', [4, 1, 7], [12, 3, 9],
    { ...sides(FILAMENT_UV.longSide, FILAMENT_UV.side, 2), up: face(uv(FILAMENT_UV.longTop), 2) }, 6),
];
const groups = [group('socket', [8, 0, 8], elements.slice(0, 1)), group('tube', [8, 1, 8], elements.slice(1), 3)];
writeProject(new URL('lamp_module_2x1.bbmodel', import.meta.url),
  { name: 'lamp_module_2x1', elements, groups, textures });
console.log(`Wrote lamp_module_2x1.bbmodel: ${elements.length} cubes, ${textures.length} textures.`);
