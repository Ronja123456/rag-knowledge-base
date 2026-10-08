# RAG 智能知识库问答系统

## 项目简介

基于 RAG（检索增强生成）的智能知识库问答系统。用户上传文档后，系统
自动切分、向量化、存储；提问时先检索相关片段，再交给大模型基于资料
作答，解决大模型的幻觉和知识过时问题。

## 技术栈

- Spring Boot 3
- 通义千问 Embedding（文本向量化）
- DeepSeek（答案生成）
- Redis（对话历史存储）
- SSE（流式输出）

## 核心功能

### 1. 文档处理
- 按段落 + 滑动窗口切分，约 200 字/段，相邻重叠 50 字
- 通义千问 Embedding 生成 1024 维向量
- 存入向量库（当前内存计算，后续可换 Milvus / Chroma）

### 2. 检索
- 用户问题向量化后，与候选片段计算余弦相似度
- 取 Top-3 作为参考资料，平衡召回率与上下文长度

### 3. 生成
- 检索片段与问题拼接为 Prompt，调用 DeepSeek
- Prompt 约束"资料未提及则回答未提及"，降低幻觉

### 4. 多轮对话
- Redis 保存最近 10 条历史，30 分钟过期
- 支持指代消解（如"它有哪些功能"）

### 5. 流式输出
- SSE 逐字返回，降低首字延迟

## 项目结构
src/main/java/com/rag/
├── controller/ # 接口层
│ ├── DocumentController.java
│ ├── ChatController.java
│ └── RagController.java
├── service/ # 业务层
│ ├── DocumentService.java # 文档切分
│ ├── EmbeddingService.java # 向量化
│ ├── VectorStoreService.java # 向量存储 + 检索
│ └── ChatHistoryService.java # 对话历史
├── entity/ # 实体
├── config/ # 配置
│ ├── RestTemplateConfig.java
│ └── AiConfig.java
└── utils/ # 工具
└── DeepSeekClient.java

## 接口说明

| 接口 | 方法 | 说明 |
|------|------|------|
| `/rag/upload` | POST | 上传文档，body 传 `{"text": "内容"}` |
| `/rag/ask` | GET | 同步问答，参数 `question`、`sessionId` |
| `/rag/ask/stream` | GET | 流式问答，参数 `question` |
## 运行方式

1. 启动 Redis
2. 参考 `application-example.yaml` 配置 `application.yaml`
3. 配置 DeepSeek 和通义千问的 API Key
4. 运行 RagApplication

## 优化方向

- chunk 切分：按语义切分，别硬切
- rerank：先粗排 topK，再用重排模型精排
- 混合检索：向量检索 + 关键词检索（BM25）
- 向量库：数据量大时换成 Milvus / Chroma
- 缓存：高频问题缓存回答

## 效果

- 构造 20 条测试问题，Top-3 召回率约 90%
- 人工抽检 20 条 AI 回答，未出现资料外编造
- 多轮对话支持指代消解
- SSE 流式输出首字响应明显快于同步方案