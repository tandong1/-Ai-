#!/bin/bash
# Docker环境下的数据库备份脚本

set -e

BACKUP_DIR="./backups/mysql"
DATE=$(date +%Y%m%d_%H%M%S)
CONTAINER_NAME="study_mysql"

# 创建备份目录
mkdir -p $BACKUP_DIR

echo "开始备份数据库..."

# 使用docker exec备份MySQL
docker exec $CONTAINER_NAME mysqldump \
  -u study_user \
  -p${MYSQL_PASSWORD} \
  study_system \
  | gzip > $BACKUP_DIR/backup_$DATE.sql.gz

# 检查备份是否成功
if [ $? -eq 0 ]; then
    echo "备份成功: $BACKUP_DIR/backup_$DATE.sql.gz"

    # 只保留最近7天的备份
    find $BACKUP_DIR -name "backup_*.sql.gz" -mtime +7 -delete
    echo "已清理7天前的旧备份"
else
    echo "备份失败"
    exit 1
fi

# 备份文件目录
echo "备份上传文件..."
tar -czf $BACKUP_DIR/uploads_$DATE.tar.gz ./data/uploads/
echo "上传文件备份完成"
