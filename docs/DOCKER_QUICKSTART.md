# Docker 快速开始指南

## 一、准备工作

### 1. 安装Docker和Docker Compose

#### Ubuntu/Debian
```bash
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
```

#### CentOS
```bash
# 安装Docker
sudo yum install -y yum-utils
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install docker-ce docker-ce-cli containerd.io

# 启动Docker
sudo systemctl start docker
sudo systemctl enable docker

# 安装Docker Compose（同上）
```

### 2. 添加当前用户到docker组（可选，避免每次sudo）
```bash
sudo usermod -aG docker $USER
# 重新登录生效
```

## 二、项目部署

### 1. 上传项目文件到服务器
```bash
# 方式1：使用git
cd /path/to/your/projects
git clone your-repo-url
cd miniprogram-1

# 方式2：使用scp上传
scp -r miniprogram-1/ user@your-server:/path/to/projects/
```

### 2. 配置环境变量
```bash
# 复制环境变量模板
cp .env.example .env

# 编辑环境变量
vim .env

# 修改以下内容：
# MYSQL_ROOT_PASSWORD=你的强密码
# MYSQL_PASSWORD=你的强密码
# ANTHROPIC_API_KEY=你的API密钥
# JWT_SECRET=至少32位的随机字符串
```

### 3. 确保init.sql在正确位置
```bash
ls docs/init.sql
# 应该能看到这个文件
```

### 4. 赋予脚本执行权限
```bash
chmod +x deploy.sh backup.sh restore.sh
```

### 5. 执行部署
```bash
./deploy.sh
```

### 6. 验证部署
```bash
# 查看容器状态（应该都是Up状态）
docker-compose ps

# 查看日志
docker-compose logs -f

# 测试API（假设有健康检查接口）
curl http://localhost:3000/health
```

## 三、目录结构说明

部署后的目录结构：
```
your-project/
├── data/                      # 持久化数据（重要！）
│   ├── mysql/                 # MySQL数据文件
│   └── uploads/               # 用户上传的文件
├── logs/                      # 日志文件
│   ├── mysql/
│   │   ├── error.log
│   │   ├── slow.log
│   │   └── mysql-bin.*
│   ├── backend/
│   └── scheduler/
├── backups/                   # 备份文件（自动创建）
│   └── mysql/
├── docker-compose.yml         # Docker编排配置
├── .env                       # 环境变量（敏感信息）
└── ...
```

## 四、数据持久化说明

### 重要：这些目录的数据会保留！
- ✅ `./data/mysql/` - MySQL数据库文件
- ✅ `./data/uploads/` - 上传的图片等文件
- ✅ `./logs/` - 日志文件

### 即使删除容器，数据也不会丢失
```bash
# 删除容器（数据还在）
docker-compose down

# 重新启动（数据恢复）
docker-compose up -d
```

### 完全清理（包括数据）
```bash
# 警告：这会删除所有数据！
docker-compose down -v
rm -rf data/
```

## 五、常见问题

### Q1: 端口被占用
```bash
# 查看哪个程序占用了端口
netstat -tulpn | grep :3306
netstat -tulpn | grep :3000

# 修改docker-compose.yml中的端口
# 例如：将 "3306:3306" 改为 "3307:3306"
```

### Q2: MySQL启动失败
```bash
# 查看日志
docker-compose logs mysql

# 常见原因：
# 1. data/mysql目录权限问题
sudo chown -R 999:999 data/mysql/

# 2. data/mysql目录已有旧数据且版本不兼容
# 解决：备份后删除重建
mv data/mysql data/mysql.bak
docker-compose up -d
```

### Q3: 忘记MySQL密码
```bash
# 查看.env文件中的密码
cat .env | grep MYSQL_PASSWORD
```

