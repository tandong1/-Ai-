var app = getApp();

Page({
  data: {
    currentUser: {},
    totalPoints: 0,
    gifts: [
      {
        id: 1,
        name: '乐高积木玩具',
        description: '激发创造力，拼搭快乐时光',
        points: 500,
        images: [
          'https://via.placeholder.com/600x400/4299e1/ffffff?text=乐高1',
          'https://via.placeholder.com/600x400/3b82f6/ffffff?text=乐高2',
          'https://via.placeholder.com/600x400/2563eb/ffffff?text=乐高3'
        ]
      },
      {
        id: 2,
        name: '儿童故事书套装',
        description: '10本精选绘本，开启阅读之旅',
        points: 300,
        images: [
          'https://via.placeholder.com/600x400/10b981/ffffff?text=故事书1',
          'https://via.placeholder.com/600x400/059669/ffffff?text=故事书2',
          'https://via.placeholder.com/600x400/047857/ffffff?text=故事书3'
        ]
      },
      {
        id: 3,
        name: '儿童智能手表',
        description: '定位、通话、计步，安全又有趣',
        points: 800,
        images: [
          'https://via.placeholder.com/600x400/f59e0b/ffffff?text=手表1',
          'https://via.placeholder.com/600x400/d97706/ffffff?text=手表2',
          'https://via.placeholder.com/600x400/b45309/ffffff?text=手表3'
        ]
      },
      {
        id: 4,
        name: '科学实验套装',
        description: '20+趣味实验，探索科学奥秘',
        points: 400,
        images: [
          'https://via.placeholder.com/600x400/8b5cf6/ffffff?text=实验1',
          'https://via.placeholder.com/600x400/7c3aed/ffffff?text=实验2',
          'https://via.placeholder.com/600x400/6d28d9/ffffff?text=实验3'
        ]
      },
      {
        id: 5,
        name: '儿童滑板车',
        description: '可折叠设计，户外运动好伙伴',
        points: 600,
        images: [
          'https://via.placeholder.com/600x400/ec4899/ffffff?text=滑板车1',
          'https://via.placeholder.com/600x400/db2777/ffffff?text=滑板车2',
          'https://via.placeholder.com/600x400/be185d/ffffff?text=滑板车3'
        ]
      },
      {
        id: 6,
        name: '超级飞侠玩具',
        description: '变形机器人，陪伴成长每一天',
        points: 350,
        images: [
          'https://via.placeholder.com/600x400/ef4444/ffffff?text=飞侠1',
          'https://via.placeholder.com/600x400/dc2626/ffffff?text=飞侠2',
          'https://via.placeholder.com/600x400/b91c1c/ffffff?text=飞侠3'
        ]
      }
    ]
  },

  onLoad: function() {
    this.loadUserData();
  },

  onShow: function() {
    this.loadUserData();
  },

  loadUserData: function() {
    // 从本地存储读取最新用户信息
    var currentUser = wx.getStorageSync('currentUser');

    if (currentUser) {
      this.setData({
        currentUser: currentUser,
        totalPoints: currentUser.totalPoints || 0
      });

      // 同步到全局数据
      app.globalData.currentUser = currentUser;
      app.globalData.totalPoints = currentUser.totalPoints || 0;
    } else {
      // 如果本地没有，尝试从全局数据获取
      var currentUser = app.globalData.currentUser || { name: '小朋友', avatar: '', totalPoints: 0 };
      var totalPoints = app.globalData.totalPoints || 0;

      this.setData({
        currentUser: currentUser,
        totalPoints: totalPoints
      });
    }
  },

  viewGiftDetail: function(e) {
    var giftId = e.currentTarget.dataset.id;
    var gift = null;

    for (var i = 0; i < this.data.gifts.length; i++) {
      if (this.data.gifts[i].id === giftId) {
        gift = this.data.gifts[i];
        break;
      }
    }

    if (!gift) return;

    if (this.data.totalPoints < gift.points) {
      wx.showToast({
        title: '积分不足，继续加油学习吧！',
        icon: 'none',
        duration: 2000
      });
      return;
    }

    var that = this;
    wx.showModal({
      title: '确认兑换',
      content: '确定要用 ' + gift.points + ' 积分兑换「' + gift.name + '」吗？',
      confirmText: '确定兑换',
      cancelText: '再想想',
      success: function(res) {
        if (res.confirm) {
          that.exchangeGift(gift);
        }
      }
    });
  },

  exchangeGift: function(gift) {
    var newPoints = this.data.totalPoints - gift.points;

    app.globalData.totalPoints = newPoints;
    if (app.globalData.currentUser) {
      app.globalData.currentUser.points = newPoints;
    }
    app.saveUserData();

    var record = {
      giftId: gift.id,
      giftName: gift.name,
      points: gift.points,
      time: Date.now(),
      userId: app.globalData.currentUser ? app.globalData.currentUser.id : 0
    };

    var records = wx.getStorageSync('exchange_records') || [];
    records.unshift(record);
    wx.setStorageSync('exchange_records', records);

    this.setData({ totalPoints: newPoints });

    wx.showToast({
      title: '兑换成功！',
      icon: 'success'
    });
  },

  viewRecords: function() {
    wx.navigateTo({
      url: '/pages/exchange-records/exchange-records'
    });
  }
});
