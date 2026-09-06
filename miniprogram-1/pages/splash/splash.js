Page({
  data: {
    fadeIn: false,
    textShow: false
  },

  onLoad: function() {
    const that = this;

    // 延迟显示动画
    setTimeout(() => {
      that.setData({ fadeIn: true });
    }, 100);

    // 延迟显示文字
    setTimeout(() => {
      that.setData({ textShow: true });
    }, 500);

    // 2.5秒后跳转到用户选择页
    setTimeout(() => {
      wx.reLaunch({
        url: '/pages/user-select/user-select'
      });
    }, 2500);
  }
});
