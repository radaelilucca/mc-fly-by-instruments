// Mechanical export of the saved lamp_module_1x1.bbmodel (no regeneration).
// Writes models/block/lamp_module_1x1_off.json, lamp_module_1x1_on.json and this mod's PNGs.
import { exportBlockModel, NS } from '../_tools/bbtools.mjs';

const root = new URL('./', import.meta.url);
const result = exportBlockModel({
  projectUrl: new URL('lamp_module_1x1.bbmodel', root),
  assetsUrl: new URL(`../../src/main/resources/assets/${NS}/`, root),
  model: 'lamp_module_1x1',
  renderType: 'minecraft:translucent',
  writeBase: false,
  variants: { _off: {}, _on: { lamp_filament_off: 'lamp_filament_on' } },
});
console.log(`Exported lamp_module_1x1: ${result.cubes} cubes; ${result.textures.join(', ')} (off + on).`);
