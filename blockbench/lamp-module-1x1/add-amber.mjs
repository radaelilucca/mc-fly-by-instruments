// One-off migration (2026-10-28, plan §6): embeds the amber glass and the amber lit filament in the saved
// 1x1 and 2x1 sources without touching their refined geometry. The pixels come from lamp-textures.mjs.
import { readFileSync, writeFileSync } from 'node:fs';
import { texture } from '../_tools/bbtools.mjs';
import { glass, filament } from './lamp-textures.mjs';

const added = () => [
  texture('lamp_glass_amber', 16, { source: glass('amber') }),
  texture('lamp_filament_on_amber', 16, { source: filament(true, 'amber') }),
];
for (const path of ['lamp_module_1x1.bbmodel', '../lamp-module-2x1/lamp_module_2x1.bbmodel']) {
  const url = new URL(path, import.meta.url);
  const project = JSON.parse(readFileSync(url, 'utf8'));
  const names = new Set(added().map(t => t.name));
  project.textures = [...project.textures.filter(t => !names.has(t.name)), ...added()];
  project.textures.forEach((t, i) => { t.id = String(i); });
  writeFileSync(url, JSON.stringify(project));
  console.log(`${path}: ${project.textures.map(t => t.name).join(', ')}`);
}
