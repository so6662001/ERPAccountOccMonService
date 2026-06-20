import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './styles/theme.css'

// Element Plus 按需引入（unplugin 自动注册组件与样式，无需全量引入）
const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
