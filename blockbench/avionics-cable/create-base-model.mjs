// Bootstrap of the v2.1 avionics cable. Writes avionics_cable.bbmodel.
// Industrial iron conduit (guide §5) with the brass kept to the connector collar only.
// Lead modelled SOUTH (z = 16 is the device side); integer coordinates, 1 texel per unit.
// Refine in Blockbench afterwards; do NOT rerun over a refined source.
import { texture, face, cube, group, writeProject, px, canvas, blit, checkMap } from '../_tools/bbtools.mjs';

const uv = (x, y, w, h) => px(16, [x, y, x + w, y + h]);
function atlas(fill, parts) {
  const img = canvas(16, 16, fill);
  for (const [name, x, y, map] of parts) blit(img, checkMap(name, map, map[0].length, map.length), x, y);
  return img;
}

// Jacket: ribbed conduit, a groove every 4 units. Light from above, darkest underneath.
const jacket = atlas('p', [
  ['side', 0, 0, ['vvvqvvvq', 'qqqpqqqp', 'pppopppo', 'ooonooon']],
  ['top', 8, 0, ['Wvvq', 'Wvvq', 'Wvvq', 'vqqp', 'Wvvq', 'Wvvq', 'Wvvq', 'vqqp']],
  ['bottom', 12, 0, ['qppo', 'qppo', 'qppo', 'poon', 'qppo', 'qppo', 'qppo', 'poon']],
  ['end', 0, 4, ['oppo', 'pyyp', 'pyyp', 'oppo']],
]);
const J = { side: [0, 0], top: [8, 0], bottom: [12, 0], end: [0, 4] };

// Hub: plain iron junction, same section as the jacket so arms join flush.
const hub = atlas('p', [
  ['side', 0, 0, ['Wvvq', 'vqqp', 'vqqp', 'qppo']],
  ['top', 4, 0, ['WWvq', 'Wvvq', 'vvqp', 'qqpo']],
  ['bottom', 8, 0, ['qppo', 'pooo', 'pooo', 'oonn']],
]);

// Relief: dark metal boot behind the collar, plus the plug tip that meets the socket.
const relief = atlas('x', [
  ['boot_face', 0, 0, ['zzzzzy', 'zyyyyx', 'zyyyyx', 'zyyyyx', 'zyyyyx', 'yxxxxw']],
  ['boot_top', 6, 0, ['yzzzzy']],
  ['boot_bottom', 6, 1, ['wxxxxw']],
  ['boot_side', 12, 0, ['z', 'y', 'y', 'y', 'y', 'x']],
  ['tip_top', 6, 2, ['yzzy']],
  ['tip_bottom', 6, 3, ['wxxw']],
  ['tip_side', 13, 0, ['z', 'y', 'y', 'x']],
  ['tip_face', 0, 8, ['zzzy', 'zwwx', 'zwwx', 'yxxw']],
]);

// Ferrule: the only brass on the cable, a 1-unit collar at the socket.
const ferrule = atlas('4', [
  ['face', 0, 0, ['777775', '733334', '733334', '733334', '733334', '544442']],
  ['top', 6, 0, ['677776']],
  ['bottom', 6, 1, ['344443']],
  ['side', 12, 0, ['6', '5', '5', '5', '5', '3']],
]);

const textures = [
  texture('avionics_cable_jacket', 16, { source: jacket, particle: true }), // 0
  texture('avionics_cable_hub', 16, { source: hub }),                       // 1
  texture('avionics_cable_relief', 16, { source: relief }),                 // 2
  texture('avionics_cable_ferrule', 16, { source: ferrule }),               // 3
];

// A run along Z of the given length: east/west use the side strip, up/down the vertical strips.
const run = (length, ends) => ({
  east: face(uv(...J.side, length, 4), 0), west: face(uv(...J.side, length, 4), 0),
  up: face(uv(...J.top, 4, length), 0), down: face(uv(...J.bottom, 4, length), 0),
  ...Object.fromEntries(ends.map(s => [s, face(uv(...J.end, 4, 4), 0)])),
});
// A ring of 1 unit along Z: east/west are 1 x n columns, up/down n x 1 rows.
const ring = (tex, n, [fx, fy], [tx, ty], [bx, by], [sx, sy], faces) => ({
  up: face(uv(tx, ty, n, 1), tex), down: face(uv(bx, by, n, 1), tex),
  east: face(uv(sx, sy, 1, n), tex), west: face(uv(sx, sy, 1, n), tex),
  ...Object.fromEntries(faces.map(s => [s, face(uv(fx, fy, n, n), tex)])),
});

const centerHub = cube('center_hub', [6, 6, 6], [10, 10, 10], {
  north: face(uv(0, 0, 4, 4), 1), south: face(uv(0, 0, 4, 4), 1), east: face(uv(0, 0, 4, 4), 1),
  west: face(uv(0, 0, 4, 4), 1), up: face(uv(4, 0, 4, 4), 1), down: face(uv(8, 0, 4, 4), 1),
});
const straight = { ...cube('cable_to_cable', [6, 6, 8], [10, 10, 16], run(8, ['north', 'south'])), visibility: false };
// No south face: it would cap the jacket inside the connector boot.
const flexible = cube('flexible_jacket', [6, 6, 8], [10, 10, 13], run(5, ['north']));
// Boot north stays visible as a ring around the jacket; its south is coplanar with the collar.
const boot = cube('strain_relief', [5, 5, 13], [11, 11, 14], ring(2, 6, [0, 0], [6, 0], [6, 1], [12, 0], ['north']), 4);
// Collar north is coplanar with the boot; its south is a brass ring around the tip.
const collar = cube('metal_ferrule', [5, 5, 14], [11, 11, 15], ring(3, 6, [0, 0], [6, 0], [6, 1], [12, 0], ['south']), 1);
// Tip: no north (inside the collar). The south plug face only reaches the item model;
// on a placed cable it mates the device socket and the exporter drops it.
const tip = cube('socket_tip', [6, 6, 15], [10, 10, 16], ring(2, 4, [0, 8], [6, 2], [6, 3], [13, 0], ['south']), 4);

const elements = [centerHub, straight, flexible, boot, collar, tip];
const groups = [
  group('hub', [8, 8, 8], [centerHub]),
  group('straight_arm', [8, 8, 8], [straight]),
  group('flexible_arm', [8, 8, 8], [flexible]),
  group('connector', [8, 8, 16], [boot, collar, tip], 1),
];
writeProject(new URL('avionics_cable.bbmodel', import.meta.url), { name: 'avionics_cable', elements, groups, textures });
console.log(`Wrote avionics_cable.bbmodel: ${elements.length} cubes, ${textures.length} textures.`);
