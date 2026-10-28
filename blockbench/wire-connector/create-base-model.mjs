// Bootstrap of the Wire Connector (Avionics Wire). Writes wire_connector.bbmodel.
// Brass (guide §5/§7: brass marks the data point), dark-metal gland where the wire enters.
// Modelled facing UP (host block below, y = 0 is the host face); integer coordinates, 1 texel per unit.
// The gland top (y = 4) is WireAttachPoints.CONNECTOR_MOUTH; shapes live in WireConnectorBlock.
// The stem reaches 2 px into the host (y -2..1): hidden in a full block, it closes the gap to an inset
// device (Avionics Computer) and carries the body when the flange is dropped on a table edge
// (wire_connector_slim, written by export-assets.mjs).
// Refine in Blockbench afterwards; do NOT rerun over a refined source.
import { texture, face, cube, group, writeProject, px, canvas, blit, checkMap } from '../_tools/bbtools.mjs';

const uv = (x, y, w, h) => px(16, [x, y, x + w, y + h]);
const img = canvas(16, 16, '4');
const parts = [
  // Flange: 1-unit plate on the host face; only its outer ring shows around the body.
  ['flange_top', 0, 0, ['777776', '765554', '755544', '755544', '754443', '644433']],
  ['flange_side', 0, 6, ['544443']],
  // Body: stepped brass collar (guide §3, "bocal escalonado").
  ['body_top', 6, 0, ['7776', '7654', '7654', '6443']],
  ['body_side', 6, 4, ['6665', '4332']],
  // Gland: dark metal grommet with the wire hole.
  ['gland_top', 10, 0, ['zx', 'xw']],
  ['gland_side', 10, 2, ['zy']],
  // Stem: shadowed brass sleeve into the host.
  ['stem_side', 12, 0, ['4443', '3332', '3332']],
];
for (const [name, x, y, map] of parts) blit(img, checkMap(name, map, map[0].length, map.length), x, y);
const textures = [texture('wire_connector', 16, { source: img, particle: true })];

const sides = (x, y, w, h) => Object.fromEntries(['north', 'east', 'south', 'west'].map(s => [s, face(uv(x, y, w, h), 0)]));
// Bottom faces sit on the host face or inside the part below: left out (no coplanar caps).
const flange = cube('flange', [5, 0, 5], [11, 1, 11], { up: face(uv(0, 0, 6, 6), 0), ...sides(0, 6, 6, 1) }, 1);
const body = cube('body', [6, 1, 6], [10, 3, 10], { up: face(uv(6, 0, 4, 4), 0), ...sides(6, 4, 4, 2) }, 1);
const gland = cube('gland', [7, 3, 7], [9, 4, 9], { up: face(uv(10, 0, 2, 2), 0), ...sides(10, 2, 2, 1) }, 4);

const stem = cube('stem', [6, -2, 6], [10, 1, 10], sides(12, 0, 4, 3), 1);

const elements = [flange, body, gland, stem];
writeProject(new URL('wire_connector.bbmodel', import.meta.url), {
  name: 'wire_connector', elements, groups: [group('connector', [8, 0, 8], elements)], textures,
});
console.log(`Wrote wire_connector.bbmodel: ${elements.length} cubes.`);
