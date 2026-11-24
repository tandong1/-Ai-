/** 打乱显示顺序，保留每个选项对应的原始答案位置。 */
function shuffleChoiceOptions(options, originalOrder, random = Math.random) {
  const order = originalOrder || options.map((_, index) => index);
  const positions = options.map((_, index) => index);
  if (positions.length < 2) return { options: options.slice(), optionOrder: order.slice() };

  let shuffled;
  // 避免随机后顺序未变，或正确选项仍停留在原来的位置。
  for (let attempt = 0; attempt < 10; attempt++) {
    shuffled = positions.slice();
    for (let i = shuffled.length - 1; i > 0; i--) {
      const j = Math.floor(random() * (i + 1));
      [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
    }
    if (shuffled.every((position, index) => position !== index)) break;
  }
  if (shuffled.some((position, index) => position === index)) {
    shuffled = positions.slice(1).concat(positions[0]);
  }
  return {
    options: shuffled.map(index => options[index]),
    optionOrder: shuffled.map(index => order[index])
  };
}

function originalAnswer(question, displayIndex, labels) {
  const originalIndex = question.optionOrder ? question.optionOrder[displayIndex] : displayIndex;
  return labels[originalIndex];
}

function displayedCorrectAnswer(question, labels) {
  if (!question.optionOrder) return question.correctAnswer;
  const originalIndex = labels.findIndex(label => label.toLowerCase() === String(question.correctAnswer).trim().toLowerCase());
  const displayIndex = question.optionOrder.indexOf(originalIndex);
  return displayIndex < 0 ? question.correctAnswer : labels[displayIndex];
}

module.exports = { shuffleChoiceOptions, originalAnswer, displayedCorrectAnswer };
