# 每日任务刮奖

三科当天都必须有题，且每一道题都要有当前用户当天的非空作答；不要求全部答对。重复记录、往日记录、其他用户记录和 is_used 标记不能代替完整作答。首页进度和刮奖资格共用这套后端查询。

三科全部完成后，首页「今日任务进度」原位置隐藏各科状态，显示刮奖膜。手指擦除约 40% 后揭晓结果；每天每个用户只生成一份结果，不论是否中奖都有鼓励语。额外积分不影响原有完成科目获得的积分。

## 上线顺序

1. 在 `study_system` 数据库依次执行 `docs/migrations/20261120_daily_scratch.sql`、`docs/migrations/20261121_scratch_messages.sql`。脚本可重复执行，不覆盖已有配置或奖励。
2. 部署新后端。
3. 在微信开发者工具编译、真机预览并发布小程序。

2026-11-21 已在京东云 `study_system` 数据库执行上述迁移，核对两张表创建成功，初始配置为 30% 中奖、奖励 1 分。同日已执行鼓励语迁移，创建 `scratch_messages` 并导入 6 条语句。尚未部署新后端。

## 数据库动态调整

```sql
-- 50% 中奖率，中奖额外获得 2 分
UPDATE scratch_config
SET win_probability = 5000, reward_points = 2
WHERE id = 1;
```

`win_probability` 为万分比，范围 0～10000；0 永不获得额外积分，10000 每次都获得。`reward_points` 为中奖时的固定积分，范围 1～10000。初始配置是 30%、1 分。每次首次开奖实时读取配置，无须重启，已生成的结果保持不变。鼓励语每次首次开奖从 `scratch_messages` 表中等概率随机选择，仅选择 `enabled=1` 且非空的语句。没有可用语句时不消耗开奖机会、不加分，配置恢复后可以重试。已刮出的语句快照保存在 `daily_scratch_rewards.message`，修改配置不会改变历史结果。

```sql
-- 新增一句
INSERT INTO scratch_messages (message, enabled) VALUES ('今天很棒，明天再来！', 1);
-- 修改文案
UPDATE scratch_messages SET message = '坚持就是进步！' WHERE id = 2;
-- 停用（重新启用设为 1）
UPDATE scratch_messages SET enabled = 0 WHERE id = 2;
```

`daily_scratch_rewards` 保存用户、数据库日期、实际奖励和鼓励语；中奖时同时写入 `users.total_points` 与 `points_records`（`source_type=bonus`）。整个结算在同一事务内，用户行锁和用户日期唯一键防止并发重复领取。题目提交结算使用相同用户行锁，避免覆盖刮奖积分。

## 接口

- `GET /rewards/scratch`：读取今日资格、是否揭晓、已有结果与积分余额，不触发开奖。
- `POST /rewards/scratch/reveal`：传入 `{"rewardDate":"2026-11-20"}`；后端校验三科完成状态和数据库日期，再结算。重复请求返回原结果。客户端日期过期时重新读取状态。

两个接口均通过现有 JWT 验证用户身份，客户端不能传入中奖概率、奖励积分或用户 ID。

## 验证

```sh
node --test miniprogram-1/tests/*.test.js
cd study-backend
mvn -Dtest=ScratchRewardServiceTest,QuestionRewardServiceTest test
```

已在 Java 17 下编译通过：前端 16 项、后端 19 项测试全部通过，覆盖数据库语句动态修改、停用过滤、无语句时重试与历史快照。

后端测试使用 H2 的 MySQL 模式验证 0%/100% 边界、配置实时生效、资格、日期、重复领取、并发领取和事务回滚。仍需在微信真机验证 Canvas 擦除手势，以及在实际 MySQL 环境验证迁移与接口联调。

2026-11-24 已执行 `20261121_refresh_scratch_messages.sql`：替换首批 6 条语句，扩充至 12 条（8 条科普、4 条鼓励与提问），核对线上全部启用。历史开奖结果未修改。

## 科目首次全对奖励

当天整科题目均为首次答对（每题 `first_attempt_correct=true`，且没有错误或重试记录），奖励 2 分；其余沿用完成奖励 1 分。部分题目答对不能获得首次全对奖励。新增结算写入 `points_records`，结果页、重新查看及历史卡片读取实际奖励；旧记录无积分明细时沿用原有 1 分。刮奖额外积分单独结算。本规则需要部署新后端并发布新版小程序后生效。
