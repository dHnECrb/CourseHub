# AGENTS.md

本文件面向 AI 编码代理，介绍 springDemo 项目的架构、构建方式与开发约定。项目注释、日志和用户界面均使用简体中文，修改代码时请保持一致。

## 项目概述

springDemo 是一个基于 Spring Boot 的 Web 应用（作者的第一个 Spring Boot 学习项目，`src/main/resources/banner.txt`），实现了一个简易的高校选课系统，包含以下功能：

- 用户登录/注销，角色分为管理员（admin）、学生（student）、教师（teacher）三种
- 基于 HttpSession 的会话管理：前端每 30 秒发送心跳，服务器定时剔除超时会话，前端检测 5 分钟无操作自动注销
- 任务系统（UserTask）：按角色（含 "all"）和起止时间控制功能入口的开放
- 学生预选课页面（`/student/select_course`，目前前后端尚未完全接通）
- 选课结果查询页面（`/admin/course_select_result`，管理员按课程/按学生两个维度查看选课情况）

顶层包为 `com.example.springdemo`，主类是 `SpringDemoApplication.java`（同时充当 `/index` 路径的演示用 Controller）。

## 技术栈

| 类别 | 技术 | 版本 |
| --- | --- | --- |
| 框架 | Spring Boot（spring-boot-starter-parent） | 4.1.1 |
| 语言 | Java | 17 |
| ORM | MyBatis-Plus（mybatis-plus-spring-boot4-starter） | 3.5.17 |
| 数据库 | MySQL（mysql-connector-java），数据库名 `env1_db` | 8.0.33 |
| 模板引擎 | Thymeleaf（spring-boot-starter-thymeleaf） | 随 Boot |
| 工具 | Lombok | 随 Boot |
| 测试 | JUnit 5 / spring-boot-starter-test | 随 Boot |

前端为服务端渲染的 Thymeleaf 模板 + 原生 JavaScript/CSS（无 npm/前端构建）。

## 构建与运行

使用 Maven Wrapper（`mvnw`），无需本地安装 Maven：

```bash
./mvnw clean compile        # 编译
./mvnw test                 # 运行测试
./mvnw clean package        # 打包，生成 target/springDemo-0.0.1-SNAPSHOT.jar
./mvnw spring-boot:run      # 直接运行
java -jar target/springDemo-0.0.1-SNAPSHOT.jar   # 运行打包产物
```

运行前提：

1. 本机 MySQL（默认 `localhost:3306`）中存在数据库 `env1_db`，表包括：`user`、`userPassword`、`course`、`select_course`、`task`（表名与实体对应关系见下文）。项目不含建表 SQL 或迁移工具（如 Flyway），表结构需自行维护。
2. `src/main/resources/application.yml` 提供数据源账号密码。**该文件被 .gitignore 排除（注释注明"包含密码等信息"），克隆后需自行创建**；修改配置时不要把它提交进版本库。
3. 配置项：服务端口 `8080`，上下文路径（context-path）为 `/app_1`，即登录页地址是 `http://localhost:8080/app_1/login`。

无 Dockerfile、CI 配置或正式部署流程；`HELP.md` 仅保留了 Spring Initializr 生成的参考链接。

## 代码组织（`src/main/java/com/example/springdemo/`）

- `SpringDemoApplication.java`：启动类，带 `@EnableScheduling`（定时任务）和 `@MapperScan("com.example.springdemo.mapper")`；同时是 `/index` 下的演示接口。
- `controller/`：登录（`LoginController`，`/login`）、主界面（`UserMainController`，`/main`，含心跳、任务列表、注销）、选课（`SelectCourseController`，`/student/select_course`）、选课结果查询（`SeeSelectedCourseController`，`/admin/course_select_result`，管理员查看学生选课汇总与明细）；`UserToLogin` 是登录表单 DTO。
- `service/`：业务逻辑。`UserLoginService`（登录校验）、`UserMainService`（会话心跳/注销）、`UserTaskService`（按角色/时间校验任务入口，`tryGoTask` 返回特定错误码）、`SelectCourseService`、`SeeSelectedCourseService`（管理员的选课结果查询：按课程汇总 GROUP_CONCAT 学生姓名、学生×课程明细矩阵，统计下学期 `YearTerm.now().next()` 的预选数据，学生口径为 `user` JOIN `userPassword` 中 `position='student'`）、`ScheduledTasksService`（每 30 秒剔除无心跳会话）、`BasicFunctions`（时间格式化、日志打印、防时序攻击的字符串比较等静态工具）、`RequestResult`（`record(isSucceed, code, string)`，各层通用的结果载体）、`LoginStatus`（目前未被使用的枚举）。
- `manager/`：`OnlineUserManager`（`ConcurrentHashMap<HttpSession, Long>` 维护在线会话，2 分钟无心跳视为离线）；`LoginCounter`（枚举单例，统计各角色登录人次）。
- `entity/`：`User`（表 `user`）、`UserPassword`（表 `userPassword`）、`Course`（表 `course`）、`CourseSelect`（表 `select_course`）、`UserTask`（表 `task`），以及枚举 `UserPosition` 和值对象 `YearTerm`（学年学期，格式 `YYYY-YYYY-1/2`，9~2 月为第一学期）。
- `mapper/`：5 个 MyBatis-Plus 的 `BaseMapper` 接口（`@Mapper` 标注），XML 文件在 `src/main/resources/mapper/*.xml`。

