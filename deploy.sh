#!/bin/bash
# Docker部署脚本

set -e

echo "===== 小小学霸系统 Docker 部署脚本 ====="

# 检查Docker是否安装
if ! command -v docker &> /dev/null; then
    echo "错误: Docker未安装，请先安装Docker"
    exit 1
fi

# 检查Docker Compose是否安装
if ! command -v docker-compose &> /dev/null; then
    echo "错误: Docker Compose未安装，请先安装Docker Compose"
    exit 1
fi

# 创建必要的目录
echo "创建数据目录..."
mkdir -p data/mysql
mkdir -p data/uploads
mkdir -p logs/mysql
mkdir -p logs/backend
mkdir -p logs/scheduler

# 设置目录权限
echo "设置目录权限..."
chmod -R 755 data
chmod -R 755 logs

# 检查环境变量文件
if [ ! -f .env ]; then
    echo "警告: .env文件不存在，复制.env.example为.env"
    cp .env.example .env
    echo "请编辑.env文件，填入正确的配置信息"
    exit 1
fi

# 停止旧容器
echo "停止旧容器..."
docker-compose down

# 构建镜像
echo "构建Docker镜像..."
docker-compose build

# 启动服务
echo "启动服务..."
docker-compose up -d

# 等待MySQL启动
echo "等待MySQL启动..."
sleep 10

# 检查服务状态
echo "检查服务状态..."
docker-compose ps

echo ""
echo "===== 部署完成 ====="
echo "后端服务: http://localhost:3000"
echo "MySQL端口: 3306"
echo ""
echo "查看日志: docker-compose logs -f"
echo "停止服务: docker-compose down"
echo "重启服务: docker-compose restart"
