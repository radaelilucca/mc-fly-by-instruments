// Mechanical export of the saved modular_table_base.bbmodel (no regeneration).
// Writes models/block/modular_table_base.json and all six PNGs, including the four connected
// tiles that no face references (the block atlas stitches every texture under textures/block/).
import { readFileSync, writeFileSync } from 'node:fs';
import { exportBlockModel, NS } from '../_tools/bbtools.mjs';

const root = new URL('./', import.meta.url);
const assetsUrl = new URL(`../../src/main/resources/assets/${NS}/`, root);
const result = exportBlockModel({
  projectUrl: new URL('modular_table_base.bbmodel', root),
  assetsUrl,
  model: 'modular_table_base',
});
// The base sits on its support: the bottom face culls against it in every orientation.
const modelUrl = new URL('models/block/modular_table_base.json', assetsUrl);
const model = JSON.parse(readFileSync(modelUrl, 'utf8'));
model.elements[0].faces.down.cullface = 'down';
writeFileSync(modelUrl, `${JSON.stringify(model, null, 2)}\n`);
console.log(`Exported modular_table_base: ${result.cubes} cube; ${result.textures.join(', ')}.`);
