#!/bin/bash
# 服务器重启脚本

echo "=== 步骤1: 拉取最新代码 ==="
git pull origin main

echo ""
echo "=== 步骤2: 停止并删除旧容器 ==="
docker compose down backend

echo ""
echo "=== 步骤3: 重新构建镜像 ==="
docker compose build --no-cache backend

echo ""
echo "=== 步骤4: 启动服务 ==="
docker compose up -d backend

echo ""
echo "=== 步骤5: 查看服务状态 ==="
docker compose ps

echo ""
echo "=== 步骤6: 查看启动日志 ==="
docker compose logs -f --tail=50 backend
