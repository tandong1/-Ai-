const { test } = require('node:test');
const assert = require('node:assert/strict');
let definition;
global.Component = config => { definition = config; };
require('../components/scratch-card/scratch-card');
function surface() {
  let reveals = 0;
  const noop = () => {};
  return Object.assign({
    ctx: { beginPath: noop, moveTo: noop, lineTo: noop, stroke: noop, arc: noop, fill: noop },
    surface: { width: 320, height: 120 }, cells: new Set(),
    triggerEvent: () => reveals++, count: () => reveals
  }, definition.methods);
}
const touch = (x, y) => ({ touches: [{ x, y }] });
test('repeated scratching of one spot does not unlock the reward', () => {
  const card = surface();
  for (let i = 0; i < 100; i++) card.start(touch(10, 10));
  assert.equal(card.count(), 0);
});
test('scratching over 40% reveals exactly once', () => {
  const card = surface();
  for (let y = 10; y < 120; y += 15) {
    card.start(touch(0, y));
    card.move(touch(320, y));
    card.end();
  }
  assert.equal(card.count(), 1);
});
test('separate touches do not erase a connecting line', () => {
  const card = surface();
  card.start(touch(0, 0)); card.end();
  card.start(touch(320, 120)); card.end();
  assert.equal(card.count(), 0);
  assert.ok(card.cells.size < 30);
});
