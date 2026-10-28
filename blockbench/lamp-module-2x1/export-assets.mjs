// Mechanical export of the saved lamp_module_2x1.bbmodel (no regeneration).
// Writes models/block/lamp_module_2x1_off.json and lamp_module_2x1_on.json. The PNGs belong to the 1x1
// source: export it first; this only checks that the embedded copies match them pixel for pixel.
import { exportBlockModel, NS } from '../_tools/bbtools.mjs';

const root = new URL('./', import.meta.url);
const result = exportBlockModel({
  projectUrl: new URL('lamp_module_2x1.bbmodel', root),
  assetsUrl: new URL(`../../src/main/resources/assets/${NS}/`, root),
  model: 'lamp_module_2x1',
  renderType: 'minecraft:translucent',
  writeBase: false,
  textures: 'verify',
  variants: {
    _off: {}, _on: { lamp_filament_off: 'lamp_filament_on' },
    // Amber (Caution, GPWS INOP): only the glass and the lit filament change (plan §6).
    _amber_off: { lamp_glass_red: 'lamp_glass_amber' },
    _amber_on: { lamp_glass_red: 'lamp_glass_amber', lamp_filament_off: 'lamp_filament_on_amber' },
  },
});
console.log(`Exported lamp_module_2x1: ${result.cubes} cubes; ${result.textures.join(', ')} (off, on, amber off, amber on).`);
