# Docker 常用操作命令

## 部署相关

### 首次部署
```bash
# 1. 配置环境变量
cp .env.example .env
vim .env  # 编辑配置

# 2. 赋予脚本执行权限
chmod +x deploy.sh backup.sh restore.sh

# 3. 执行部署
./deploy.sh
```

### 日常操作
```bash
# 启动所有服务
docker-compose up -d

# 停止所有服务
docker-compose down

# 重启所有服务
docker-compose restart

# 重启单个服务
docker-compose restart backend
docker-compose restart mysql

# 查看服务状态
docker-compose ps

# 查看实时日志
docker-compose logs -f

# 查看某个服务的日志
docker-compose logs -f backend
docker-compose logs -f mysql
```

## 数据库操作

### 进入MySQL容器
```bash
# 方式1：使用docker exec
docker exec -it study_mysql mysql -u study_user -p

# 方式2：使用docker-compose
docker-compose exec mysql mysql -u study_user -p
```

### 执行SQL文件
```bash
# 从宿主机执行SQL文件
docker exec -i study_mysql mysql -u study_user -p${MYSQL_PASSWORD} study_system < your_script.sql

# 或使用docker-compose
docker-compose exec -T mysql mysql -u study_user -p${MYSQL_PASSWORD} study_system < your_script.sql
```

### 数据库备份
```bash
# 手动备份
./backup.sh

# 设置定时备份（每天凌晨2点）
crontab -e
# 添加：
0 2 * * * cd /path/to/project && ./backup.sh >> logs/backup.log 2>&1
```

### 数据库恢复
```bash
./restore.sh backups/mysql/backup_20240115_020000.sql.gz
```

### 查看数据库大小
```bash
docker exec study_mysql mysql -u study_user -p${MYSQL_PASSWORD} -e \
  "SELECT table_schema AS 'Database', 
   ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)' 
   FROM information_schema.tables 
   WHERE table_schema = 'study_system' 
   GROUP BY table_schema;"
```

## 容器管理

### 查看容器资源使用
```bash
docker stats study_mysql study_backend
```

### 查看容器详细信息
```bash
docker inspect study_mysql
docker inspect study_backend
```

### 进入容器Shell
```bash
# 进入后端容器
docker exec -it study_backend sh

# 进入MySQL容器
docker exec -it study_mysql bash
```

### 清理容器和镜像
```bash
# 删除所有停止的容器
docker container prune

# 删除未使用的镜像
docker image prune

# 删除未使用的卷
docker volume prune

# 清理所有（危险！会删除所有未使用的资源）
docker system prune -a
```

## 更新部署

### 更新代码后重新部署
```bash
# 拉取最新代码
git pull

# 重新构建并启动
docker-compose up -d --build

# 或使用部署脚本
./deploy.sh
```

### 仅更新后端服务
```bash
docker-compose up -d --build backend
```

## 数据持久化

### 数据存储位置
- MySQL数据: `./data/mysql/`
- 上传文件: `./data/uploads/`
- 日志文件: `./logs/`

### 迁移数据
```bash
# 1. 打包数据目录
tar -czf data_backup.tar.gz data/

# 2. 传输到新服务器
scp data_backup.tar.gz user@new-server:/path/to/project/

# 3. 在新服务器解压
tar -xzf data_backup.tar.gz
```

## 故障排查

### 查看容器启动失败原因
```bash
docker-compose logs mysql
docker-compose logs backend
```

### MySQL连接失败
```bash
# 检查MySQL是否启动
docker-compose ps mysql

# 检查MySQL健康状态
docker inspect study_mysql | grep Health -A 10

# 查看MySQL错误日志
docker-compose logs mysql | tail -50
```

### 端口冲突
```bash
# 查看端口占用
netstat -tulpn | grep :3306
netstat -tulpn | grep :3000

# 修改docker-compose.yml中的端口映射
# 例如: "3307:3306" 将宿主机端口改为3307
```

### 权限问题
```bash
# 修复数据目录权限
sudo chown -R 999:999 data/mysql/  # MySQL容器内用户UID是999
chmod -R 755 data/
chmod -R 755 logs/
```

## 监控和维护

### 查看磁盘使用
```bash
# 查看数据目录大小
du -sh data/
du -sh logs/

# 查看Docker占用空间
docker system df
```

### 定期维护任务
```bash
# 1. 优化数据库表
docker exec study_mysql mysqlcheck -u study_user -p${MYSQL_PASSWORD} --optimize study_system

# 2. 清理日志文件（保留最近7天）
find logs/ -name "*.log" -mtime +7 -delete

# 3. 清理旧备份（保留最近30天）
find backups/ -name "backup_*.sql.gz" -mtime +30 -delete
```

## 安全建议

1. **不要暴露MySQL端口到外网**
   - 生产环境注释掉docker-compose.yml中的`ports: - "3306:3306"`
   - 只允许内部容器通过网络连接

2. **定期更新镜像**
   ```bash
   docker-compose pull
   docker-compose up -d
   ```

3. **定期备份**
   - 设置自动备份脚本
   - 将备份存储到远程位置

4. **监控日志**
   ```bash
   # 监控错误日志
   tail -f logs/mysql/error.log
   tail -f logs/backend/*.log
   ```
