const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const choices = require('../utils/choice-options');
const labels = ['A', 'B', 'C', 'D'];
function page(type = 'choice') {
  let config, submitted;
  vm.runInNewContext(fs.readFileSync(require.resolve('../pages/subject-challenge/subject-challenge.js'), 'utf8'), {
    require: name => name.includes('choice-options') ? {
      ...choices, shuffleChoiceOptions: (options, order) => choices.shuffleChoiceOptions(options, order, () => 0.999)
    } : { post: (url, payload) => { submitted = payload; return new Promise(() => {}); } },
    Page: value => { config = value; }, getApp: () => ({ globalData: {} }),
    wx: { showLoading() {}, showToast() {} }, console: { log() {}, error() {} }
  });
  const question = { id: 1, type, options: ['苹果', '香蕉', '橙子', '葡萄'], correctAnswer: 'B' };
  config.data.questions = [question];
  config.data.currentQuestionData = question;
  config.data.answers = { 0: 0 };
  config.setData = values => Object.assign(config.data, values);
  return { config, submitted: () => submitted };
}
test('shuffling moves every option and keeps its original answer mapping', () => {
  for (let seed = 1; seed <= 100; seed++) {
    let state = seed;
    const random = () => ((state = (state * 16807) % 2147483647) - 1) / 2147483646;
    const result = choices.shuffleChoiceOptions(['a', 'b', 'c', 'd'], undefined, random);
    assert.equal(new Set(result.options).size, 4);
    result.optionOrder.forEach((original, index) => {
      assert.notEqual(original, index);
      assert.equal(result.options[index], ['a', 'b', 'c', 'd'][original]);
    });
  }
});
test('first wrong answer then retry changes positions and clears the selection', () => {
  const { config } = page();
  config.submitCurrentAnswer();
  assert.equal(config.data.attemptCount[0], 1);
  config.retryQuestion();
  assert.deepEqual(Array.from(config.data.currentQuestionData.options), ['香蕉', '橙子', '葡萄', '苹果']);
  assert.equal(config.data.answers[0], undefined);
  assert.equal(config.data.showAnalysis, false);
  assert.equal(config.data.currentQuestionData.displayCorrectAnswer, 'A');
  const options = config.data.currentQuestionData.options;
  config.retryQuestion();
  assert.equal(config.data.currentQuestionData.options, options);
});
test('retry correct answer is judged and submitted using the original letter', () => {
  const { config, submitted } = page();
  config.submitCurrentAnswer(); config.retryQuestion();
  config.data.answers[0] = 0;
  config.submitCurrentAnswer();
  assert.equal(config.data.isCorrect, true);
  config.finishChallenge();
  assert.equal(submitted().answers[0].userAnswer, 'B');
  assert.equal(submitted().answers[0].attemptCount, 2);
});
test('second wrong answer shows the correct letter in the new display order', () => {
  const { config } = page();
  config.submitCurrentAnswer(); config.retryQuestion();
  config.data.answers[0] = 1;
  config.submitCurrentAnswer();
  assert.equal(config.data.isCorrect, false);
  assert.equal(config.data.showCorrectAnswer, true);
  assert.ok(config.data.currentFeedback.endsWith('A'));
});
test('fill-in questions retry without shuffling', () => {
  const { config } = page('fill');
  config.data.answers[0] = '错误';
  config.submitCurrentAnswer();
  const question = config.data.currentQuestionData;
  config.retryQuestion();
  assert.equal(config.data.currentQuestionData, question);
  assert.equal(config.data.currentQuestionData.retryShuffled, undefined);
});
test('two options exchange places and a single option is handled without looping', () => {
  assert.deepEqual(choices.shuffleChoiceOptions(['a', 'b'], undefined, () => 0.999).options, ['b', 'a']);
  assert.deepEqual(choices.shuffleChoiceOptions(['a']).options, ['a']);
});
