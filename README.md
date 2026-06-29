# Pulse

基于 Spring Cloud 微服务架构的新闻社交平台，采用 CQRS 读写分离设计，支持新闻发布、评论、关注、个性化信息流等功能。

## 项目特点

- **CQRS 读写分离**：写入服务（aura-write-service）与读取服务（aura-read-service）独立部署，可按需水平扩展
- **Redis 优先架构**：写入操作先写 Redis 再异步刷盘，读取操作 Redis 优先、数据库兜底，保证高性能
- **异步持久化**：通过 RabbitMQ 异步消费消息，将数据最终一致地写入 PostgreSQL
- **Feed 流预计算**：发布新闻时采用 Write Fan-out 策略，将新闻 ID 推送到所有粉丝的收件箱，读取 Feed 时直接返回
- **三级缓存**：Caffeine 本地缓存 -> Redis 分布式缓存 -> PostgreSQL 持久层
- **JWT 鉴权网关**：Gateway 统一鉴权，白名单放行注册/登录，公开接口免鉴权，下游服务信任网关注入的 UserId
- **虚拟线程**：Spring Boot 3.2+ Virtual Threads 支持，提升并发性能
- **雪花 ID 生成**：基于 Redis INCR 的分布式 ID 生成器

## 技术栈

| 分类 | 技术 |
|------|------|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.2.2 |
| 微服务 | Spring Cloud 2023.0.1 |
| 注册中心 | Spring Cloud Alibaba Nacos |
| 网关 | Spring Cloud Gateway (WebFlux) |
| 限流熔断 | Spring Cloud Alibaba Sentinel |
| 数据库 | PostgreSQL |
| ORM | MyBatis |
| 缓存 | Redis (Lettuce) |
| 消息队列 | RabbitMQ |
| 本地缓存 | Caffeine (Read Service) |
| JWT | jjwt 0.9.1 |
| JSON | Fastjson2 |
| 代码简化 | Lombok |

## 项目结构

```
pulse/
├── pom.xml                          # 根 POM
├── aura/
│   ├── pom.xml                      # 父 POM（依赖管理）
│   ├── aura-gateway/                # API 网关（端口 8080）
│   ├── aura-write-service/          # 写服务（端口 8082）
│   └── aura-read-service/           # 读服务（端口 8081）
```

## 快速启动

### 1. 环境要求

- JDK 21+
- Maven 3.8+
- Docker / Docker Compose

### 2. 启动基础设施

项目依赖 PostgreSQL、Redis、RabbitMQ、Nacos，使用 Docker Compose 一键启动：

```bash
docker compose up -d
```

`docker-compose.yml` 参考：

```yaml
version: "3.8"
services:
  postgres:
    image: postgres:16
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: root
      POSTGRES_PASSWORD: 123456
      POSTGRES_DB: aura
    volumes:
      - pgdata:/var/lib/postgresql/data

  redis:
    image: redis:7
    ports:
      - "6379:6379"
    command: redis-server --requirepass 123456

  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest

  nacos:
    image: nacos/nacos-server:v2.3.0
    ports:
      - "8848:8848"
    environment:
      MODE: standalone

volumes:
  pgdata:
```

### 3. 初始化数据库

PostgreSQL 启动后，连接并执行建表语句：

```sql
CREATE TABLE user_table (
    id          BIGINT PRIMARY KEY,
    phone       VARCHAR,
    password    VARCHAR,
    nickname    VARCHAR,
    email       VARCHAR,
    create_time TIMESTAMP
);

CREATE TABLE news_table (
    id          BIGINT PRIMARY KEY,
    title       VARCHAR,
    content     TEXT,
    author_id   BIGINT,
    like_count  INTEGER DEFAULT 0,
    create_time TIMESTAMP
);

CREATE TABLE comment_table (
    id          BIGINT PRIMARY KEY,
    news_id     BIGINT,
    user_id     BIGINT,
    content     TEXT,
    parent_id   BIGINT DEFAULT 0,
    like_count  INTEGER DEFAULT 0,
    create_time TIMESTAMP
);

CREATE TABLE user_follow (
    id          BIGINT PRIMARY KEY,
    user_id     BIGINT,
    author_id   BIGINT,
    create_time TIMESTAMP,
    UNIQUE(user_id, author_id)
);
```

### 4. 配置应用

将每个服务的 `application.yml.example` 复制为 `application.yml`，填入实际的密码和密钥：

```bash
cp aura/aura-gateway/src/main/resources/application.yml.example aura/aura-gateway/src/main/resources/application.yml
cp aura/aura-write-service/src/main/resources/application.yml.example aura/aura-write-service/src/main/resources/application.yml
cp aura/aura-read-service/src/main/resources/application.yml.example aura/aura-read-service/src/main/resources/application.yml
```

需要修改的配置项（以 write-service 为例）：

```yaml
jwt:
  secret: 你的JWT密钥              # 自定义，建议使用随机字符串

spring:
  datasource:
    password: 123456               # PostgreSQL 密码，与 docker-compose 一致
  data:
    redis:
      password: 123456             # Redis 密码，与 docker-compose 一致
  rabbitmq:
    username: guest                # RabbitMQ 账号
    password: guest                # RabbitMQ 密码
  cloud:
    nacos:
      server-addr: 127.0.0.1:8848  # Nacos 地址
```

