// ---------------------------------------------------------------------------
// ESLint 扁平配置（桌面端）
//
// 设计原则：**只拦"真错误"，不争风格**
//   - vue3-essential：模板/组件的硬错误（重复 key、v-for 缺 key 之外的结构性问题等）；
//   - no-unused-vars：未使用变量（warn 而非 error，先让问题可见）；
//   - no-undef / no-dupe-keys：运行时会直接报错的那类问题；
//   - 关闭 multi-word-component-names 等"命名偏好"规则，避免为了过 lint 去改名。
//
// 为什么用 warn：存量代码里已有一些 console/未使用变量，一上来全设 error 会让
// `npm run lint` 长期红着，最后没人再看它。先让 `--max-warnings` 兜住新增问题。
// ---------------------------------------------------------------------------
import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'

export default [
  {
    // 不检查构建产物与依赖
    ignores: ['dist/**', 'node_modules/**', 'coverage/**'],
  },
  js.configs.recommended,
  ...pluginVue.configs['flat/essential'],
  {
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        // 浏览器环境（项目没有引入 globals 包，这里列出实际用到的）
        window: 'readonly',
        document: 'readonly',
        localStorage: 'readonly',
        sessionStorage: 'readonly',
        console: 'readonly',
        setTimeout: 'readonly',
        clearTimeout: 'readonly',
        setInterval: 'readonly',
        clearInterval: 'readonly',
        requestAnimationFrame: 'readonly',
        cancelAnimationFrame: 'readonly',
        fetch: 'readonly',
        URL: 'readonly',
        URLSearchParams: 'readonly',
        FormData: 'readonly',
        Blob: 'readonly',
        process: 'readonly',
      },
    },
    rules: {
      // 命名偏好类：不强制（组件用 View 后缀已足够表意）
      'vue/multi-word-component-names': 'off',
      // 存量代码存在未使用变量，先降级为提示
      'no-unused-vars': ['warn', { args: 'none', varsIgnorePattern: '^_' }],
      // 控制台输出：开发期日志与错误上报是刻意保留的
      'no-console': 'off',
      // 空块（catch 里只写注释的情况）允许
      'no-empty': ['warn', { allowEmptyCatch: true }],
    },
  },
  {
    // 测试脚本允许 node 环境写法
    files: ['tests/**/*.js', '*.config.js'],
    languageOptions: {
      globals: { console: 'readonly', process: 'readonly', __dirname: 'readonly' },
    },
  },
]
