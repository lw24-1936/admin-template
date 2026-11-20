import { readonly, ref } from 'vue'

/**
 * 轻量 i18n（无第三方依赖）。
 *
 * 当前覆盖应用外壳、登录页、错误页与已知菜单名；业务页面的字段/校验文案仍以中文为主，
 * 新增页面时应优先使用 t('key') 而不是硬编码，便于后续逐步补齐。
 */
const STORAGE_KEY = 'admintem_locale'
const supported = ['zh-CN', 'en-US']
const locale = ref(localStorage.getItem(STORAGE_KEY) || 'zh-CN')

const messages = {
  'zh-CN': {
    'common.search': '查询',
    'common.reset': '重置',
    'common.confirm': '确定',
    'common.cancel': '取消',
    'common.save': '保存',
    'common.delete': '删除',
    'common.loading': '加载中',
    'common.home': '返回首页',
    'common.reload': '重新加载',
    'theme.toLight': '切换到浅色主题',
    'theme.toDark': '切换到深色主题',
    'language.switch': '切换语言',
    'menu.home': '工作台',
    'menu.user': '用户管理',
    'menu.role': '角色管理',
    'menu.permission': '权限管理',
    'menu.dept': '部门管理',
    'menu.online': '在线会话',
    'menu.audit': '操作审计',
    'menu.config': '系统参数',
    'menu.dict': '数据字典',
    'menu.file': '文件中心',
    'header.profile': '个人资料',
    'header.password': '修改密码',
    'header.logout': '退出登录',
    'header.mfa': '两步验证',
    'login.title': '登录',
    'login.subtitle': '使用您的账号继续访问',
    'login.username': '账号',
    'login.password': '密码',
    'login.usernamePlaceholder': '请输入账号',
    'login.passwordPlaceholder': '请输入密码',
    'login.remember': '记住我（7 天内免登录）',
    'login.submit': '登 录',
    'login.noAccount': '还没有账号？',
    'login.createAccount': '创建账号',
    'login.mfaTitle': '两步验证',
    'login.mfaHint': '请输入认证器中的 6 位动态验证码',
    'login.mfaCode': '6 位验证码',
    'login.mfaSubmit': '验证并登录',
    'login.mfaCancel': '返回账号登录',
    'error.403.title': '无权访问',
    'error.403.desc': '当前账号没有访问此页面的权限，请联系管理员开通。',
    'error.404.title': '页面不存在',
    'error.404.desc': '地址可能已失效，或页面已被移除。',
    'error.500.title': '服务暂时不可用',
    'error.500.desc': '服务器处理请求时出现异常，请稍后重试。',
  },
  'en-US': {
    'common.search': 'Search',
    'common.reset': 'Reset',
    'common.confirm': 'Confirm',
    'common.cancel': 'Cancel',
    'common.save': 'Save',
    'common.delete': 'Delete',
    'common.loading': 'Loading',
    'common.home': 'Back to home',
    'common.reload': 'Reload',
    'theme.toLight': 'Switch to light theme',
    'theme.toDark': 'Switch to dark theme',
    'language.switch': 'Switch language',
    'menu.home': 'Dashboard',
    'menu.user': 'Users',
    'menu.role': 'Roles',
    'menu.permission': 'Permissions',
    'menu.dept': 'Departments',
    'menu.online': 'Online sessions',
    'menu.audit': 'Audit log',
    'menu.config': 'System config',
    'menu.dict': 'Dictionaries',
    'menu.file': 'Files',
    'header.profile': 'Profile',
    'header.password': 'Change password',
    'header.logout': 'Sign out',
    'header.mfa': 'Two-factor auth',
    'login.title': 'Sign in',
    'login.subtitle': 'Continue with your account',
    'login.username': 'Account',
    'login.password': 'Password',
    'login.usernamePlaceholder': 'Enter account',
    'login.passwordPlaceholder': 'Enter password',
    'login.remember': 'Remember me for 7 days',
    'login.submit': 'Sign in',
    'login.noAccount': 'No account yet?',
    'login.createAccount': 'Create account',
    'login.mfaTitle': 'Two-factor authentication',
    'login.mfaHint': 'Enter the 6-digit code from your authenticator',
    'login.mfaCode': '6-digit code',
    'login.mfaSubmit': 'Verify and sign in',
    'login.mfaCancel': 'Back to password sign in',
    'error.403.title': 'Access denied',
    'error.403.desc': 'Your account cannot access this page. Contact an administrator.',
    'error.404.title': 'Page not found',
    'error.404.desc': 'The address may be invalid or the page has been removed.',
    'error.500.title': 'Service unavailable',
    'error.500.desc': 'The server encountered an error. Please retry later.',
  },
}

const menuKeys = {
  工作台: 'menu.home',
  用户管理: 'menu.user',
  角色管理: 'menu.role',
  权限管理: 'menu.permission',
  部门管理: 'menu.dept',
  在线会话: 'menu.online',
  操作审计: 'menu.audit',
  系统参数: 'menu.config',
  数据字典: 'menu.dict',
  文件中心: 'menu.file',
}

export function t(key, fallback = key) {
  const dict = messages[locale.value] || messages['zh-CN']
  return dict[key] ?? messages['zh-CN'][key] ?? fallback
}

/** 菜单名来自数据库，已知名称翻译；未登记的名称原样返回。 */
export function translateMenuName(name) {
  const key = menuKeys[name]
  return key ? t(key, name) : name
}

export function setLocale(value) {
  const next = supported.includes(value) ? value : 'zh-CN'
  locale.value = next
  localStorage.setItem(STORAGE_KEY, next)
  document.documentElement.lang = next
}

export function getLocale() {
  return locale.value
}

export function useI18n() {
  return {
    locale: readonly(locale),
    supported,
    t,
    setLocale,
    translateMenuName,
  }
}

if (typeof document !== 'undefined') {
  document.documentElement.lang = locale.value
}
