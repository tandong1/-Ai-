#!/bin/bash
# 数据库恢复脚本

set -e

if [ -z "$1" ]; then
    echo "用法: ./restore.sh <备份文件路径>"
    echo "示例: ./restore.sh backups/mysql/backup_20240115_020000.sql.gz"
    exit 1
fi

BACKUP_FILE=$1
CONTAINER_NAME="study_mysql"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "错误: 备份文件不存在: $BACKUP_FILE"
    exit 1
fi

echo "警告: 此操作将覆盖当前数据库！"
read -p "是否继续? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "取消恢复"
    exit 0
fi

echo "开始恢复数据库..."

# 解压并恢复
gunzip < $BACKUP_FILE | docker exec -i $CONTAINER_NAME mysql \
  -u study_user \
  -p${MYSQL_PASSWORD} \
  study_system

if [ $? -eq 0 ]; then
    echo "数据库恢复成功"
else
    echo "数据库恢复失败"
    exit 1
fi
