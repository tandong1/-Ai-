# 快速开始指南

## ✅ 项目结构调整完成

```
WeChatProjects/
├── miniprogram-1/          # 微信小程序前端
├── study-backend/          # Spring Boot后端
├── docs/                   # 文档
├── config/                 # 配置文件
├── docker-compose.yml      # Docker编排
├── deploy.sh              # 部署脚本
├── backup.sh              # 备份脚本
├── restore.sh             # 恢复脚本
└── README.md              # 主文档
```

## 🚀 一分钟快速部署

### 1. 配置环境变量

```bash
cd /Users/wangmian/WeChatProjects

# 复制环境变量模板
cp .env.example .env

# 编辑配置（必须修改密码和API密钥）
vim .env
```

必须配置的内容：
```bash
MYSQL_ROOT_PASSWORD=your_strong_root_password_here
MYSQL_PASSWORD=your_strong_user_password_here
ANTHROPIC_API_KEY=sk-ant-your-api-key-here
JWT_SECRET=your_jwt_secret_at_least_32_characters_long
```

### 2. 一键部署

```bash
# 赋予脚本执行权限
chmod +x deploy.sh backup.sh restore.sh

# 执行部署
./deploy.sh
```

### 3. 验证部署

```bash
# 查看容器状态
docker-compose ps

# 测试健康检查
curl http://localhost:3000/api/health

# 测试用户列表
curl http://localhost:3000/api/users
```

## 📱 前端开发

### 微信小程序

1. 打开微信开发者工具
2. 导入项目：选择 `miniprogram-1` 目录
3. 配置 AppID（测试可以使用测试号）
4. 修改API地址（开发环境）

```javascript
// miniprogram-1/app.js
App({
  globalData: {
    apiUrl: 'http://localhost:3000/api'  // 开发环境
    // apiUrl: 'https://your-domain.com/api'  // 生产环境
  }
})
```

5. 编译并预览

## 🔧 后端开发

### 本地运行（不使用Docker）

```bash
# 1. 确保MySQL已运行
mysql -u root -p

# 2. 创建数据库和用户
CREATE DATABASE study_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'study_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON study_system.* TO 'study_user'@'localhost';
FLUSH PRIVILEGES;

# 3. 导入数据库
mysql -u study_user -p study_system < docs/init.sql

# 4. 配置环境变量
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=study_system
export DB_USER=study_user
export DB_PASSWORD=your_password
export JWT_SECRET=your_jwt_secret_at_least_32_characters

# 5. 运行后端
cd study-backend
mvn spring-boot:run
```

### 使用Docker运行

```bash
# 在项目根目录
docker-compose up -d
```

## 📊 API测试

### 1. 获取用户列表（无需认证）

```bash
curl http://localhost:3000/api/users
```

响应示例：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "大宝",
      "avatar": "/images/avatar1.png",
      "totalPoints": 0,
      "currentLevel": "初学者"
    }
  ],
  "timestamp": 1705305600000
}
```

### 2. 用户登录

```bash
curl -X POST http://localhost:3000/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"userId": 1}'
```

响应示例：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "name": "大宝",
      "avatar": "/images/avatar1.png",
      "totalPoints": 0,
      "currentLevel": "初学者"
    }
  },
  "timestamp": 1705305600000
}
```

### 3. 获取今日题目（需要认证）

```bash
TOKEN="your_token_here"

curl -X GET "http://localhost:3000/api/questions/daily?subject=math" \
  -H "Authorization: Bearer $TOKEN"
```

### 4. 提交答案（需要认证）

```bash
curl -X POST http://localhost:3000/api/questions/submit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "subject": "math",
    "answers": [
      {
        "questionId": 1,
        "userAnswer": "5/6"
      }
    ]
  }'
```

## 🗄️ 数据库管理

### 进入MySQL容器

