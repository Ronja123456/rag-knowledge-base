# 企业 AI 知识库客服（公开演示版）

基于 RAG 的公开演示：访客无需登录，只能查询 3 个预置知识库；不能上传、删除或修改资料。

## 技术栈

- Spring Boot 3
- 通义千问 Embedding（`DASHSCOPE_API_KEY`）
- DeepSeek 生成（`DEEPSEEK_API_KEY`）
- Redis：向量缓存、按 IP 每日限流
- SSE：流式回答

## 演示能力

- 页面：`http://localhost:8081/demo`
- 预置知识库：电商退换货 FAQ、教培课程咨询、财税公司业务介绍
- 启动时若不存在则初始化，已存在则从 Redis 加载
- 每个 IP 每天最多提问 20 次
- 回答附带引用片段；资料未覆盖时回复「资料未提及，请转人工」
- DeepSeek 超时或失败时降级为「当前咨询较多，请稍后再试或联系人工」

## 环境变量（必须）

| 变量 | 说明 |
|------|------|
| `DEEPSEEK_API_KEY` | DeepSeek API Key |
| `DASHSCOPE_API_KEY` | 通义千问 / DashScope API Key |

可选：`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`。

不要把 Key 写进代码或提交到 Git。`application.yaml` 已在 `.gitignore` 中。

## 运行方式

1. 启动 Redis
2. 设置环境变量后启动 `RagKnowledgeBaseApplication`
3. 打开 `/demo`

Windows PowerShell 示例：

```powershell[main](src/main)
$env:DEEPSEEK_API_KEY="你的Key"
$env:DASHSCOPE_API_KEY="你的Key"
mvn spring-boot:run
```

## 公开接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/demo` | GET | 演示页 |
| `/api/demo/knowledge-bases` | GET | 预置知识库列表 |
| `/api/demo/ask/stream` | POST | 流式问答（SSE） |
| `/api/demo/handoff` | POST | 转人工登记 |

上传、清空、内部调试接口默认关闭（返回 404）。本地排障可将 `demo.admin-api-enabled` 设为 `true`。

## 说明

系统 Prompt、密钥与内部检索参数不会返回给前端。
