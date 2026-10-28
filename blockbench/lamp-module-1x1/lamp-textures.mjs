// Shared lamp textures (16 x 16, pixels = model units), owned by the 1x1 source and reused by the 2x1.
// Inspired by Create's nixie tube and train signal lamp (guide §9.4). Texture names are a contract with
// LampModuleRenderer, which sorts quads by sprite. Regions: `short` = 1x1 faces and 2x1 ends,
// `long` = 2x1 faces along X.
import { canvas, blit, checkMap } from '../_tools/bbtools.mjs';

// --- Socket: dark metal like the nixie base; the brass rim on top shows through the glass ---------
const SOCKET = {
  side: checkMap('socket side', ['xzzx'], 4, 1),
  top: checkMap('socket top', ['6553', '5wx2', '5xw2', '3221'], 4, 4),
  bottom: checkMap('socket bottom', ['wxxw', 'xyyx', 'xyyx', 'wxxw'], 4, 4),
  longSide: checkMap('socket long side', ['xzzyzzyzzx'], 10, 1),
  longTop: checkMap('socket long top', ['6555555553', '5wxwxwxwx2', '5xwxwxwxw2', '3222222221'], 10, 4),
  longBottom: checkMap('socket long bottom', ['wxxxxxxxxw', 'xyyyyyyyyx', 'xyyyyyyyyx', 'wxxxxxxxxw'], 10, 4),
};
export const SOCKET_UV = {
  side: [0, 0, 4, 1], top: [4, 0, 8, 4], bottom: [8, 0, 12, 4],
  longSide: [0, 4, 10, 5], longTop: [0, 6, 10, 10], longBottom: [0, 10, 10, 14],
};

// --- Glass: redstone tint; clear core (low alpha) so the filament shows, denser edges and highlight.
// The 1x1 tube is lit along a column; the 2x1 bar is a horizontal tube, lit along its top row.
// Alpha keys: h, m, l.
const GLASS = {
  side: checkMap('glass side', ['SUTS', 'SUTR', 'RTSR'], 4, 3),
  sideAlpha: checkMap('glass side alpha', ['mhlm', 'mhlm', 'mmlm'], 4, 3),
  top: checkMap('glass top', ['SUTS', 'UTTR', 'TTSR', 'SRRR'], 4, 4),
  topAlpha: checkMap('glass top alpha', ['mhmm', 'hllm', 'mllm', 'mmmm'], 4, 4),
  longSide: checkMap('glass long side', ['SUUUUUUUUS', 'STSSTSSTSR', 'RSRRSRRSRR'], 10, 3),
  longSideAlpha: checkMap('glass long side alpha', ['mhhhhhhhhm', 'mllllllllm', 'mmmmmmmmmm'], 10, 3),
  longTop: checkMap('glass long top', ['SUUUUUUUUS', 'UTTTTTTTTR', 'TSSSSSSSSR', 'SRRRRRRRRR'], 10, 4),
  longTopAlpha: checkMap('glass long top alpha', ['mhhhhhhhhm', 'hllllllllm', 'mllllllllm', 'mmmmmmmmmm'], 10, 4),
};
const ALPHA = { h: 0xb0, m: 0x80, l: 0x48 };
export const GLASS_UV = { side: [0, 0, 4, 3], top: [4, 0, 8, 4], longSide: [0, 4, 10, 7], longTop: [0, 8, 10, 12] };

// --- Filament: dark-metal mesh when off; redstone checker when on (train signal lamp) -------------
const FILAMENT = {
  off: {
    side: checkMap('filament off side', ['zx', 'xz'], 2, 2),
    top: checkMap('filament off top', ['zz', 'zy'], 2, 2),
    longSide: checkMap('filament off long side', ['zxzxzxzx', 'xzxzxzxz'], 8, 2),
    longTop: checkMap('filament off long top', ['zzzzzzzz', 'zyzyzyzy'], 8, 2),
  },
  on: {
    side: checkMap('filament on side', ['UT', 'TS'], 2, 2),
    top: checkMap('filament on top', ['UT', 'TU'], 2, 2),
    longSide: checkMap('filament on long side', ['UTUTUTUT', 'TSTSTSTS'], 8, 2),
    longTop: checkMap('filament on long top', ['UTUTUTUT', 'TUTUTUTU'], 8, 2),
  },
};
export const FILAMENT_UV = { side: [0, 0, 2, 2], top: [2, 0, 4, 2], longSide: [0, 2, 8, 4], longTop: [0, 4, 8, 6] };

const paint = (img, maps, uv) => { for (const [key, map] of Object.entries(maps)) blit(img, map, uv[key][0], uv[key][1]); return img; };

// --- Amber (Caution, GPWS INOP; plan §6): the same maps with the redstone keys swapped for the light
// brass tones (guide §9.4). Same shape and alpha, so red and amber read as one object in two colours.
const AMBER = { R: '5', S: '6', T: '7', U: '8' };
const tint = (maps, color) => color === 'amber'
  ? Object.fromEntries(Object.entries(maps).map(([key, map]) => [key, map.map(row => [...row].map(k => AMBER[k] ?? k).join(''))]))
  : maps;

export const socket = () => paint(canvas(16, 16, 'w'), SOCKET, SOCKET_UV);
export const filament = (lit, color = 'red') => paint(canvas(16, 16, lit ? (color === 'amber' ? '7' : 'T') : 'x'),
  lit ? tint(FILAMENT.on, color) : FILAMENT.off, FILAMENT_UV);
export function glass(color = 'red') {
  const colors = tint({ side: GLASS.side, top: GLASS.top, longSide: GLASS.longSide, longTop: GLASS.longTop }, color);
  const img = paint(canvas(16, 16, color === 'amber' ? '5' : 'R'), colors, GLASS_UV);
  const alpha = Array.from({ length: 16 }, () => Array(16).fill(ALPHA.m));
  for (const key of Object.keys(colors)) {
    const [x0, y0] = GLASS_UV[key];
    GLASS[`${key}Alpha`].forEach((row, y) => [...row].forEach((k, x) => { alpha[y0 + y][x0 + x] = ALPHA[k]; }));
  }
  // '#rrggbbaa' = translucent pixel; only glass may use it (guide §9.4).
  return img.map((row, y) => row.map((hex, x) => hex + alpha[y][x].toString(16).padStart(2, '0')));
}
