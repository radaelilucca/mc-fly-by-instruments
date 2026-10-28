// Mechanical export of the saved fms_module_2x3.bbmodel (no regeneration).
// Writes models/block/fms_module_2x3.json and textures/block/fms_module.png. The housing references
// create:block/industrial_iron_block by id (Create's assets are All Rights Reserved).
import { exportBlockModel, NS } from '../_tools/bbtools.mjs';

const root = new URL('./', import.meta.url);
const result = exportBlockModel({
  projectUrl: new URL('fms_module_2x3.bbmodel', root),
  assetsUrl: new URL(`../../src/main/resources/assets/${NS}/`, root),
  model: 'fms_module_2x3',
});
console.log(`Exported fms_module_2x3: ${result.cubes} cubes; ${result.textures.join(', ')}.`);
