/**
 * 封装的HTTP请求工具
 */
const http = {
  // 临时直接写在这里
  apiUrl: 'https://www.wangmian.xyz/api',
  timeout: 30000,

  /**
   * 发起请求
   * @param {String} url - 接口路径（不含baseURL）
   * @param {Object} options - 请求配置
   */
  request(url, options = {}) {
    return new Promise((resolve, reject) => {
      // 获取token
      const token = wx.getStorageSync('token') || '';

      // 完整URL
      const fullUrl = this.apiUrl + url;

      wx.request({
        url: fullUrl,
        method: options.method || 'GET',
        data: options.data || {},
        header: {
          'Content-Type': 'application/json',
          'Authorization': token ? `Bearer ${token}` : '',
          ...options.header
        },
        timeout: this.timeout,
        success: (res) => {
          // 添加调试日志
          console.log('=== HTTP响应 ===');
          console.log('URL:', fullUrl);
          console.log('statusCode:', res.statusCode);
          console.log('res.data:', JSON.stringify(res.data, null, 2));
          console.log('res.data.code:', res.data.code);
          console.log('res.data.data:', res.data.data);

          // 统一处理响应
          if (res.statusCode === 200) {
            if (res.data.code === 200) {
              resolve(res.data.data);
            } else {
              wx.showToast({
                title: res.data.message || '请求失败',
                icon: 'none'
              });
              reject(res.data);
            }
          } else if (res.statusCode === 401) {
            // token过期，跳转登录
            wx.showToast({
              title: '登录已过期',
              icon: 'none'
            });
            wx.removeStorageSync('token');
            wx.removeStorageSync('currentUserId');
            wx.reLaunch({
              url: '/pages/user-select/user-select'
            });
            reject(res);
          } else {
            wx.showToast({
              title: '网络请求失败',
              icon: 'none'
            });
            reject(res);
          }
        },
        fail: (err) => {
          console.error('请求失败:', err);
          wx.showToast({
            title: '网络连接失败',
            icon: 'none'
          });
          reject(err);
        }
      });
    });
  },

  /**
   * GET请求
   */
  get(url, data = {}) {
    return this.request(url, {
      method: 'GET',
      data
    });
  },

  /**
   * POST请求
   */
  post(url, data = {}) {
    return this.request(url, {
      method: 'POST',
      data
    });
  },

  /**
   * PUT请求
   */
  put(url, data = {}) {
    return this.request(url, {
      method: 'PUT',
      data
    });
  },

  /**
   * DELETE请求
   */
  delete(url, data = {}) {
    return this.request(url, {
      method: 'DELETE',
      data
    });
  }
};

module.exports = http;