> **注意**：gateway 和 write-service 都配置了 `jwt.secret`，请保持一致。

### 5. 编译并启动

```bash
# 编译所有模块
mvn clean package -DskipTests

# 按顺序启动（或使用 IDE 启动）
java -jar aura/aura-gateway/target/aura-gateway-1.0.0.jar
java -jar aura/aura-write-service/target/aura-write-service-1.0.0.jar
java -jar aura/aura-read-service/target/aura-read-service-1.0.0.jar
```

### 6. 启动顺序

```
Nacos → PostgreSQL / Redis / RabbitMQ → aura-gateway → aura-write-service → aura-read-service
```

## API 接口

所有请求通过 Gateway（端口 8080）统一入口。

### 用户

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/api/write/user/register` | 注册（phone + password） | 否 |
| POST | `/api/write/user/login` | 登录，返回 JWT | 否 |
| PUT | `/api/write/user/profile` | 更新昵称/邮箱 | 是 |
| GET | `/api/read/user/{id}` | 获取用户信息 | 否 |

### 新闻

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/api/write/news` | 发布新闻 | 是 |
| PUT | `/api/write/news/{id}` | 编辑新闻 | 是 |
| DELETE | `/api/write/news/{id}` | 删除新闻 | 是 |
| POST | `/api/write/news/{id}/like` | 点赞新闻 | 是 |
| GET | `/api/read/news/{id}` | 获取新闻详情 | 否 |
| GET | `/api/read/news/all?page=&size=` | 分页获取全部新闻 | 否 |
| GET | `/api/read/news/feed?page=&size=` | 个人 Feed 流 | 是 |
| GET | `/api/read/news/author/{authorId}` | 某用户的全部新闻 | 否 |

### 评论

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/api/write/comment` | 发表评论（支持 parentId 嵌套回复） | 是 |
| PUT | `/api/write/comment/{id}` | 编辑评论 | 是 |
| DELETE | `/api/write/comment/{id}` | 删除评论 | 是 |
| POST | `/api/write/comment/{id}/like` | 点赞评论 | 是 |
| GET | `/api/read/comment/news/{newsId}` | 获取新闻的评论树 | 否 |

### 关注

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/api/write/follow/{authorId}` | 关注 | 是 |
| POST | `/api/write/follow/unfollow/{authorId}` | 取消关注 | 是 |
| GET | `/api/read/follow/counts` | 获取粉丝/关注数 | 是 |
| GET | `/api/read/follow/following` | 我关注的人 | 是 |
| GET | `/api/read/follow/{authorId}/followers` | 某用户的粉丝列表 | 否 |
| GET | `/api/read/follow/check/{authorId}` | 检查是否已关注 | 否 |

## 架构设计

```
                          ┌───────────────────┐
                          │     浏览器/客户端   │
                          └─────────┬─────────┘
                                    │
                          ┌─────────▼─────────┐
                          │   aura-gateway     │
                          │    (端口 8080)      │
                          │   JWT 鉴权 + 路由   │
                          └──┬──────────────┬──┘
                  /api/read/**             /api/write/**
                             │                   │
              ┌──────────────▼─────┐   ┌────────▼──────────────┐
              │ aura-read-service  │   │ aura-write-service    │
              │    (端口 8081)      │   │    (端口 8082)        │
              │  CQRS 读取侧        │   │   CQRS 写入侧         │
              └──┬──────┬──────┘   └──┬─────┬──────────┬──┘
                 │      │             │     │          │
                 │      │             │     │          │
   ┌─────────────▼┐ ┌──▼────────┐ ┌──▼─────▼──────────▼──┐
   │  PostgreSQL   │ │ RabbitMQ  │ │       Redis          │
   │  (持久存储)    │ │ (异步MQ)  │ │  (缓存 + 分布式ID)    │
   └──────────────┘ └───────────┘ └──────────────────────┘
```

## Redis Key 说明

| Key 模式 | 类型 | 用途 |
|----------|------|------|
| `user:id:gen` | String | 用户自增 ID |
| `user:{id}` | String (JSON) | 用户缓存 |
| `user:phone:{phone}` | String | 手机号到用户 ID 映射 |
| `news:id:gen` | String | 新闻自增 ID |
| `news:{id}` | String (JSON) | 新闻缓存 |
| `news:all` | Sorted Set | 按时间排序的全部新闻 |
| `news:author:{id}` | Sorted Set | 某用户的新闻列表 |
| `news:like` | Sorted Set | 按点赞数排序的新闻 |
| `user:feed:{userId}` | List | 个性化 Feed 收件箱 |
| `comment:id:gen` | String | 评论自增 ID |
| `comment:news:{newsId}` | String (JSON) | 评论树 |
| `follow:user:{userId}` | Sorted Set | 我关注的人 |
| `follow:author:{authorId}` | Sorted Set | 我的粉丝 |

## License

MIT
