// One-off migration of the saved source (2026-10-28): GPWS Computer -> Avionics Computer.
// Renames the embedded textures and replaces the screen textures with the four runtime states
// from screens.mjs. Geometry and every other texture stay exactly as refined in Blockbench.
import { readFileSync, writeFileSync } from 'node:fs';
import { texture } from '../_tools/bbtools.mjs';
import { SCREENS, screenTexture } from './screens.mjs';

const url = new URL('avionics_computer.bbmodel', import.meta.url);
const project = JSON.parse(readFileSync(url, 'utf8'));
project.name = 'avionics_computer';
for (const t of project.textures) t.name = t.name.replace(/^gpws_computer/, 'avionics_computer');

const screenIndex = project.textures.findIndex(t => t.name === 'avionics_computer_screen.png');
if (screenIndex < 0) throw new Error('screen texture not found');
// Keep the base screen slot (the cube references its index); drop every old variant after it.
const kept = project.textures.filter(t => !t.name.startsWith('avionics_computer_screen_'));
const added = Object.entries(SCREENS).map(([name, { frames, frameTime = 1 }]) =>
  texture(name, 16, { source: screenTexture(frames), frameTime }));
const base = added.shift();
kept[screenIndex] = { ...base, uuid: kept[screenIndex].uuid };
project.textures = [...kept, ...added];
project.textures.forEach((t, i) => { t.id = String(i); });
writeFileSync(url, JSON.stringify(project));
console.log(`Migrated avionics_computer.bbmodel: ${project.textures.map(t => t.name).join(', ')}`);
