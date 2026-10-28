// Bootstrap of the v2.1 GPWS Computer: industrial iron cabinet with brass trim, no wood.
// Writes gpws_computer.bbmodel. Model faces NORTH (front = z 0). Refine in Blockbench;
// do NOT rerun over a refined source. Guide: docs/visual-design-guide.md; identity: README.md.
// Iron dominates (base, body, top) and brass marks the "smart" parts (lid band, rivets, the
// screen frame and the test button), so it does not read as the Advanced Terrain Scanner.
import { canvas, blit, checkMap, texture, face, cube, group, writeProject, px } from '../_tools/bbtools.mjs';

// --- Pixel maps --------------------------------------------------------------------------
// Body faces are 14 (wide) x 12 (y 14 -> 2). Iron frame: dark rim, lit bevel top/left,
// shaded bevel bottom/right, brass rivets 2 px in from the corners. Row 0 is the lid's shadow.
const bodyFace = middle => checkMap('body', [
  'nnnnnnnnnnnnnn',
  'nWvvvvvvvvvvpn',
  'nv6qqqqqqqq6on',
  'nvqqqqqqqqqqon',
  ...middle,
  'nvqqqqqqqqqqon',
  'nv6qqqqqqqq6on',
  'npooooooooooon',
  'nnnnnnnnnnnnnn',
], 14, 12);
// Sides: two cooling louvres (dark slot over a lit lip): a cabinet that runs warm.
const SIDE = bodyFace(['nvqwwwwwwwwqon', 'nvqvvvvvvvvqon', 'nvqwwwwwwwwqon', 'nvqvvvvvvvvqon']);
// Rear: the data socket (guide §8.3), where the avionics cable usually comes in.
const REAR = bodyFace(['nvqqqzzzxqqqon', 'nvqqqzwwxqqqon', 'nvqqqzwwxqqqon', 'nvqqqxxxxqqqon']);
// Front: plain iron; the screen and the test button cover the middle.
const FRONT = bodyFace(Array(4).fill('nvqqqqqqqqqqon'));
// Lid band (16 x 2) in brass, base plinth (16 x 2) in iron.
const LID_SIDE = checkMap('lid side', ['2778888888887762', '1344444444444321'], 16, 2);
const BASE_SIDE = checkMap('base side', ['nWvvvvvvvvvvvvpn', 'nooooooooooooooo'.slice(0, 15) + 'n'], 16, 2);

// Screen (12 x 8), after the CC:Tweaked computer: brass frame around a near-black 10 x 6 screen
// with a light prompt ">" (parchment white). Armed, a cursor "_" blinks after the prompt: an
// animated strip of two frames swapped in by the gpws_computer_armed model (README).
const screenFace = (cursor) => checkMap('screen', [
  '877777777776',
  '7LLLLLLLLLL4',
  '7LKLLLLLLLL4',
  '7LLKLLLLLLL4',
  '7LKL' + (cursor ? 'KK' : 'LL') + 'LLLLL4',
  '7LLLLLLLLLL4',
  '7LLLLLLLLLL4',
  '644444444443',
], 12, 8);
const screenTexture = frames => {
  const img = canvas(16, 16 * frames.length, 'n');
  frames.forEach((cursor, i) => blit(img, screenFace(cursor), 0, 16 * i));
  return img;
};
// Top (16 x 16): brass rim continuing the lid band, iron plate with a vent grille and rivets.
const TOP = checkMap('top', [
  '1788888888888871',
  '7nnnnnnnnnnnnnn4',
  '7nWvvvvvvvvvvpn4',
  '7nv6qqqqqqqq6on4',
  '7nvqqqqqqqqqqon4',
  '7nvqwwwwwwwwqon4',
  '7nvqvvvvvvvvqon4',
  '7nvqwwwwwwwwqon4',
  '7nvqvvvvvvvvqon4',
  '7nvqwwwwwwwwqon4',
  '7nvqvvvvvvvvqon4',
  '7nvqqqqqqqqqqon4',
  '7nv6qqqqqqqq6on4',
  '7npooooooooooon4',
  '7nnnnnnnnnnnnnn4',
  '1344444444444431',
], 16, 16);
const BEZEL_TOP = checkMap('bezel top', ['888888888887'], 12, 1);
const BEZEL_BOTTOM = checkMap('bezel bottom', ['433333333332'], 12, 1);
const BEZEL_SIDE = checkMap('bezel side', ['6', '5', '5', '5', '5', '5', '5', '4'], 1, 8);
// Brass push button (2 x 2), the GPWS self-test.
const BUTTON_FRONT = checkMap('button front', ['86', '63'], 2, 2);
const BUTTON_TOP = checkMap('button top', ['87'], 2, 1);
const BUTTON_BOTTOM = checkMap('button bottom', ['43'], 2, 1);
const BUTTON_SIDE = checkMap('button side', ['5', '4'], 1, 2);

