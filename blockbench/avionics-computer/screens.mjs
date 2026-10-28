// Screen pixel maps of the Avionics Computer (12 x 8 on a 16 x 16 frame), after the CC:Tweaked
// computer: brass frame around a near-black 10 x 6 screen with a light prompt ">". Each runtime
// state swaps only this texture (README, "Estados da tela"); geometry never changes.
import { canvas, blit, checkMap } from '../_tools/bbtools.mjs';

const FRAME_TOP = '877777777776';
const FRAME_BOTTOM = '644444444443';
// Glyph cells sit right of the prompt, inside the lens (columns 4-9 of rows 1-6).
const screenFace = (glyph = ['......', '......', '......', '......', '......', '......']) => {
  const prompt = ['LLLLLLLLLL', 'LKLLLLLLLL', 'LLKLLLLLLL', 'LKLLLLLLLL', 'LLLLLLLLLL', 'LLLLLLLLLL'];
  const rows = prompt.map((row, y) => {
    const cells = [...row];
    [...glyph[y]].forEach((key, x) => { if (key !== '.') cells[3 + x] = key; });
    return `7${cells.join('')}4`;
  });
  return checkMap('screen', [FRAME_TOP, ...rows, FRAME_BOTTOM], 12, 8);
};

// Normal: the CC:Tweaked cursor "_" blinks after the prompt.
const CURSOR = ['......', '......', '......', '.KK...', '......', '......'];
// Caution / warning: a two-pixel "!" flashing in the alert colour (guide §9.4: amber = light
// brass tones, red = redstone ramp). INOP: a steady amber "x".
const bang = key => ['..' + key + key + '..', '..' + key + key + '..', '..' + key + key + '..',
  '......', '..' + key + key + '..', '......'];
const CROSS = ['......', '.6.6..', '..6...', '.6.6..', '......', '......'];

export const SCREENS = {
  avionics_computer_screen: { frames: [null] },
  avionics_computer_screen_normal: { frames: [CURSOR, null], frameTime: 8 },
  avionics_computer_screen_caution: { frames: [bang('8'), null], frameTime: 6 },
  avionics_computer_screen_warning: { frames: [bang('U'), null], frameTime: 4 },
  avionics_computer_screen_inop: { frames: [CROSS] },
};

export function screenTexture(frames) {
  const img = canvas(16, 16 * frames.length, 'n');
  frames.forEach((glyph, i) => blit(img, screenFace(glyph ?? undefined), 0, 16 * i));
  return img;
}
