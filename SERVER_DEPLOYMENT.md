# 服务器部署完整指南

## 环境要求
- 操作系统：Linux (Ubuntu/CentOS/Debian)
- 内存：至少2GB（推荐4GB）
- 磁盘：至少20GB
- 端口：3000（后端API）、3306（MySQL，可选暴露）

---

## 第一步：连接到服务器

```bash
# 使用SSH连接到你的服务器
ssh root@your_server_ip

# 或使用普通用户
ssh username@your_server_ip
```

---

## 第二步：检查服务器环境

```bash
# 查看操作系统版本
cat /etc/os-release

# 查看内存
free -h

# 查看磁盘空间
df -h

# 查看当前目录
pwd
```

---

## 第三步：安装Docker和Docker Compose

### Ubuntu/Debian系统

```bash
# 更新软件包
sudo apt update

# 安装Docker
curl -fsSL https://get.docker.com | bash

# 启动Docker服务
sudo systemctl start docker
sudo systemctl enable docker

# 安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker --version
docker-compose --version

# 添加当前用户到docker组（可选，避免每次sudo）
sudo usermod -aG docker $USER
# 需要重新登录生效
```

### CentOS系统

```bash
# 安装Docker
sudo yum install -y yum-utils
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install docker-ce docker-ce-cli containerd.io

# 启动Docker
sudo systemctl start docker
sudo systemctl enable docker

# 安装Docker Compose（同上）
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 验证
docker --version
docker-compose --version
```

---

## 第四步：下载代码（你已完成）

```bash
# 克隆仓库到服务器
cd /home/your_username
git clone git@github.com:tandong1/-Ai-.git study-system

# 或使用HTTPS
git clone https://github.com/tandong1/-Ai-.git study-system

# 进入项目目录
cd study-system
ls -la
```

---

## 第五步：配置环境变量 ⭐ 重要

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑环境变量
vim .env
# 或使用nano
nano .env
```

**必须修改以下内容：**

```bash
# MySQL配置
MYSQL_ROOT_PASSWORD=your_strong_root_password_HERE    # ⚠️ 必须修改
MYSQL_PASSWORD=your_strong_user_password_HERE          # ⚠️ 必须修改

# 大模型API（如果有）
ANTHROPIC_API_KEY=sk-ant-your-api-key-here            # ⚠️ 必须配置

# JWT配置
JWT_SECRET=your_jwt_secret_at_least_32_characters_long  # ⚠️ 必须修改，至少32位

# OSS配置（可选，如果使用阿里云OSS）
OSS_ACCESS_KEY_ID=your_oss_access_key
OSS_ACCESS_KEY_SECRET=your_oss_access_secret
OSS_BUCKET=your-bucket-name
```

**生成强密码示例：**
```bash
# 生成随机密码
openssl rand -base64 32
```

**保存文件：**
- vim: 按 `ESC`，输入 `:wq`，按 `Enter`
- nano: 按 `Ctrl+X`，按 `Y`，按 `Enter`

---

## 第六步：创建必要的目录

```bash
# 创建数据和日志目录
mkdir -p data/mysql
mkdir -p data/uploads
mkdir -p logs/mysql
mkdir -p logs/backend
mkdir -p logs/scheduler
mkdir -p backups/mysql

# 设置权限
chmod -R 755 data
chmod -R 755 logs
chmod -R 755 backups

# 给脚本添加执行权限
chmod +x deploy.sh backup.sh restore.sh
```

---

## 第七步：部署服务 🚀

### 方式一：使用部署脚本（推荐）

```bash
# 执行一键部署
./deploy.sh
```

脚本会自动：
1. 检查Docker和Docker Compose
2. 创建必要的目录
3. 检查环境变量
4. 构建镜像
5. 启动服务

### 方式二：手动部署

```bash
# 构建并启动所有服务
docker-compose up -d --build

# 查看启动日志
docker-compose logs -f
```

---

## 第八步：验证部署

### 1. 查看容器状态

```bash
docker-compose ps
```

**预期输出：**
```
NAME              IMAGE                    STATUS
study_backend     study-system-backend     Up (healthy)
study_mysql       mysql:8.0                Up (healthy)
study_scheduler   study-system-scheduler   Up
```

所有容器状态应该是 `Up`，backend和mysql应该有 `(healthy)` 标记。

### 2. 查看日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看MySQL日志
docker-compose logs -f mysql

# 查看后端日志
docker-compose logs -f backend

# 按Ctrl+C退出日志查看
```

### 3. 测试API接口

