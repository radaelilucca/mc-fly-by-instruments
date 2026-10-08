// Bootstrap only. Preserve subsequent Blockbench edits with export-assets.mjs.
import { writeFileSync } from 'node:fs';
import { deflateSync } from 'node:zlib';
import { randomUUID } from 'node:crypto';

const size = 64;
const pixels = Buffer.alloc(size * size * 4);
function rect(x, y, width, height, rgb) {
  for (let py = y; py < y + height; py++) for (let px = x; px < x + width; px++) {
    pixels.set([...rgb, 255], (py * size + px) * 4);
  }
}
rect(0, 0, 64, 64, [24, 29, 32]);
// Atlas islands: faceplate, blank screen, keycaps, screws and side/underside.
rect(0, 0, 20, 30, [52, 59, 69]);
rect(0, 0, 20, 1, [93, 101, 108]);
rect(0, 1, 1, 28, [76, 83, 92]);
rect(19, 1, 1, 29, [36, 42, 49]);
rect(1, 29, 18, 1, [36, 42, 49]);
rect(24, 0, 16, 24, [8, 12, 15]);
rect(24, 0, 16, 1, [18, 23, 27]);
rect(24, 1, 1, 22, [15, 20, 24]);
rect(25, 2, 14, 1, [11, 16, 20]);
rect(44, 0, 8, 8, [157, 161, 163]);
rect(44, 0, 8, 1, [209, 213, 215]);
rect(44, 1, 1, 7, [186, 189, 191]);
rect(51, 1, 1, 7, [106, 112, 116]);
rect(45, 7, 6, 1, [106, 112, 116]);
rect(44, 10, 4, 4, [27, 32, 36]);
rect(44, 10, 4, 1, [78, 84, 89]);
rect(45, 11, 2, 1, [167, 175, 176]);
rect(50, 10, 6, 4, [27, 32, 36]);
rect(50, 10, 6, 1, [73, 79, 86]);
rect(52, 11, 2, 1, [151, 160, 163]);
rect(44, 18, 4, 4, [129, 135, 138]);
rect(45, 18, 2, 4, [57, 63, 68]);
rect(44, 19, 4, 2, [57, 63, 68]);
rect(50, 18, 4, 4, [30, 58, 73]);
rect(50, 18, 4, 1, [79, 116, 134]);
rect(51, 19, 2, 1, [160, 185, 190]);
rect(24, 28, 20, 20, [29, 34, 39]);
rect(0, 34, 20, 6, [45, 51, 60]);
rect(0, 34, 20, 1, [78, 87, 95]);
rect(0, 39, 20, 1, [24, 29, 32]);
rect(44, 26, 16, 6, [109, 118, 124]);
rect(44, 26, 16, 1, [186, 191, 194]);
rect(44, 31, 16, 1, [77, 85, 93]);

function crc32(bytes) {
  let crc = 0xffffffff;
  for (const byte of bytes) { crc ^= byte; for (let i = 0; i < 8; i++) crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0); }
  return (crc ^ 0xffffffff) >>> 0;
}
function chunk(type, bytes) {
  const data = Buffer.concat([Buffer.from(type), bytes]), count = Buffer.alloc(4), crc = Buffer.alloc(4);
  count.writeUInt32BE(bytes.length); crc.writeUInt32BE(crc32(data));
  return Buffer.concat([count, data, crc]);
}
const rows = Buffer.alloc(size * (size * 4 + 1));
for (let y = 0; y < size; y++) pixels.copy(rows, y * (size * 4 + 1) + 1, y * size * 4, (y + 1) * size * 4);
const header = Buffer.alloc(13); header.writeUInt32BE(size, 0); header.writeUInt32BE(size, 4); header[8] = 8; header[9] = 6;
const png = Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', header), chunk('IDAT', deflateSync(rows)), chunk('IEND', Buffer.alloc(0))]);
const elements = [], outliner = [];
const islands = { panel: [0, 0, 20, 30], screen: [24, 0, 40, 24], metal: [44, 0, 52, 8],
  key: [44, 10, 48, 14], function: [50, 10, 56, 14], screw: [44, 18, 48, 22], blue: [50, 18, 54, 22],
  bottom: [24, 28, 44, 48], side: [0, 34, 20, 40], bezel: [44, 26, 60, 32] };
