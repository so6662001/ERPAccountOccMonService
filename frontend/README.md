# 普讯科技产品应用数据监测平台 — 前端

Vue3 + TypeScript + Vite + Element Plus + Pinia + Vue Router。对接 `backend/` 接口，还原 `prototype/` 设计。

## 运行
```bash
npm install
npm run dev      # http://localhost:5173 （已配置 /api 代理到 http://localhost:8080）
npm run build    # 产物输出 dist/
```

> 默认登录 admin / admin123（后端首启自动创建）。

## 结构
```
src/
  api/        http 封装(ApiResult 解包/JWT 注入/401 跳转) + 各模块接口
  stores/     Pinia(auth)
  router/     路由 + 登录守卫(hash 模式)
  layouts/    AppLayout(深色侧边栏+顶栏，沿用原型风格)
  views/      Login / Dashboard / CustomerList / RuleList / RuleTemplates /
              RunList / RunDetail / Analytics / RolloutList / AlertCenter / WecomSettings
  styles/     主题(沿用原型设计令牌)
  types/      ApiResult/PageResult/级别与状态映射
```

## 已对接接口
认证、客户、规则(+历史回滚)、模板库、检测结果(+运行详情/违规样本)、趋势统计、灰度发布(启动/推进/回滚)、告警中心(认领/闭环/误报)、企业微信渠道(新增/测试)。

已实现页面：登录、总览、客户、规则列表、**规则编辑器**、模板库、**模板差异同步**、检测结果/运行详情、趋势统计、灰度发布、告警中心、企业微信渠道、**数据源管理**、**检测任务**。

## 构建优化（代码分割）
`vite.config.ts` 通过 `manualChunks` 将依赖拆分为独立可缓存的 chunk：
- `element-plus`（UI 库）、`vue-vendor`（vue/vue-router/pinia）、`vendor`（其余依赖）
- 各路由页面经动态 `import()` 自动按需分包（每页 2–5 KB）

效果：原单一 ~1.2 MB 主包拆分为 vendor 大包独立缓存 + 极小的页面分片，首屏与后续导航按需加载。

### Element Plus 按需引入
已接入 `unplugin-auto-import` + `unplugin-vue-components`（`ElementPlusResolver`，`importStyle: 'css'`）：
- 组件与 `ElMessage` 等 API 按使用自动引入（无需全量 `app.use(ElementPlus)` 与全量 CSS）
- CSS 由 359 KB 整包降为约 197 KB（仅用到的组件样式，gzip ~26 KB）

## 单元 / 组件测试（Vitest）
```bash
npm run test       # 运行
npm run coverage   # 覆盖率
```
- `src/utils/display.spec.ts`：映射与格式化纯函数
- `src/components/SeverityTag.spec.ts`：组件挂载与渲染（@vue/test-utils + jsdom）
