// Mechanical export of the saved fms_navigator.bbmodel (no regeneration).
// Writes models/block/fms_navigator.json, models/block/fms_navigator_linked.json (same geometry, linked
// atlas) and their PNGs; models/item/fms_navigator*.json inherit them and only set the display transforms.
import { exportBlockModel, NS } from '../_tools/bbtools.mjs';

const root = new URL('./', import.meta.url);
const result = exportBlockModel({
  projectUrl: new URL('fms_navigator.bbmodel', root),
  assetsUrl: new URL(`../../src/main/resources/assets/${NS}/`, root),
  model: 'fms_navigator',
  variants: { _linked: { fms_navigator: 'fms_navigator_linked' } },
});
console.log(`Exported fms_navigator: ${result.cubes} cubes; ${result.textures.join(', ')} (+ linked atlas).`);
