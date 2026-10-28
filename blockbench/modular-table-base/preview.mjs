// Top-down check render of the connected tiles: lays tables out on a grid and mixes the tile
// quadrants with the same rule as TableConnections. Writes preview.png (8 px per texel).
import { writeFileSync } from 'node:fs';
import { PALETTE, png } from '../_tools/bbtools.mjs';
import { TILES } from './table-textures.mjs';

// '#' = table. Covers a straight row, a block, an L (concave corner), a lone table and a ring.
const LAYOUT = [
  '###.#.###',
  '###...#.#',
  '#.....###',
  '##.##....',
];
const SCALE = 8, GAP = '.';
const at = (x, y) => LAYOUT[y]?.[x] === '#';
// Quadrant tile: same rule as TableConnections.tile(side, vertical, diagonal).
const tileFor = (a, b, c) => !a && !b ? 'modular_table_top' : a && !b ? 'modular_table_top_horizontal'
  : !a ? 'modular_table_top_vertical' : !c ? 'modular_table_top_inner' : 'modular_table_top_wood';

const W = LAYOUT[0].length * 16, H = LAYOUT.length * 16;
const img = Array.from({ length: H * SCALE }, () => Array(W * SCALE).fill('#20232A'));
LAYOUT.forEach((row, ty) => [...row].forEach((cell, tx) => {
  if (cell === GAP) return;
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const du = x < 8 ? -1 : 1, dv = y < 8 ? -1 : 1;
    const name = tileFor(at(tx + du, ty), at(tx, ty + dv), at(tx + du, ty + dv));
    const hex = PALETTE[TILES[name][y][x]];
    for (let sy = 0; sy < SCALE; sy++) for (let sx = 0; sx < SCALE; sx++) {
      img[(ty * 16 + y) * SCALE + sy][(tx * 16 + x) * SCALE + sx] = hex;
    }
  }
}));
writeFileSync(new URL('preview.png', import.meta.url), png(img));
console.log(`Wrote preview.png (${W * SCALE} x ${H * SCALE}).`);
