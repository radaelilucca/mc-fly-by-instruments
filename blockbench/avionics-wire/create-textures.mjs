// Avionics Wire textures. The wire itself is not a block model: WireRenderer draws a 1×1 texel tube and
// samples one column per face (1, 5, 9, 13 = +side, underside, -side, top), with v running along the wire
// at 16 texels per block, so this strip repeats instead of stretching. The item is a flat sprite.
// Hand-written character maps with the guide palette (new "Fio isolado" ramp: 0 9 u, then dark metal w).
import { writeFileSync, mkdirSync } from 'node:fs';
import { PALETTE, png, checkMap, NS } from '../_tools/bbtools.mjs';

const assets = new URL(`../../src/main/resources/assets/${NS}/`, import.meta.url);
const CLEAR = '#00000000';
const paint = (map, clear) => map.map(row => [...row].map(key => key === '.' ? clear : PALETTE[key]));

// Columns in groups of four: +side 'u', underside '0', -side '9', top 'w' (light from above).
// Every 8 texels (half a block) a one-texel ridge marks the extrusion, like a printed jacket.
const plain = 'uuuu0000' + '9999wwww';
const ridge = '99990000' + '0999uuuu';
const wire = checkMap('wire', Array.from({ length: 16 }, (_, row) => row % 8 === 7 ? ridge : plain), 16, 16);

// Spool seen from the side: brass flanges, black coil, iron axle stubs.
const spool = checkMap('spool', [
  '................',
  '................',
  '..87........76..',
  '..75wwwwwwww64..',
  '..75uuuuuuuu64..',
  '..750000000064..',
  '..75uuuuuuuu64..',
  'Wv750000000064po',
  'vq75uuuuuuuu64on',
  '..750000000064..',
  '..75uuuuuuuu64..',
  '..750000000064..',
  '..759999999964..',
  '..53........42..',
  '................',
  '................',
], 16, 16);

for (const [folder, map] of [['entity', wire], ['item', spool]]) {
  const target = new URL(`textures/${folder}/avionics_wire.png`, assets);
  mkdirSync(new URL('./', target), { recursive: true });
  writeFileSync(target, png(paint(map, CLEAR)));
}
writeFileSync(new URL('models/item/avionics_wire.json', assets), `${JSON.stringify({
  parent: 'minecraft:item/generated', textures: { layer0: `${NS}:item/avionics_wire` },
}, null, 2)}\n`);
console.log('Wrote the wire strip, the spool sprite and the item model.');
