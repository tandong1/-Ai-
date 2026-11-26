const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
function setup(post) {
  let page; const stored=[]; const app={globalData:{}};
  vm.runInNewContext(fs.readFileSync(require.resolve('../pages/shop/shop.js'),'utf8'),{
    Page: p=>page=p, getApp:()=>app,
    require:()=>({post,get:()=>Promise.resolve([])}),
    wx:{setStorageSync:(...args)=>stored.push(args),showToast(){},showLoading(){},hideLoading(){}},console
  });
  page.setData=values=>Object.assign(page.data,values);
  page.data.currentUser={id:1,totalPoints:100}; page.data.totalPoints=100;
  page.data.selectedGift={id:2,points:30,giftType:'physical'};
  page.data.exchangeRequestId='same-request';
  page.data.exchangeForm={recipient:'姓名',phone:'13800000000',address:'地址'};
  return {page,stored,app};
}
const tick=()=>new Promise(resolve=>setImmediate(resolve));
test('exchange waits for server and persists the authoritative balance',async()=>{
  let resolve, payload;
  const {page,stored,app}=setup((url,data)=>{assert.equal(url,'/gifts/exchange');payload=data;return new Promise(r=>resolve=r);});
  page.exchangeGift();assert.equal(page.data.totalPoints,100);assert.equal(stored.length,0);
  resolve({totalPoints:65});await tick();
  assert.equal(payload.requestId,'same-request');assert.equal(page.data.totalPoints,65);
  assert.equal(app.globalData.currentUser.totalPoints,65);assert.equal(stored[0][1].totalPoints,65);
  assert.equal(page.data.selectedGift,null);assert.equal(Object.keys(page.data.exchangeForm).length,0);
});
test('failure preserves balance and request ID for retry without local awards',async()=>{
  let calls=0;
  const {page,stored}=setup(()=>{calls++;return Promise.reject(new Error('network'));});
  page.exchangeGift();page.exchangeGift();await tick();
  assert.equal(calls,1);assert.equal(stored.length,0);assert.equal(page.data.totalPoints,100);
  assert.equal(page.data.exchangeRequestId,'same-request');assert.equal(page.data.submitting,false);
});
test('physical address and virtual account details are required before submitting',()=>{
  let calls=0;const {page}=setup(()=>{calls++;return Promise.resolve({});});
  page.data.exchangeForm.address='';page.exchangeGift();assert.equal(calls,0);
  page.data.selectedGift.giftType='virtual';page.data.exchangeForm={account:'a'};page.exchangeGift();assert.equal(calls,0);
});