### Q4: 如何进入MySQL查看数据
```bash
# 方式1：使用docker exec
docker exec -it study_mysql mysql -u study_user -p

# 方式2：使用docker-compose
docker-compose exec mysql mysql -u study_user -p

# 输入密码（在.env中的MYSQL_PASSWORD）
# 然后执行SQL：
USE study_system;
SHOW TABLES;
SELECT * FROM users;
```

### Q5: 容器无法访问外网
```bash
# 检查Docker网络
docker network ls
docker network inspect study_network

# 重启Docker服务
sudo systemctl restart docker
docker-compose up -d
```

## 六、日常维护

### 每日检查
```bash
# 查看容器状态
docker-compose ps

# 查看磁盘使用
df -h
du -sh data/
```

### 每周备份
```bash
# 执行备份脚本
./backup.sh

# 或设置自动备份
crontab -e
# 添加：每周日凌晨2点备份
0 2 * * 0 cd /path/to/project && ./backup.sh >> logs/backup.log 2>&1
```

### 每月维护
```bash
# 优化数据库
docker exec study_mysql mysqlcheck -u study_user -p${MYSQL_PASSWORD} --optimize study_system

# 清理旧日志
find logs/ -name "*.log" -mtime +30 -delete

# 清理旧备份
find backups/ -name "*.sql.gz" -mtime +90 -delete

# 更新Docker镜像
docker-compose pull
docker-compose up -d
```

## 七、生产环境建议

### 安全配置
1. **不要暴露MySQL端口**
   - 注释掉 `ports: - "3306:3306"`
   - MySQL只允许内部容器访问

2. **使用强密码**
   - 至少20位随机字符
   - 包含大小写字母、数字、特殊字符

3. **定期备份**
   - 本地备份 + 远程备份
   - 测试恢复流程

4. **监控日志**
   - 定期查看error.log
   - 设置日志告警

### 性能优化
1. **调整MySQL内存**（根据服务器内存）
   - 编辑 `config/mysql/my.cnf`
   - `innodb_buffer_pool_size` 设为可用内存的50-70%

2. **限制容器资源**（防止单个容器占满资源）
   ```yaml
   # 在docker-compose.yml中添加：
   deploy:
     resources:
       limits:
         cpus: '1'
         memory: 1G
   ```

### 监控方案（可选）
```bash
# 安装Portainer（Docker可视化管理）
docker volume create portainer_data
docker run -d -p 9000:9000 \
  --name portainer --restart always \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v portainer_data:/data \
  portainer/portainer-ce

# 访问: http://your-server:9000
```

## 八、快速命令速查

```bash
# 启动
docker-compose up -d

# 停止
docker-compose down

# 重启
docker-compose restart

# 查看日志
docker-compose logs -f

# 备份
./backup.sh

# 恢复
./restore.sh backups/mysql/backup_xxx.sql.gz

# 进入MySQL
docker exec -it study_mysql mysql -u study_user -p

# 查看容器状态
docker-compose ps

# 查看资源使用
docker stats
```

## 九、紧急情况处理

### 数据库损坏
```bash
# 1. 立即停止服务
docker-compose stop

# 2. 备份当前数据（即使损坏也要备份）
cp -r data/mysql data/mysql.emergency.backup

# 3. 尝试修复
docker-compose start mysql
docker exec study_mysql mysqlcheck -u root -p${MYSQL_ROOT_PASSWORD} --auto-repair --all-databases

# 4. 如果修复失败，从备份恢复
./restore.sh backups/mysql/backup_latest.sql.gz
```

### 磁盘空间不足
```bash
# 1. 清理Docker资源
docker system prune -a

# 2. 清理旧日志
find logs/ -name "*.log" -mtime +7 -delete

# 3. 清理旧备份
find backups/ -name "*.sql.gz" -mtime +7 -delete

# 4. 压缩日志文件
gzip logs/mysql/*.log
gzip logs/backend/*.log
```

---

## 需要帮助？

查看详细文档：
- `docs/DOCKER_COMMANDS.md` - Docker命令参考
- `docs/SPEC.md` - 系统架构说明
- `docs/API.md` - API接口文档
