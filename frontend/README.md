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

页面与原型 22 页对应，核心业务页已实现；其余页面(数据源/任务/客户详情/规则编辑器/模板差异等)按相同模式扩展即可。
