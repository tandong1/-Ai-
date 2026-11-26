-- 在部署新后端前执行一次；已有礼物默认实物。
ALTER TABLE gifts ADD COLUMN gift_type VARCHAR(20) NOT NULL DEFAULT 'physical';
ALTER TABLE exchange_records
 ADD COLUMN request_id VARCHAR(64) NULL,
 ADD COLUMN gift_type VARCHAR(20) NOT NULL DEFAULT 'physical',
 ADD COLUMN recipient VARCHAR(80) NULL,
 ADD COLUMN account VARCHAR(120) NULL,
 ADD COLUMN password_encrypted TEXT NULL,
 ADD COLUMN game_region VARCHAR(100) NULL,
 ADD COLUMN game_name VARCHAR(100) NULL,
 ADD COLUMN remark VARCHAR(500) NULL,
 ADD UNIQUE KEY uk_exchange_request (user_id,request_id);
-- 将虚拟商品的 gift_type 设置为 'virtual'。