```bash
# 方式1
docker exec -it study_mysql mysql -u study_user -p

# 方式2
docker-compose exec mysql mysql -u study_user -p
```

### 查看数据

```sql
USE study_system;
SHOW TABLES;
SELECT * FROM users;
SELECT * FROM questions LIMIT 5;
```

### 备份数据库

```bash
# 手动备份
./backup.sh

# 设置定时备份
crontab -e
# 添加：每天凌晨2点备份
0 2 * * * cd /Users/wangmian/WeChatProjects && ./backup.sh >> logs/backup.log 2>&1
```

### 恢复数据库

```bash
./restore.sh backups/mysql/backup_20240115_020000.sql.gz
```

## 📝 常用命令

### Docker相关

```bash
# 启动所有服务
docker-compose up -d

# 停止所有服务
docker-compose down

# 重启服务
docker-compose restart

# 查看日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f mysql

# 重新构建并启动
docker-compose up -d --build

# 查看容器状态
docker-compose ps

# 查看资源使用
docker stats study_mysql study_backend
```

### 后端相关

```bash
cd study-backend

# 编译
mvn clean compile

# 打包
mvn clean package

# 运行测试
mvn test

# 本地运行
mvn spring-boot:run

# 运行jar
java -jar target/study-system-1.0.0.jar
```

## ❓ 常见问题

### 1. 端口冲突

**问题**: 3000或3306端口已被占用

**解决**:
```bash
# 查看端口占用
netstat -an | grep 3000
netstat -an | grep 3306

# 修改docker-compose.yml的端口映射
# 例如: "3001:3000"
```

### 2. MySQL启动失败

**问题**: MySQL容器启动失败

**解决**:
```bash
# 查看日志
docker-compose logs mysql

# 清理数据目录重新初始化
sudo rm -rf data/mysql
docker-compose up -d mysql
```

### 3. 后端无法连接数据库

**问题**: 后端报错 "Connection refused"

**解决**:
1. 确保MySQL健康检查通过
   ```bash
   docker-compose ps mysql
   ```
2. 检查环境变量配置
3. 查看后端日志
   ```bash
   docker-compose logs backend
   ```

### 4. JWT Token验证失败

**问题**: 返回401未授权

**解决**:
1. 确保JWT_SECRET配置正确且至少32位
2. 确保请求头携带了token
   ```
   Authorization: Bearer <token>
   ```
3. Token可能已过期，重新登录获取新token

## 📚 文档索引

- **README.md** - 项目主文档
- **PROJECT_STRUCTURE.md** - 项目结构说明
- **docs/SPEC.md** - 技术规格文档（详细设计）
- **docs/API.md** - API接口文档（完整接口说明）
- **docs/DOCKER_QUICKSTART.md** - Docker快速开始
- **docs/DOCKER_COMMANDS.md** - Docker命令参考
- **study-backend/README.md** - 后端说明文档
- **study-backend/IMPLEMENTATION.md** - 后端实现说明
- **study-backend/CODE_REVIEW.md** - 代码检查清单

## 🎯 下一步

1. **测试基础功能**
   - 用户登录
   - 获取题目
   - 提交答案

2. **继续开发**
   - AI题目生成服务
   - 定时任务
   - 知识库管理
   - 礼物商城

3. **前端开发**
   - 连接后端API
   - 完善UI界面
   - 测试完整流程

## ✅ 检查清单

部署前确认：
- [ ] 配置了.env文件
- [ ] 修改了所有默认密码
- [ ] 配置了ANTHROPIC_API_KEY
- [ ] 配置了JWT_SECRET（至少32位）
- [ ] Docker和Docker Compose已安装

运行后检查：
- [ ] docker-compose ps显示所有容器都是Up状态
- [ ] curl http://localhost:3000/api/health 返回正常
- [ ] curl http://localhost:3000/api/users 返回用户列表
- [ ] 可以成功登录并获取token

---

**所有配置已完成，可以开始开发了！** 🎉
