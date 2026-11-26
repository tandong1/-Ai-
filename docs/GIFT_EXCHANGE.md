# 礼物兑换

兑换由服务端处理，客户端不再自行扣分或写本地兑换记录。接口只采用 JWT 中的用户 ID，积分价格和库存从数据库读取。

- `POST /api/gifts/exchange`：giftId、requestId，以及配送信息。相同用户相同 requestId 重试只兑换一次。
- `GET /api/gifts/balance`：当前用户数据库余额。
- `GET /api/gifts/exchanges`：当前用户最近 100 条兑换记录，不包含密码。

兑换在一个事务内锁定用户和礼物，校验上架、库存及积分，保存订单、扣减余额和有限库存、写 consume/exchange 积分流水。失败整体回滚。stock=-1 表示不限库存。兑换记录默认 pending，未实现自动发货或取消退款。

## 部署

1. 备份数据库。在现有数据库执行一次 `docs/migrations/20261126_gift_exchange.sql`。全新数据库使用已更新的 `docs/init.sql`，不要再重复执行迁移。
2. 现有礼物默认 physical。将虚拟礼物改成 virtual，例如用数据库客户端执行 `UPDATE gifts SET gift_type='virtual' WHERE id=具体礼物ID;`。
3. 在服务器生成密钥 `openssl rand -base64 32`，将输出写入项目 `.env` 的 `EXCHANGE_ENCRYPTION_KEY`。不要提交真实密钥，长期保留并备份；丢失后无法解密已有密码。
4. 重建：`docker compose up -d --build --no-deps backend`。
5. 微信开发者工具上传发布新版小程序。

实物：收货人、电话、详细地址必填，备注选填。
虚拟商品：账号、游戏名或平台名必填；密码、大区、备注选填。密码使用 AES-256-GCM 加密存入 password_encrypted（Base64 IV:Base64 密文及认证标签），通过环境变量密钥解密。密码不存入小程序缓存，不在记录接口返回，也没有公开解密接口。填写密码但服务端未配置有效密钥时拒绝兑换且不扣分。

原本仅存在手机 exchange_records 缓存中的历史兑换不自动补扣积分或迁移，避免把旧测试数据当作真实订单。

## 验证

`node --test miniprogram-1/tests/*.test.js`

Java 17、Maven：`mvn -f study-backend/pom.xml -Dtest=GiftExchangeServiceTest test`

上线后使用测试账号核对：余额不足、库存为零、不填必填信息均不扣分；正常兑换后重新登录余额保持；网络重试不重复扣分；不同账号不能读取他人的订单。

## 云端适配记录

2026-11-26 已在京东云 study_system 执行本次迁移，验证新增 8 个兑换字段和 uk_exchange_request 唯一索引。现有 6 个礼物保留，ID 1、2 的和平精英礼物设置为 virtual，其余为 physical；当时兑换订单为 0。此云端库无需再次执行上述迁移。服务端加密密钥仍需在服务器 .env 配置，再重建后端并发布小程序。
