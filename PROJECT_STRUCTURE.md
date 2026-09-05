# 项目结构说明

## 调整后的目录结构

```
WeChatProjects/                        # 项目根目录
│
├── miniprogram-1/                     # 微信小程序（前端）
│   ├── pages/                         # 页面
│   │   ├── index/                     # 首页
│   │   ├── challenge/                 # 挑战页面
│   │   ├── history/                   # 历史记录
│   │   ├── points/                    # 我的积分
│   │   ├── shop/                      # 积分商城
│   │   └── user-select/               # 用户选择
│   ├── components/                    # 组件
│   ├── utils/                         # 工具类
│   ├── images/                        # 图片资源
│   ├── app.js                         # 小程序入口
│   ├── app.json                       # 小程序配置
│   └── app.wxss                       # 全局样式
│
├── study-backend/                     # Spring Boot后端
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/study/
│   │   │   │   ├── StudyApplication.java  # 启动类
│   │   │   │   ├── controller/        # 控制层（API接口）
│   │   │   │   │   ├── UserController.java
│   │   │   │   │   ├── QuestionController.java
│   │   │   │   │   └── HealthController.java
│   │   │   │   ├── service/           # 业务层（业务逻辑）
│   │   │   │   │   ├── UserService.java
│   │   │   │   │   ├── QuestionService.java
│   │   │   │   │   └── impl/          # 实现类
│   │   │   │   │       ├── UserServiceImpl.java
│   │   │   │   │       └── QuestionServiceImpl.java
│   │   │   │   ├── mapper/            # 持久层（数据访问）
│   │   │   │   │   ├── UserMapper.java
│   │   │   │   │   ├── QuestionMapper.java
│   │   │   │   │   ├── QuestionRecordMapper.java
│   │   │   │   │   ├── PointsRecordMapper.java
│   │   │   │   │   └── WrongQuestionMapper.java
│   │   │   │   ├── entity/            # 实体类（数据库映射）
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── Question.java
│   │   │   │   │   ├── QuestionRecord.java
│   │   │   │   │   ├── PointsRecord.java
│   │   │   │   │   └── WrongQuestion.java
│   │   │   │   ├── dto/               # 数据传输对象（请求参数）
│   │   │   │   │   ├── UserLoginDTO.java
│   │   │   │   │   └── SubmitAnswerDTO.java
│   │   │   │   ├── vo/                # 视图对象（响应数据）
│   │   │   │   │   ├── UserVO.java
│   │   │   │   │   ├── LoginVO.java
│   │   │   │   │   ├── QuestionVO.java
│   │   │   │   │   ├── DailyQuestionsVO.java
│   │   │   │   │   ├── AnswerResultVO.java
│   │   │   │   │   └── SubmitResultVO.java
│   │   │   │   ├── config/            # 配置类
│   │   │   │   │   ├── MyBatisPlusConfig.java
│   │   │   │   │   ├── WebConfig.java
│   │   │   │   │   └── JwtInterceptor.java
│   │   │   │   ├── common/            # 通用组件
│   │   │   │   │   ├── Result.java
│   │   │   │   │   ├── BusinessException.java
│   │   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │   └── util/              # 工具类
│   │   │   │       └── JwtUtil.java
│   │   │   └── resources/
│   │   │       └── application.yml    # 应用配置
│   │   └── test/                      # 测试代码
│   │       └── java/com/study/
│   │           └── StudyApplicationTests.java
│   ├── pom.xml                        # Maven配置
│   ├── Dockerfile                     # Docker镜像构建
│   ├── .gitignore                     # Git忽略文件
│   ├── README.md                      # 后端说明文档
│   ├── IMPLEMENTATION.md              # 实现说明文档
│   └── CODE_REVIEW.md                 # 代码检查清单
│
├── docs/                              # 文档目录
│   ├── SPEC.md                        # 技术规格文档
│   ├── API.md                         # API接口文档
│   ├── init.sql                       # 数据库初始化脚本
│   ├── DOCKER_QUICKSTART.md           # Docker快速开始
│   └── DOCKER_COMMANDS.md             # Docker命令参考
│
├── config/                            # 配置文件
│   └── mysql/
│       └── my.cnf                     # MySQL配置文件
│
├── data/                              # 数据持久化目录（.gitignore）
│   ├── mysql/                         # MySQL数据文件
│   └── uploads/                       # 用户上传的文件
│
├── logs/                              # 日志目录（.gitignore）
│   ├── mysql/                         # MySQL日志
│   ├── backend/                       # 后端日志
│   └── scheduler/                     # 定时任务日志
│
├── backups/                           # 备份目录（.gitignore）
│   └── mysql/                         # 数据库备份
│
├── docker-compose.yml                 # Docker编排配置
├── .env.example                       # 环境变量模板
├── .gitignore                         # Git忽略文件
├── README.md                          # 项目主说明文档
├── deploy.sh                          # 部署脚本
├── backup.sh                          # 备份脚本
└── restore.sh                         # 恢复脚本
```

## 目录说明

### 前端目录 (miniprogram-1/)
- **pages/** - 小程序页面，每个页面包含 .js .json .wxml .wxss 四个文件
- **components/** - 可复用的组件
- **utils/** - 工具函数（如网络请求、数据处理等）
- **images/** - 静态图片资源

### 后端目录 (study-backend/)

#### MVC三层架构
1. **Controller层** - 接收HTTP请求，参数校验，返回响应
2. **Service层** - 处理业务逻辑，调用Mapper
3. **Mapper层** - 数据库操作，执行SQL

#### 数据流转
```
DTO (请求) → Controller → Service → Mapper → Database
                              ↓
Entity (数据库) ← Mapper ← Service → VO (响应) → Controller
```

### 配置目录 (config/)
- MySQL配置、Nginx配置等外部服务的配置文件

### 文档目录 (docs/)
- 技术文档、API文档、数据库脚本等

### 数据目录 (data/)
- Docker容器的数据持久化，不纳入版本控制

### 日志目录 (logs/)
- 应用运行日志，不纳入版本控制

## 部署方式

### Docker Compose部署（推荐）
```bash
# 在项目根目录执行
./deploy.sh
```

### 本地开发
```bash
# 后端
cd study-backend
mvn spring-boot:run

# 前端
# 使用微信开发者工具打开 miniprogram-1 目录
```

## 环境变量

所有敏感配置通过环境变量管理：
- `.env` - 实际配置（不纳入版本控制）
- `.env.example` - 配置模板（纳入版本控制）

## 版本控制

### 纳入Git
- 源代码
- 配置模板
- 文档
- 构建脚本

### 不纳入Git
- 敏感配置 (.env)
- 数据文件 (data/)
- 日志文件 (logs/)
- 备份文件 (backups/)
- IDE配置 (.idea/, .vscode/)

## 项目特点

1. **前后端分离** - 小程序前端 + Spring Boot后端
2. **容器化部署** - Docker + Docker Compose
3. **标准MVC架构** - Controller → Service → Mapper
4. **统一响应格式** - Result<T> 统一包装
5. **JWT认证** - 无状态Token认证
6. **数据持久化** - Docker Volume挂载宿主机
7. **自动备份** - 数据库定时备份脚本
8. **完整文档** - API文档、部署文档、代码检查清单

## 下一步开发

参考 README.md 中的项目进度表，优先开发：
1. AI题目生成服务
2. 定时任务（每日生成题目）
3. 知识库管理
4. 礼物商城

---

**目录结构已调整完成，前端和后端同级，便于独立开发和部署！**
