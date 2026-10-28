// Mechanical export of the saved gpws_computer.bbmodel (no regeneration).
// Writes models/block/gpws_computer.json, gpws_computer_armed.json (blinking cursor) and PNGs.
import { exportBlockModel, NS } from '../_tools/bbtools.mjs';

const root = new URL('./', import.meta.url);
const result = exportBlockModel({
  projectUrl: new URL('gpws_computer.bbmodel', root),
  assetsUrl: new URL(`../../src/main/resources/assets/${NS}/`, root),
  model: 'gpws_computer',
  variants: { _armed: { gpws_computer_screen: 'gpws_computer_screen_armed' } },
});
console.log(`Exported gpws_computer: ${result.cubes} cubes; ${result.textures.join(', ')} (+ armed).`);
