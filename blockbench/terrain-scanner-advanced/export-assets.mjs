// Mechanical export of the saved terrain_scanner_advanced.bbmodel (no regeneration).
// Writes models/block/terrain_scanner_advanced.json, terrain_scanner_advanced_powered.json and PNGs.
import { exportBlockModel, NS } from '../_tools/bbtools.mjs';

const root = new URL('./', import.meta.url);
const result = exportBlockModel({
  projectUrl: new URL('terrain_scanner_advanced.bbmodel', root),
  assetsUrl: new URL(`../../src/main/resources/assets/${NS}/`, root),
  model: 'terrain_scanner_advanced',
  variants: { _powered: { terrain_scanner_advanced: 'terrain_scanner_advanced_powered' } },
});
console.log(`Exported terrain_scanner_advanced: ${result.cubes} cubes; ${result.textures.join(', ')} (+ powered).`);
