const http = require('../../utils/http.js');

Page({
  data: {
    records: []
  },

  onLoad: function() {
    this.loadRecords();
  },

  loadRecords: function() {
    http.get('/gifts/exchanges').then(records => {
      this.setData({ records: records.map(record => ({ ...record, timeStr: this.formatTime(record.time),
        statusText: ({ pending: '待处理', completed: '已完成', cancelled: '已取消' })[record.status] || record.status
      })) });
    }).catch(() => {});
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