### 资源文件（`src/main/resources/`）

- `application.yml`：数据源等配置（见上文，被 gitignore）。
- `templates/`：Thymeleaf 页面（index 即登录页、login_* 系列结果页、user_main、select_course、course_select_result、error/403）。
- `static/`：原生 JS/CSS（`heartbeat.js` 心跳与空闲检测、`script1.js` 登录表单）。
- `mapper/*.xml`：MyBatis 映射文件，**内容几乎全部被注释掉**——项目实际使用 MyBatis-Plus 的 `BaseMapper` 方法，手写 SQL 仅作历史参考保留。
- `messages_zh_CN.properties`：唯一的国际化消息文件，`logMessage.*`（服务端日志）与 `responseMessage.*`（前端提示）两组 key 通过 `MessageSource` 使用；`messages.properties` 为空文件；`messages_en_US.properties `（**文件名末尾有一个空格**）也是空文件。

## 开发约定

- **注释、日志、提示信息用简体中文**；Javadoc 风格的中文注释在 service 层较常见。
- 依赖注入主要用 `jakarta.annotation.Resource`，个别地方用 `@Autowired` 或构造器注入；非空参数标注 `org.jspecify.annotations.NonNull`。
- 实体类用 Lombok `@Data` + `@TableName("表名")`（表名带反引号转义），但仍手写 getter 和全参/单参构造器。**字段命名使用蛇形命名（`start_time`、`deadline_time`、`will_offer`、`user_id`）以匹配数据库列**，新实体请沿用。
- 业务结果用 `RequestResult(boolean isSucceed, int code, String string)` 传递；`code` 约定：200/0 成功，401 未登录，403 无权限/时间不合适，404 不存在，1 任务未开始，2 任务已截止（`UserTaskService.tryGoTask`）。
- 日志统一走 `BasicFunctions.printTimeHttpSessionAnd(...)`（带时间戳和 sessionId 的 `System.out.println`）；`ScheduledTasksService` 用 `java.util.logging.Logger`。日志文案取自 `messages_zh_CN.properties` 的 `logMessage.*`，`Locale.CHINA` 硬编码。
- 废弃代码以注释块形式保留（mapper 接口、XML、service 中的大段注释），这是本项目既有风格，修改时可参考但不必清理。
- 前端 JS 中的请求路径硬编码了上下文路径 `/app_1`（如 `fetch('/app_1/main/heartbeat')`），模板内则使用 Thymeleaf 的 `@{...}` 语法（自动适配 context-path）。**改动 context-path 时需要同步修改前端 JS 和后端 redirect 字符串**。

## 测试

- 唯一的测试类是 `src/test/java/com/example/springdemo/MapperTests.java`（`@SpringBootTest`），直接对真实数据库 `env1_db` 做增删改查，本质是手写的数据库联调脚本，而不是隔离的单元测试：会插入/修改/删除真实数据（如 `testSelect` 断言用户恰好 6 个、`testCreateUser` 插入 id=12 的用户），且用例间有数据依赖。
- 运行测试 `./mvnw test` 前必须先启动 MySQL 并准备好 `application.yml`。执行测试会污染数据库数据，请注意。
- 项目没有 MockMvc、Web 层测试或前端测试。

## 安全注意事项

- 密码以**明文**存储在 `userPassword` 表，登录比较用 `BasicFunctions.safeEquals`（`MessageDigest.isEqual`，防时序攻击）。这是学习项目的简化处理，生产化时需改为哈希存储。
- `application.yml` 含数据库密码且已被 gitignore；绝对不要将其内容提交或输出到日志/截图。
- 登录失败统一返回"账号或密码错误"，不区分账号不存在与密码错误（防暴力破解，见 `UserLoginService.login` 注释），修改登录逻辑时请保留该行为。
- SQL 均由 MyBatis-Plus 拼接，未发现字符串拼接 SQL；XML 中残留的手写 SQL 使用 `#{}` 参数占位。

## 已知问题与注意事项

- `src/main/resources/templates/.idea/` 下误提交了一套 IDE 配置目录，与根目录 `.idea/` 无关，可考虑删除。
- `messages_en_US.properties `（文件名末尾有空格）和 `messages.properties` 均为空文件，只有中文消息真正生效。
- 选课功能尚未完成：`SelectCourseService` 有取课、批量改选方法，但 `SelectCourseController` 只有入口页，`select_course.html` 的接口对接仍在开发中（工作区有未提交的改动）。
- 选课结果查询（`SeeSelectedCourseController`，`/admin/course_select_result`）已完成：任务入口依赖 `task` 表中 site 为 `/admin/course_select_result`、position 为 `admin` 的记录（本地数据库已存在），没有该记录时入口返回 404。
- `LoginStatus` 枚举、`SpringDemoApplication` 中被注释的 `/test` 接口等属于遗留/未接线的代码。
