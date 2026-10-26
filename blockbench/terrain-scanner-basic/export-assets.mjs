// Mechanical export of the saved terrain_scanner_basic.bbmodel (no regeneration).
// Writes models/block/terrain_scanner.json, terrain_scanner_powered.json and this mod's PNGs.
import { exportBlockModel, NS } from '../_tools/bbtools.mjs';

const root = new URL('./', import.meta.url);
const result = exportBlockModel({
  projectUrl: new URL('terrain_scanner_basic.bbmodel', root),
  assetsUrl: new URL(`../../src/main/resources/assets/${NS}/`, root),
  model: 'terrain_scanner',
  variants: { _powered: { terrain_scanner: 'terrain_scanner_powered' } },
});
console.log(`Exported terrain_scanner: ${result.cubes} cubes; ${result.textures.join(', ')} (+ powered).`);
