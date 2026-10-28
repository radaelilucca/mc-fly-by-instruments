// Bootstrap of the v2.1 modular table base. Writes modular_table_base.bbmodel.
// Brass rim + dark-wood top (guide §9.3), flush 16 x 4 x 16 slab: modules rest on Y 4 (a wire connector's 4 px body fits its edge) and the
// edge cells overlap the rim, so a raised rim would clip them. Rim, planks and the connected
// tiles live in the pixels (table-textures.mjs). Refine in Blockbench; do NOT rerun over a refined source.
import { texture, face, cube, group, writeProject, px } from '../_tools/bbtools.mjs';
import { TILES, SIDE_UV, side, image } from './table-textures.mjs';

const names = Object.keys(TILES);
const textures = [
  ...names.map(name => texture(name, 16, { source: image(TILES[name]), particle: name === 'modular_table_top' })),
  texture('modular_table_side', 16, { source: side() }),
];
const TOP = 0, WOOD = names.indexOf('modular_table_top_wood'), SIDE = textures.length - 1;
const uv = region => px(16, region);
const sides = Object.fromEntries(['north', 'south', 'east', 'west'].map(s => [s, face(uv(SIDE_UV), SIDE)]));
// The other tiles are embedded but unused by the faces: the client model swaps them in per quadrant.
const elements = [
  cube('table', [0, 0, 0], [16, 4, 16],
    { ...sides, up: face(uv([0, 0, 16, 16]), TOP), down: face(uv([0, 0, 16, 16]), WOOD) }, 1),
];
const groups = [group('base', [8, 0, 8], elements)];
writeProject(new URL('modular_table_base.bbmodel', import.meta.url),
  { name: 'modular_table_base', elements, groups, textures });
console.log(`Wrote modular_table_base.bbmodel: ${elements.length} cube, ${textures.length} textures.`);
