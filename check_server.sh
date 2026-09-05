#!/bin/bash
# 服务器部署检查脚本
# 使用方法: chmod +x check_server.sh && ./check_server.sh

echo "======================================"
echo "服务器部署环境检查"
echo "======================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查函数
check_command() {
    if command -v $1 &> /dev/null; then
        echo -e "${GREEN}✓${NC} $1 已安装: $(command -v $1)"
        if [ ! -z "$2" ]; then
            echo "  版本: $($1 $2 2>&1 | head -n 1)"
        fi
        return 0
    else
        echo -e "${RED}✗${NC} $1 未安装"
        return 1
    fi
}

# 1. 检查操作系统
echo "1. 操作系统信息:"
echo "   $(cat /etc/os-release | grep PRETTY_NAME | cut -d'"' -f2)"
echo "   内核: $(uname -r)"
echo ""

# 2. 检查内存
echo "2. 内存信息:"
free -h | grep Mem | awk '{print "   总内存: "$2" | 已用: "$3" | 可用: "$7}'
echo ""

# 3. 检查磁盘
echo "3. 磁盘空间:"
df -h / | tail -n 1 | awk '{print "   总空间: "$2" | 已用: "$3" | 可用: "$4" | 使用率: "$5}'
echo ""

# 4. 检查Docker
echo "4. 检查Docker:"
check_command docker "--version"
if [ $? -eq 0 ]; then
    sudo systemctl is-active docker &> /dev/null
    if [ $? -eq 0 ]; then
        echo -e "   ${GREEN}✓${NC} Docker服务正在运行"
    else
        echo -e "   ${RED}✗${NC} Docker服务未运行"
        echo "   启动: sudo systemctl start docker"
    fi
fi
echo ""

# 5. 检查Docker Compose
echo "5. 检查Docker Compose:"
check_command docker-compose "--version"
echo ""

# 6. 检查Git
echo "6. 检查Git:"
check_command git "--version"
echo ""

# 7. 检查端口占用
echo "7. 检查端口占用:"
for port in 3000 3306; do
    if netstat -tuln 2>/dev/null | grep -q ":$port "; then
        echo -e "   ${YELLOW}⚠${NC} 端口 $port 已被占用"
    else
        echo -e "   ${GREEN}✓${NC} 端口 $port 可用"
    fi
done
echo ""

# 8. 检查防火墙
echo "8. 检查防火墙:"
if command -v ufw &> /dev/null; then
    sudo ufw status | grep "Status" | awk '{print "   UFW状态: "$2}'
elif command -v firewall-cmd &> /dev/null; then
    sudo firewall-cmd --state 2>/dev/null | awk '{print "   Firewalld状态: "$1}'
else
    echo "   未检测到防火墙"
fi
echo ""

# 9. 检查项目目录
echo "9. 检查项目目录:"
if [ -f "docker-compose.yml" ]; then
    echo -e "   ${GREEN}✓${NC} docker-compose.yml 存在"
else
    echo -e "   ${RED}✗${NC} docker-compose.yml 不存在"
fi

if [ -f ".env" ]; then
    echo -e "   ${GREEN}✓${NC} .env 文件存在"
else
    echo -e "   ${YELLOW}⚠${NC} .env 文件不存在，请执行: cp .env.example .env"
fi

if [ -f "docs/init.sql" ]; then
    echo -e "   ${GREEN}✓${NC} init.sql 存在"
else
    echo -e "   ${RED}✗${NC} init.sql 不存在"
fi
echo ""

# 10. 检查目录权限
echo "10. 检查目录权限:"
for dir in data logs backups; do
    if [ -d "$dir" ]; then
        echo -e "   ${GREEN}✓${NC} $dir/ 目录存在"
    else
        echo -e "   ${YELLOW}⚠${NC} $dir/ 目录不存在，将自动创建"
    fi
done
echo ""

# 总结
echo "======================================"
echo "检查完成！"
echo "======================================"
echo ""

# 提供建议
echo "下一步操作:"
echo "1. 如果Docker未安装，请执行:"
echo "   curl -fsSL https://get.docker.com | bash"
echo ""
echo "2. 如果.env文件不存在，请执行:"
echo "   cp .env.example .env"
echo "   vim .env  # 修改密码和API密钥"
echo ""
echo "3. 创建必要的目录:"
echo "   mkdir -p data/mysql data/uploads logs/mysql logs/backend logs/scheduler backups/mysql"
echo ""
echo "4. 赋予脚本执行权限:"
echo "   chmod +x deploy.sh backup.sh restore.sh"
echo ""
echo "5. 开始部署:"
echo "   ./deploy.sh"
echo ""
