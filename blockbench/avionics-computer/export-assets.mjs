// Mechanical export of the saved avionics_computer.bbmodel (no regeneration).
// Writes models/block/avionics_computer.json (item and base), one variant per screen state
// (_normal, _caution, _warning, _inop: only the screen texture changes) and the PNGs.
import { exportBlockModel, NS } from '../_tools/bbtools.mjs';

const root = new URL('./', import.meta.url);
const states = ['normal', 'caution', 'warning', 'inop'];
const result = exportBlockModel({
  projectUrl: new URL('avionics_computer.bbmodel', root),
  assetsUrl: new URL(`../../src/main/resources/assets/${NS}/`, root),
  model: 'avionics_computer',
  variants: Object.fromEntries(states.map(state =>
    [`_${state}`, { avionics_computer_screen: `avionics_computer_screen_${state}` }])),
});
console.log(`Exported avionics_computer: ${result.cubes} cubes; ${result.textures.join(', ')} (+ ${states.join(', ')}).`);
