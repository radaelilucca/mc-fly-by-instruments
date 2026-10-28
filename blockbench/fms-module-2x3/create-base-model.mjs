// Bootstrap of the v2.1 FMS module 2x3. Writes fms_module_2x3.bbmodel.
// Simple Create-style console: industrial iron housing (create:block/industrial_iron_block), a dark
// lens screen in a brass frame, two brass knobs and two industrial-iron keys (guide §9.4).
// Every detail stands 1 unit above the housing: relief is kept to a single pixel.
// Modelled with the table facing up: rests on Y 0, centred on X/Z 8, 10 x 14 footprint (guide §9.2).
// Low Z is the top of the panel (screen); the controls sit on the pilot side.
// Refine in Blockbench afterwards; do NOT rerun over a refined source.
import { texture, face, cube, group, writeProject, px, canvas, blit, checkMap } from '../_tools/bbtools.mjs';

// --- Atlas (32 x 32, pixels = model units) ---------------------------------------------------
const MAPS = {
  // Brass frame lit top-left, shaded bottom-right, around a dark lens with a blue glint.
  // The lens is reserved for screen content later.
  screenTop: ['87777776', '7VQOOON4', '7QYNNML4', '7ONNMLL4', '7ONMMLL4', '7NMLLLL4', '7MLLLLL4', '64444443'],
  screenLit: ['46666664'],
  screenShaded: ['35555553'],
  // Brass knobs: a dark notch on the cap points like a dial.
  knobSide: ['64'],
  knobTopLeft: ['83', '65'],
  knobTopRight: ['38', '56'],
  // Keys: light industrial-iron caps with a dark rim, so they read against the housing.
  keyTop: ['Wv'],
  keySide: ['on'],
  keyEnd: ['n'],
};
export const UV = {
  screenTop: [0, 0, 8, 8], screenLit: [0, 8, 8, 9], screenShaded: [0, 9, 8, 10],
  knobSide: [8, 0, 10, 1], knobTopLeft: [8, 2, 10, 4], knobTopRight: [10, 2, 12, 4],
  keyTop: [8, 5, 10, 6], keySide: [8, 6, 10, 7], keyEnd: [10, 5, 11, 6],
};
function atlas() {
  const img = canvas(32, 32, 'w');
  for (const [name, map] of Object.entries(MAPS)) {
    const [u0, v0, u1, v1] = UV[name];
    blit(img, checkMap(name, map, u1 - u0, v1 - v0), u0, v0);
  }
  return img;
}

// Create's assets are All Rights Reserved: reference them by id, never embed their pixels in this
// repo. For the Blockbench preview, keep a local copy in reference/create-textures/ (gitignored).
const textures = [
  texture('fms_module', 32, { source: atlas() }),                                     // 0
  texture('industrial_iron_block', 16, { namespace: 'create', particle: true,
    relativePath: '../../reference/create-textures/industrial_iron_block.png' }),      // 1
];
const A = name => face(UV[name], 0);
const iron = region => face(px(16, region), 1);

const elements = [
  // Crops of the Create texture keep its dark rim on the housing edges.
  cube('housing', [3, 0, 1], [13, 2, 15], {
    north: iron([3, 14, 13, 16]), south: iron([3, 14, 13, 16]), east: iron([1, 14, 15, 16]),
    west: iron([1, 14, 15, 16]), up: iron([3, 1, 13, 15]), down: iron([3, 1, 13, 15]),
  }, 0),
  cube('screen', [4, 2, 2], [12, 3, 10], { up: A('screenTop'), north: A('screenLit'),
    west: A('screenLit'), south: A('screenShaded'), east: A('screenShaded') }, 3),
  ...[['knob_left', 4, 'knobTopLeft'], ['knob_right', 10, 'knobTopRight']].map(([name, x, top]) =>
    cube(name, [x, 2, 12], [x + 2, 3, 14], { north: A('knobSide'), south: A('knobSide'),
      east: A('knobSide'), west: A('knobSide'), up: A(top) }, 3)),
  ...[['key_upper', 11], ['key_lower', 13]].map(([name, z]) =>
    cube(name, [7, 2, z], [9, 3, z + 1], { north: A('keySide'), south: A('keySide'),
      east: A('keyEnd'), west: A('keyEnd'), up: A('keyTop') }, 1)),
];
const groups = [
  group('housing', [8, 0, 8], elements.slice(0, 1)),
  group('display', [8, 2, 6], elements.slice(1, 2), 3),
  group('controls', [8, 2, 12], elements.slice(2), 1),
];
writeProject(new URL('fms_module_2x3.bbmodel', import.meta.url),
  { name: 'fms_module_2x3', elements, groups, textures });
console.log(`Wrote fms_module_2x3.bbmodel: ${elements.length} cubes, ${textures.length} textures.`);
