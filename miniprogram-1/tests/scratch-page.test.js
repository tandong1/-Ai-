const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
function page(http) {
  let config;
  const stored = { currentUser: { id: 1, totalPoints: 10 } };
  const app = { globalData: {} };
  vm.runInNewContext(fs.readFileSync(require.resolve('../pages/daily-challenge/daily-challenge.js'), 'utf8'), {
    require: () => http, getApp: () => app, Page: value => { config = value; },
    wx: { getStorageSync: key => stored[key], setStorageSync: (key, value) => { stored[key] = value; } },
    console: { error: () => {} }
  });
  config.setData = values => Object.assign(config.data, values);
  config.data.scratch = { eligible: true, revealed: false, rewardDate: '2026-11-20' };
  return { config, stored, app };
}
const settle = () => new Promise(resolve => setImmediate(resolve));
test('rapid reveal events send only one request and persist returned balance', async () => {
  let finish, calls = 0;
  const pending = new Promise(resolve => { finish = resolve; });
  const { config, stored, app } = page({ post: () => { calls++; return pending; } });
  config.revealScratch(); config.revealScratch();
  assert.equal(calls, 1);
  finish({ eligible: true, revealed: true, points: 2, totalPoints: 12, message: '今天很棒' });
  await settle();
  assert.equal(config.data.scratchLoading, false);
  assert.equal(config.data.totalPoints, 12);
  assert.equal(stored.currentUser.totalPoints, 12);
  assert.equal(app.globalData.totalPoints, 12);
  config.revealScratch(); assert.equal(calls, 1);
});
test('failed requests allow retry and never award local points', async () => {
  let calls = 0;
  const { config, stored } = page({ post: () => { calls++; return Promise.reject(new Error('offline')); } });
  config.revealScratch(); await settle();
  assert.equal(config.data.scratchLoading, false);
  assert.equal(stored.currentUser.totalPoints, 10);
  config.revealScratch(); await settle();
  assert.equal(calls, 2);
});
test('incomplete tasks cannot trigger a claim', () => {
  let calls = 0;
  const { config } = page({ post: () => { calls++; } });
  config.data.scratch.eligible = false;
  config.revealScratch();
  assert.equal(calls, 0);
});
test('a late status response cannot overwrite a freshly claimed reward', async () => {
  let finishStatus;
  const { config } = page({
    get: () => new Promise(resolve => { finishStatus = resolve; }),
    post: () => Promise.resolve({ eligible: true, revealed: true, points: 1, totalPoints: 11 })
  });
  config.loadScratch(); config.revealScratch(); await settle();
  finishStatus({ eligible: true, revealed: false, totalPoints: 10 }); await settle();
  assert.equal(config.data.scratch.revealed, true);
  assert.equal(config.data.totalPoints, 11);
});
