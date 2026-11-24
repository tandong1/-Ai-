const { test } = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
function load(file, extras = {}) {
  let page;
  vm.runInNewContext(fs.readFileSync(require.resolve(file), 'utf8'), {
    Page: value => { page = value; }, getApp: () => ({ globalData: {} }),
    require: name => name.includes('choice-options') ? require('../utils/choice-options') : {},
    console: { log() {}, error() {} }, ...extras
  });
  page.setData = values => Object.assign(page.data, values);
  return page;
}
test('result and toast display the two points returned by the server', async () => {
  let toast;
  const store = { currentUser: { totalPoints: 10 } };
  const result = { pointsEarned: 2, newBalance: 12, correctCount: 1, details: [] };
  const page = load('../pages/subject-challenge/subject-challenge.js', {
    require: name => name.includes('choice-options') ? require('../utils/choice-options') : { post: () => Promise.resolve(result) },
    wx: { showLoading() {}, hideLoading() {}, showToast: value => { toast = value; },
      getStorageSync: key => store[key], setStorageSync: (key, value) => { store[key] = value; } },
    getCurrentPages: () => []
  });
  page.data.questions = [{ id: 1, type: 'choice', options: ['甲', '乙'], correctAnswer: 'A' }];
  page.data.answers = { 0: 0 };
  page.finishChallenge();
  await new Promise(resolve => setImmediate(resolve));
  assert.equal(page.data.totalPoints, 2);
  assert.equal(page.data.showResult, true);
  assert.equal(store.currentUser.totalPoints, 12);
  assert.equal(toast.title, '首次全对！+2积分');
});
test('reopening a completed challenge preserves its actual award', () => {
  const page = load('../pages/subject-challenge/subject-challenge.js');
  page.loadCompletedQuestions({ pointsEarned: 2, questions: [
    { questionId: 1, questionType: 'choice', options: ['甲', '乙'], userAnswer: 'A', isCorrect: true }
  ] });
  assert.equal(page.data.totalPoints, 2);
});
test('history displays actual awards and supports older responses', () => {
  const page = load('../pages/history/history.js');
  assert.equal(page.buildCard({ totalQuestions: 1, correctCount: 1, earnedPoints: 2 }).earnedPoints, 2);
  assert.equal(page.buildCard({ totalQuestions: 1, correctCount: 1 }).earnedPoints, 1);
});
