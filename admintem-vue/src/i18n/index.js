/**
 * 轻量文案表（仅简体中文）。
 *
 * 系统不再提供中英文切换；保留 t() 只是为了让外壳、登录、错误页与菜单文案
 * 有统一出处，避免散落的硬编码。业务页面仍可直接使用中文文案。
 */
const messages = {
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
  'login.forgot': '忘记密码',
  'error.403.title': '无权访问',
  'error.403.desc': '当前账号没有访问此页面的权限，请联系管理员开通。',
  'error.404.title': '页面不存在',
  'error.404.desc': '地址可能已失效，或页面已被移除。',
  'error.500.title': '服务暂时不可用',
  'error.500.desc': '服务器处理请求时出现异常，请稍后重试。',
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
  return messages[key] ?? fallback
}

/** 菜单名来自数据库，已知名称映射到统一文案；未登记的名称原样返回。 */
export function translateMenuName(name) {
  const key = menuKeys[name]
  return key ? t(key, name) : name
}

export function useI18n() {
  return { t, translateMenuName }
}