// --- Texture assembly (1:1) ---------------------------------------------------------------
const AT = {
  bezelTop: [0, 6, 12, 7], bezelBottom: [0, 7, 12, 8], bezelSide: [12, 0, 13, 8],
  buttonFront: [0, 8, 2, 10], buttonTop: [2, 8, 4, 9], buttonBottom: [2, 9, 4, 10], buttonSide: [4, 8, 5, 10],
  side: [16, 0, 30, 12], rear: [0, 16, 14, 28], front: [14, 16, 28, 28],
  lidSide: [0, 28, 16, 30], baseSide: [0, 30, 16, 32],
};
const MAPS = {
  bezelTop: BEZEL_TOP, bezelBottom: BEZEL_BOTTOM, bezelSide: BEZEL_SIDE,
  buttonFront: BUTTON_FRONT, buttonTop: BUTTON_TOP, buttonBottom: BUTTON_BOTTOM, buttonSide: BUTTON_SIDE,
  side: SIDE, rear: REAR, front: FRONT, lidSide: LID_SIDE, baseSide: BASE_SIDE,
};
function atlas() {
  const img = canvas(32, 32, 'n');
  for (const [name, map] of Object.entries(MAPS)) blit(img, map, AT[name][0], AT[name][1]);
  return img;
}

// --- Geometry (front = NORTH) -------------------------------------------------------------
// The lid underside only shows its outer pixel ring (the body is inset 1), which is the brass
// frame of create:block/brass_casing; its wooden core stays hidden behind the body.
// Create's assets are All Rights Reserved: referenced by id, never embedded. For the Blockbench
// preview, keep local copies in reference/create-textures/ (gitignored).
const textures = [
  texture('gpws_computer', 32, { source: atlas() }),                                  // 0
  texture('industrial_iron_block', 16, { namespace: 'create', particle: true,
    relativePath: '../../reference/create-textures/industrial_iron_block.png' }),     // 1
  texture('brass_casing', 16, { namespace: 'create',
    relativePath: '../../reference/create-textures/brass_casing.png' }),              // 2
  texture('gpws_computer_top', 16, { source: (() => { const img = canvas(16, 16, 'n'); blit(img, TOP, 0, 0); return img; })() }), // 3
  texture('gpws_computer_screen', 16, { source: screenTexture([false]) }),                        // 4
  // Variant only: 8 ticks per frame, the CC:Tweaked cursor rhythm.
  texture('gpws_computer_screen_armed', 16, { source: screenTexture([true, false]), frameTime: 8 }), // 5
];
const A = name => face(AT[name], 0);
const FULL = px(16, [0, 0, 16, 16]);
const iron = face(FULL, 1), brass = face(FULL, 2), top = face(FULL, 3);
const ring = side => ({ north: A(side), south: A(side), east: A(side), west: A(side) });

const elements = [
  cube('base', [0, 0, 0], [16, 2, 16], { ...ring('baseSide'), up: iron, down: iron }, 1),
  // Up/down faces of the body sit against the lid and the base: left disabled (no z-fighting).
  cube('body', [1, 2, 1], [15, 14, 15], { north: A('front'), south: A('rear'), east: A('side'), west: A('side') }, 1),
  cube('lid', [0, 14, 0], [16, 16, 16], { ...ring('lidSide'), up: top, down: brass }, 3),
  cube('screen', [2, 5, 0], [14, 13, 1], { north: face(px(16, [0, 0, 12, 8]), 4), east: A('bezelSide'), west: A('bezelSide'),
    up: A('bezelTop'), down: A('bezelBottom') }, 6),
  cube('test_button', [7, 2, 0], [9, 4, 1], { north: A('buttonFront'), east: A('buttonSide'), west: A('buttonSide'),
    up: A('buttonTop'), down: A('buttonBottom') }, 6),
];
const groups = [group('housing', [8, 8, 8], elements.slice(0, 3)), group('panel', [8, 8, 0], elements.slice(3), 6)];
writeProject(new URL('gpws_computer.bbmodel', import.meta.url), { name: 'gpws_computer', elements, groups, textures });
console.log(`Wrote gpws_computer.bbmodel: ${elements.length} cubes, ${textures.length} textures.`);
