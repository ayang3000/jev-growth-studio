# Jev Growth Studio

一套基于 **JDK 17 + Spring Boot 4 + Spring AI 2 + Jev** 的 ASO / SEO 优化系统。它不是简单地把“写一篇 SEO 文案”交给大模型，而是把流程拆成两个职责清晰的智能层：

- **Jev（System One 决策层）**：生成前并行执行策略路由、关键词意图分类、机会评分与风险判断；生成后再次判断意图对齐、关键词自然度、品牌安全、事实落地与发布路由。
- **Spring AI（生成层）**：只使用 Jev 通过的关键词与策略，生成符合 JSON Schema 的 SEO 或 ASO 内容。
- **确定性应用层**：执行阈值门禁、保存报告、制定 14 天实验方案、接收真实指标反馈。概率只作为信号，不自动授权发布。

## 能力

```text
优化简报 + 关键词指标
        │
        ▼
  Jev typed decisions
 Choice / Score / Noul
        │
        ├── 低置信度 / 品牌风险 ──► REVIEW_REQUIRED
        │
        ▼
 Spring AI 结构化生成
        │
        ▼
 内容草案 + 实验计划 + 发布门禁
        │
        ▼
 线上指标反馈（CTR/CVR/留存等）
```

系统提供：

- SEO：标题、slug、meta description、正文概要、内容结构、关键词、CTA、Schema 类型建议。
- ASO：30 字符标题、30 字符副标题、80 字符短描述、长描述、关键词、截图文案。
- Jev：每个候选词的意图、0–100 适配分和采用概率；整体策略、机会、就绪度、品牌安全和复核判定。
- API、H2 持久化、Actuator 健康检查、浏览器工作台与无外部密钥演示模式。

## 快速启动

要求：JDK 17。项目已内置 Gradle Wrapper，无需预装 Gradle。

```bash
./gradlew bootRun
```

打开 [http://localhost:8080](http://localhost:8080)。默认使用本地启发式决策和模板生成器，便于零密钥验收；所有结果会被标记为 `REVIEW_REQUIRED`，不会伪装成 Jev 或大模型结果。

也可以统一用启动脚本。复制示例配置到 Git 忽略的 `.env`，按需把 `JEV_ENABLED` 和 `AI_ENABLED` 改为 `true` 并填写对应的真实 API Key，然后运行：

```bash
cp .env.example .env
./start.sh
```

脚本会从 `.env` 加载配置并启动一次应用；也可以不创建 `.env`，直接通过环境变量配置。不要把真实密钥写入 `start.sh` 或提交到 Git。

运行测试：

```bash
./gradlew test
```

## 启用 Jev

创建服务端 API Key 后设置：

```bash
export JEV_ENABLED=true
export JEV_API_KEY="your-key"
./gradlew bootRun
```

默认调用 `https://www.jevai.org/api/v1/decisions`，模型为 `typesafe-ai/jev`。如果使用其他兼容的 Jev System One 服务，可配置：

```bash
export JEV_URL="https://your-provider.example/v1/systemone"
export JEV_MODEL="jev-latest"
```

实现同时兼容直接返回 `{ model, answers, usage }` 以及 `{ code, message, data }` 包装格式。Jev 请求把所有 `choice`、`score` 和 `noul` 问题放在同一次调用中并行评估。

## 启用 Spring AI

当前生成器使用 Spring AI 的 `ChatClient` 和结构化输出 Schema 校验。默认 OpenAI，也支持 OpenAI-compatible 服务：

```bash
export AI_ENABLED=true
export SPRING_AI_CHAT_MODEL=openai
export OPENAI_API_KEY="your-key"
export OPENAI_MODEL="gpt-5-mini"
# 可选：export OPENAI_BASE_URL="https://your-compatible-api.example"
./gradlew bootRun
```

建议在生产环境同时启用 Jev 和 Spring AI；只启用其中一层时，系统仍会安全降级并要求人工复核。

## API

创建优化报告：

```bash
curl -X POST http://localhost:8080/api/v1/optimizations \
  -H 'Content-Type: application/json' \
  -d '{
    "channel":"SEO",
    "productName":"FlowNote",
    "market":"中国大陆",
    "language":"zh-CN",
    "audience":"需要整理会议纪要的产品团队",
    "goal":"提升自然搜索注册转化",
    "keywords":[
      {"keyword":"AI会议纪要","monthlyVolume":12000,"difficulty":42,"currentRank":18},
      {"keyword":"会议纪要工具","monthlyVolume":8000,"difficulty":35,"currentRank":26}
    ],
    "brandRules":["不得承诺百分之百准确","不得虚构统计数据"]
  }'
```

其他端点：

| 方法 | 路径 | 用途 |
|---|---|---|
| `GET` | `/api/v1/optimizations/{id}` | 获取报告 |
| `GET` | `/api/v1/optimizations?limit=20` | 最近报告 |
| `POST` | `/api/v1/optimizations/{id}/feedback` | 回传实验指标 |
| `GET` | `/api/v1/system/status` | 查看 Jev / 生成器运行模式 |
| `GET` | `/actuator/health` | 健康检查 |

反馈示例：

```json
{
  "metric": "organic_ctr",
  "baseline": 0.031,
  "observed": 0.044,
  "notes": "14 天实验，流量分配 50/50"
}
```

## 生产化建议

- 将 H2 换成 PostgreSQL，并把实验与版本作为独立实体管理。
- 从 Search Console、App Store Connect、Google Play Console 或第三方关键词平台自动导入真实指标。
- 将同步接口改为队列任务；对 Jev 的 `429/529` 做指数退避与熔断。
- 为不同市场校准 Jev 阈值；当前默认置信度阈值为 `0.68`、品牌安全概率为 `0.80`。
- 发布动作必须位于独立权限系统之后；本项目只生成建议，不直接修改站点或商店资产。

## 参考

- [Spring AI ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
- [Spring AI Structured Output](https://docs.spring.io/spring-ai/reference/api/structured-output/validation.html)
- [Jev REST API](https://www.jevai.org/docs)
