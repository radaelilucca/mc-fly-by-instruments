// Bootstrap of the Wire Support (Avionics Wire). Writes wire_support.bbmodel.
// Industrial iron (guide §7: the heavy structural part), no brass: it carries no data.
// Modelled facing UP (host block below). A cleat: base plate, four corner posts and a cap, so the wire
// passes *through* a ring open on all four sides and is held on any face, walls and ceilings included.
// The wire centre runs at y = 2 (WireAttachPoints.SUPPORT_WIRE), between base and cap, with lanes 2 px
// apart inside the 6 px gap between the posts. Shape: WireSupportBlock.PARTS (the hull).
// Refine in Blockbench afterwards; do NOT rerun over a refined source.
import { texture, face, cube, group, writeProject, px, canvas, blit, checkMap } from '../_tools/bbtools.mjs';

const uv = (x, y, w, h) => px(16, [x, y, x + w, y + h]);
const img = canvas(16, 16, 'p');
const parts = [
  // Cap: concentric squares (guide §3 vocabulary), light top-left.
  ['cap_top', 0, 0, ['WWWWWWWv', 'Wvqqqqvp', 'Wqvvvvqp', 'Wqvqqvqp', 'Wqvqqvqp', 'Wqvvvvqp', 'Wvqqqqvp', 'vppppppo']],
  ['cap_bottom', 8, 0, ['pooooooo', 'onnnnnnn', 'onnnnnnn', 'onnnnnnn', 'onnnnnnn', 'onnnnnnn', 'onnnnnnn', 'onnnnnnn']],
  ['cap_side', 0, 8, ['vqqqqqqp']],
  ['base_side', 0, 9, ['qppppppo']],
  ['base_top', 8, 8, ['qppppppo', 'pooooooo', 'pooooooo', 'pooooooo', 'pooooooo', 'pooooooo', 'pooooooo', 'onnnnnnn']],
  ['post_side', 0, 10, ['v', 'p']],
];
for (const [name, x, y, map] of parts) blit(img, checkMap(name, map, map[0].length, map.length), x, y);
const textures = [texture('wire_support', 16, { source: img, particle: true })];

const sides = (x, y, w, h) => Object.fromEntries(['north', 'east', 'south', 'west'].map(s => [s, face(uv(x, y, w, h), 0)]));
// Faces against the host face or hidden inside another part are left out (no coplanar caps).
const base = cube('base', [4, 0, 4], [12, 1, 12], { up: face(uv(8, 8, 8, 8), 0), ...sides(0, 9, 8, 1) }, 3);
const posts = [[4, 4], [11, 4], [4, 11], [11, 11]].map(([x, z], i) =>
  cube(`post_${i}`, [x, 1, z], [x + 1, 3, z + 1], sides(0, 10, 1, 2), 3));
const cap = cube('cap', [4, 3, 4], [12, 4, 12], {
  up: face(uv(0, 0, 8, 8), 0), down: face(uv(8, 0, 8, 8), 0), ...sides(0, 8, 8, 1),
}, 3);

const elements = [base, ...posts, cap];
writeProject(new URL('wire_support.bbmodel', import.meta.url), {
  name: 'wire_support', elements, groups: [group('support', [8, 0, 8], elements)], textures,
});
console.log(`Wrote wire_support.bbmodel: ${elements.length} cubes.`);
