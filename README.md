# 小小学霸学习系统

一个为小学生设计的智能学习系统，支持每日AI生成个性化题目、自动批改、积分激励和礼物兑换。

## 项目结构

```
WeChatProjects/
├── miniprogram-1/              # 微信小程序前端
│   ├── pages/                  # 页面
│   ├── components/             # 组件
│   ├── utils/                  # 工具类
│   ├── images/                 # 图片资源
│   └── app.js                  # 小程序入口
│
├── study-backend/              # Spring Boot后端
│   ├── src/
│   │   └── main/
│   │       ├── java/com/study/
│   │       │   ├── controller/ # 控制层
│   │       │   ├── service/    # 业务层
│   │       │   ├── mapper/     # 持久层
│   │       │   ├── entity/     # 实体类
│   │       │   ├── dto/        # 数据传输对象
│   │       │   ├── vo/         # 视图对象
│   │       │   ├── config/     # 配置类
│   │       │   ├── common/     # 通用组件
│   │       │   └── util/       # 工具类
│   │       └── resources/
│   │           └── application.yml
│   ├── pom.xml                 # Maven配置
│   └── Dockerfile              # Docker构建文件
│
├── docs/                       # 文档
│   ├── SPEC.md                 # 技术规格文档
│   ├── API.md                  # API接口文档
│   ├── init.sql                # 数据库初始化脚本
│   ├── DOCKER_QUICKSTART.md    # Docker快速开始
│   └── DOCKER_COMMANDS.md      # Docker命令参考
│
├── config/                     # 配置文件
│   └── mysql/
│       └── my.cnf              # MySQL配置
│
├── data/                       # 数据持久化（.gitignore）
│   ├── mysql/                  # MySQL数据目录
│   └── uploads/                # 上传文件目录
│
├── logs/                       # 日志目录（.gitignore）
│   ├── mysql/
│   ├── backend/
│   └── scheduler/
│
├── docker-compose.yml          # Docker编排配置
├── .env.example                # 环境变量模板
├── deploy.sh                   # 部署脚本
├── backup.sh                   # 备份脚本
└── restore.sh                  # 恢复脚本
```

## 技术栈

### 前端
- **框架**: 微信小程序
- **UI**: 原生组件

### 后端
- **框架**: Spring Boot 3.2.1
- **Java**: 17
- **数据库**: MySQL 8.0
- **ORM**: MyBatis Plus 3.5.5
- **认证**: JWT
- **AI**: Anthropic Claude API

### 部署
- **容器化**: Docker + Docker Compose
- **反向代理**: Nginx（可选）

## 快速开始

### 1. 环境准备

#### 必需软件
- Docker 20.10+
- Docker Compose 2.0+

#### 可选软件（本地开发）
- Java 17+
- Maven 3.6+
- MySQL 8.0+
- 微信开发者工具

### 2. 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑环境变量
vim .env

# 修改以下配置
MYSQL_ROOT_PASSWORD=your_strong_root_password
MYSQL_PASSWORD=your_strong_user_password
ANTHROPIC_API_KEY=sk-ant-your-api-key
JWT_SECRET=your_jwt_secret_at_least_32_characters_long
```

### 3. 部署

#### 方式一：使用部署脚本（推荐）

```bash
# 赋予执行权限
chmod +x deploy.sh backup.sh restore.sh

# 一键部署
./deploy.sh
```

#### 方式二：手动部署

```bash
# 创建必要的目录
mkdir -p data/mysql data/uploads logs/mysql logs/backend logs/scheduler

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f
```

### 4. 验证部署

```bash
# 检查容器状态
docker-compose ps

# 健康检查
curl http://localhost:3000/api/health

# 测试用户接口
curl http://localhost:3000/api/users
```

### 5. 访问服务

- **后端API**: http://localhost:3000/api
- **MySQL**: localhost:3306
  - 用户名: study_user
  - 密码: 见.env文件

## 开发指南

### 后端开发

```bash
# 进入后端目录
cd study-backend

# 本地运行（需要配置环境变量）
mvn spring-boot:run

# 编译打包
mvn clean package

# 运行测试
mvn test
```

### 前端开发

```bash
# 使用微信开发者工具打开 miniprogram-1 目录
# 配置后端API地址（开发环境）
```

## API文档

详见 `docs/API.md`

主要接口：

### 用户模块
- `GET /api/users` - 获取用户列表
- `POST /api/users/login` - 用户登录

### 题目模块
- `GET /api/questions/daily?subject=math` - 获取今日题目
- `POST /api/questions/submit` - 提交答案

### 健康检查
- `GET /api/health` - 健康检查

## 数据库管理

### 备份

```bash
# 手动备份
./backup.sh

# 定时备份（每天凌晨2点）
crontab -e
# 添加：
0 2 * * * cd /path/to/project && ./backup.sh >> logs/backup.log 2>&1
```

### 恢复

```bash
./restore.sh backups/mysql/backup_20240115_020000.sql.gz
```

### 直接访问数据库

```bash
# 进入MySQL容器
docker exec -it study_mysql mysql -u study_user -p

# 或使用docker-compose
docker-compose exec mysql mysql -u study_user -p
```

## 常见问题

### 1. 端口被占用
修改 `docker-compose.yml` 中的端口映射：
```yaml
ports:
  - "3001:3000"  # 将宿主机端口改为3001
```

### 2. MySQL启动失败
```bash
# 检查日志
docker-compose logs mysql

# 清理数据目录重新初始化
sudo rm -rf data/mysql
docker-compose up -d
```

### 3. 后端无法连接数据库
确保环境变量配置正确，MySQL健康检查通过后再启动后端。

## 维护

### 查看日志
```bash
# 所有服务
docker-compose logs -f

# 单个服务
docker-compose logs -f backend
docker-compose logs -f mysql
```

### 重启服务
```bash
# 重启所有服务
docker-compose restart

# 重启单个服务
docker-compose restart backend
```

### 更新代码
```bash
# 拉取最新代码
git pull

# 重新构建并启动
docker-compose up -d --build
```

## 项目进度

### ✅ 已完成
- [x] 数据库设计和初始化
- [x] Spring Boot后端基础架构
- [x] 用户模块（登录、列表）
- [x] 题目模块（获取、提交、批改）
- [x] 错题本自动记录
- [x] 积分系统
- [x] JWT认证
- [x] Docker部署配置
- [x] 微信小程序前端UI

### 🚧 进行中
- [ ] AI题目生成服务
- [ ] 定时任务（每日生成题目）
- [ ] 知识库上传和管理
- [ ] 礼物商城
- [ ] 兑换记录

### 📋 待开发
- [ ] 成就系统
- [ ] 学习报告
- [ ] 家长端功能
- [ ] 数据分析和可视化

## 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 许可证

暂无

## 联系方式

如有问题，请提交 Issue 或联系项目维护者。

---

**祝你的外甥学习进步！** 🎓
