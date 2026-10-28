// Pixel maps of the v2.1 modular table base (guide §9.3): 2 px brass rim around a dark-wood top.
// Connected textures follow the compact CTM layout: five 16 x 16 tiles whose quadrants are mixed
// per neighbour by ModularTableModels (client) and TableConnections (rules). The wood pattern is a
// function of the pixel only, so any mix of tiles from neighbouring tables lines up seamlessly.
import { canvas, blit, checkMap } from '../_tools/bbtools.mjs';

// Horizontal dark-wood planks: 3 px plank + 1 px seam, staggered butt joints, lit first row,
// a sparse deterministic grain mark. Period 16 on both axes, so it tiles across tables.
const JOINTS = [3, 11, 7, 15];
export const wood = (x, y) => {
  if (y % 4 === 3) return 'a';
  if (x === JOINTS[y >> 2]) return 'b';
  if ((x * 5 + y * 3) % 13 === 0) return 'c';
  if (y % 4 === 0) return (x * 3 + y) % 7 === 0 ? 'f' : 'e';
  return 'd';
};

// Brass rim (andesite-frame motif of the scanners, 2 px wide): outer outline in the 2nd tone,
// inner bevel lit top/left (7) and darker bottom/right (5), 6 where they cross, 1 at outer corners.
// Concave corners (both sides connected, diagonal missing) get a 2 x 2 square joining the rims.
const INNER = {
  nw: [[0, 0, '2'], [1, 0, '7'], [0, 1, '7'], [1, 1, '7']],
  ne: [[15, 0, '2'], [14, 0, '5'], [15, 1, '7'], [14, 1, '6']],
  sw: [[0, 15, '2'], [1, 15, '7'], [0, 14, '5'], [1, 14, '6']],
  se: [[15, 15, '2'], [14, 15, '5'], [15, 14, '5'], [14, 14, '5']],
};
function tile({ up = false, down = false, left = false, right = false, inner = false }) {
  const rows = [];
  for (let y = 0; y < 16; y++) {
    let row = '';
    for (let x = 0; x < 16; x++) {
      const outX = (left && x === 0) || (right && x === 15), outY = (up && y === 0) || (down && y === 15);
      const lit = (up && y === 1) || (left && x === 1), dark = (down && y === 14) || (right && x === 14);
      if (outX && outY) row += '1';
      else if (outX || outY) row += '2';
      else if (lit) row += dark ? '6' : '7';
      else if (dark) row += '5';
      else row += wood(x, y);
    }
    rows.push(row);
  }
  if (inner) {
    const grid = rows.map(r => [...r]);
    for (const square of Object.values(INNER)) for (const [x, y, k] of square) grid[y][x] = k;
    return grid.map(r => r.join(''));
  }
  return rows;
}

// Tile name -> map. Names are a contract with ModularTableModels (sprite ids) and the tests.
export const TILES = {
  modular_table_top: tile({ up: true, down: true, left: true, right: true }),  // no neighbours (item look)
  modular_table_top_horizontal: tile({ up: true, down: true }),                // joined along U (x)
  modular_table_top_vertical: tile({ left: true, right: true }),               // joined along V (z)
  modular_table_top_inner: tile({ inner: true }),                              // joined, diagonal missing
  modular_table_top_wood: tile({}),                                            // joined all round
};
for (const [name, map] of Object.entries(TILES)) checkMap(name, map, 16, 16);

// Side band (16 x 3 used, rows 0-2): lit lip, brass body with rivets every 8 px, dark foot.
// No end caps, so the band runs on unbroken along a row of tables.
const BAND = ['6666666666666666', '4448444444484444', '2222222222222222'];
export const SIDE_UV = [0, 0, 16, 3];
export const side = () => {
  const img = canvas(16, 16, '4');
  for (let y = 0; y < 16; y += 3) blit(img, BAND.slice(0, Math.min(3, 16 - y)), 0, y);
  return img;
};
export const image = map => { const img = canvas(16, 16, 'a'); blit(img, map, 0, 0); return img; };
