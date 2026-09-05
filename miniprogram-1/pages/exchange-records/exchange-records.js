var app = getApp();

Page({
  data: {
    records: []
  },

  onLoad: function() {
    this.loadRecords();
  },

  loadRecords: function() {
    var records = wx.getStorageSync('exchange_records') || [];
    var currentUserId = app.globalData.currentUser ? app.globalData.currentUser.id : 0;

    var userRecords = records.filter(function(record) {
      return record.userId === currentUserId;
    });

    for (var i = 0; i < userRecords.length; i++) {
      userRecords[i].timeStr = this.formatTime(userRecords[i].time);
    }

    this.setData({ records: userRecords });
  },

  formatTime: function(timestamp) {
    var date = new Date(timestamp);
    var year = date.getFullYear();
    var month = date.getMonth() + 1;
    var day = date.getDate();
    var hour = date.getHours();
    var minute = date.getMinutes();

    month = month < 10 ? '0' + month : month;
    day = day < 10 ? '0' + day : day;
    hour = hour < 10 ? '0' + hour : hour;
    minute = minute < 10 ? '0' + minute : minute;

    return year + '-' + month + '-' + day + ' ' + hour + ':' + minute;
  }
});