function group(name, build) {
  const start = elements.length; build();
  outliner.push({ name, uuid: randomUUID(), origin: [8, 0, 8], children: elements.slice(start).map(e => e.uuid) });
}
function cube(name, from, to, material, top = material) {
  elements.push({ name, type: 'cube', uuid: randomUUID(), from, to, origin: [8, 0, 8],
    rotation: [0, 0, 0], box_uv: false, autouv: 0, rescale: false, shade: true,
    faces: Object.fromEntries(['north', 'east', 'south', 'west', 'up', 'down'].map(side => [side,
      { uv: islands[side === 'up' ? top : side === 'down' ? 'bottom' : material], texture: 0 }])) });
}
group('housing_and_mount', () => {
  cube('solid_mounting_body', [3.25, 0, .75], [12.75, 1.8, 15.25], 'side', 'panel');
  cube('graphite_faceplate', [3.5, 1.8, 1], [12.5, 2.05, 15], 'side', 'panel');
});
group('display_upper_two_thirds', () => {
  cube('blank_display_reserved', [4.5, 2.05, 1.65], [11.5, 2.12, 9.85], 'screen');
  cube('bezel_top', [4.5, 2.05, 1], [11.5, 2.35, 1.65], 'bezel', 'metal');
  cube('bezel_bottom', [4.5, 2.05, 9.85], [11.5, 2.35, 10.5], 'bezel', 'metal');
  cube('bezel_left', [3.5, 2.05, 1], [4.5, 2.35, 10.5], 'bezel', 'metal');
  cube('bezel_right', [11.5, 2.05, 1], [12.5, 2.35, 10.5], 'bezel', 'metal');
});
group('line_select_keys', () => {
  for (const x of [3.67, 11.67]) for (let row = 0; row < 4; row++) {
    const z = 2.6 + row * 1.9;
    cube(`line_select_${x < 8 ? 'left' : 'right'}_${row + 1}`, [x, 2.35, z], [x + .66, 2.55, z + .62], 'key');
  }
});
group('compact_keypad_lower_third', () => {
  for (let row = 0; row < 4; row++) for (let col = 0; col < 6; col++) {
    const x = 3.95 + col * 1.37, z = 10.95 + row * .95;
    cube(`key_${row + 1}_${col + 1}`, [x, 2.05, z], [x + 1.05, 2.3, z + .65], 'key',
      row === 0 ? 'function' : row === 3 && col >= 4 ? 'blue' : 'key');
  }
});
group('corner_fasteners', () => {
  for (const x of [3.65, 11.95]) for (const z of [1.2, 14.4]) {
    cube(`fastener_${x}_${z}`, [x, z < 8 ? 2.35 : 2.05, z], [x + .4, z < 8 ? 2.45 : 2.15, z + .4], 'screw');
  }
});
const project = {
  meta: { format_version: '4.10', model_format: 'java_block', box_uv: false },
  name: 'fms_module_2x3', java_block_version: '1.9', ambientocclusion: true, visible_box: [1, 1, 0],
  resolution: { width: 64, height: 64 }, elements, outliner,
  textures: [{ name: 'fms_module', namespace: 'aeronautics_fly_by_instruments', folder: 'block', id: '0',
    uuid: randomUUID(), width: 64, height: 64, uv_width: 64, uv_height: 64, particle: true, internal: true,
    saved: false, source: `data:image/png;base64,${png.toString('base64')}` }]
};
writeFileSync(new URL('fms_module_2x3.bbmodel', import.meta.url), JSON.stringify(project));
console.log(`Prepared ${elements.length} editable cubes, centered at X/Z=8 with mounting face Y=0.`);