```bash
# 健康检查
curl http://localhost:3000/api/health

# 预期返回：
# {"code":200,"message":"success","data":{"status":"UP","timestamp":...}}

# 测试用户列表
curl http://localhost:3000/api/users

# 预期返回用户列表
```

### 4. 测试数据库连接

```bash
# 进入MySQL容器
docker exec -it study_mysql mysql -u study_user -p

# 输入密码（.env中的MYSQL_PASSWORD）

# 在MySQL中执行
USE study_system;
SHOW TABLES;
SELECT * FROM users;
EXIT;
```

---

## 第九步：配置防火墙

### Ubuntu/Debian (UFW)

```bash
# 允许SSH（重要！）
sudo ufw allow 22/tcp

# 允许HTTP（如果需要）
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# 允许后端API端口（或通过Nginx反向代理）
sudo ufw allow 3000/tcp

# 不要开放MySQL端口（安全）
# sudo ufw allow 3306/tcp  # ❌ 不建议

# 启用防火墙
sudo ufw enable

# 查看状态
sudo ufw status
```

### CentOS (firewalld)

```bash
# 允许端口
sudo firewall-cmd --permanent --add-port=22/tcp
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=443/tcp
sudo firewall-cmd --permanent --add-port=3000/tcp

# 重载防火墙
sudo firewall-cmd --reload

# 查看状态
sudo firewall-cmd --list-all
```

---

## 第十步：设置自动备份

```bash
# 编辑定时任务
crontab -e

# 添加以下内容（每天凌晨2点备份）
0 2 * * * cd /path/to/study-system && ./backup.sh >> logs/backup.log 2>&1

# 保存退出
```

---

## 常见问题排查

### 问题1：容器无法启动

```bash
# 查看详细日志
docker-compose logs mysql
docker-compose logs backend

# 检查端口占用
netstat -tulpn | grep 3000
netstat -tulpn | grep 3306

# 重新启动
docker-compose down
docker-compose up -d
```

### 问题2：MySQL启动失败

```bash
# 清理MySQL数据目录
sudo rm -rf data/mysql/*
docker-compose up -d mysql

# 等待30秒后查看日志
docker-compose logs mysql
```

### 问题3：后端无法连接数据库

```bash
# 检查MySQL是否健康
docker-compose ps mysql

# 查看后端日志
docker-compose logs backend

# 检查环境变量
cat .env | grep DB_
```

### 问题4：API返回502或无响应

```bash
# 检查后端容器状态
docker-compose ps backend

# 进入后端容器检查
docker exec -it study_backend sh
curl http://localhost:3000/api/health
exit

# 重启后端服务
docker-compose restart backend
```

---

## 常用运维命令

```bash
# 查看所有容器
docker-compose ps

# 查看日志
docker-compose logs -f

# 重启服务
docker-compose restart

# 停止服务
docker-compose down

# 更新代码后重新部署
git pull
docker-compose up -d --build

# 备份数据库
./backup.sh

# 恢复数据库
./restore.sh backups/mysql/backup_xxx.sql.gz

# 查看磁盘使用
du -sh data/
du -sh logs/

# 清理Docker资源
docker system prune -a
```

---

## 生产环境建议

### 1. 使用Nginx反向代理

```bash
# 安装Nginx
sudo apt install nginx  # Ubuntu
sudo yum install nginx  # CentOS

# 配置
sudo vim /etc/nginx/sites-available/study-system
```

Nginx配置示例：
```nginx
server {
    listen 80;
    server_name your-domain.com;

    location /api {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
# 启用配置
sudo ln -s /etc/nginx/sites-available/study-system /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 2. 配置HTTPS（使用Let's Encrypt）

```bash
# 安装Certbot
sudo apt install certbot python3-certbot-nginx

# 获取SSL证书
sudo certbot --nginx -d your-domain.com

# 自动续期
sudo certbot renew --dry-run
```

### 3. 不要暴露MySQL端口

在 `docker-compose.yml` 中注释掉：
```yaml
# ports:
#   - "3306:3306"
```

### 4. 定期备份

```bash
# 每天备份
0 2 * * * cd /path/to/study-system && ./backup.sh >> logs/backup.log 2>&1

# 将备份上传到远程（可选）
0 3 * * * rsync -avz /path/to/study-system/backups/ user@backup-server:/backups/
```

---

## 完成！

访问你的API：
- **本地**: http://localhost:3000/api/health
- **服务器**: http://your_server_ip:3000/api/health
- **域名**: http://your-domain.com/api/health (配置Nginx后)

下一步：
1. 测试所有API接口
2. 配置微信小程序连接后端
3. 实现AI题目生成功能
4. 添加定时任务

**祝部署顺利！** 🎉
