/**
 * 桌面端应用入口
 * - 挂载 Vue 应用，注册 Pinia / Router
 * - Element Plus 采用**按需精简注册**：只注册实际用到的组件、指令与各自样式，
 *   不再 `app.use(ElementPlus)` + `element-plus/dist/index.css` 全量引入；
 *   语言包改由 App.vue 的 <el-config-provider :locale="zhCn"> 提供
 * - 图标分两类：模板中静态使用的图标在组件内局部 import；
 *   菜单里按名称动态渲染的图标（`<component :is="'HomeFilled'">`）在此全局注册
 * - 全局注册按钮权限指令 v-perm（fail-closed：无权限/未加载/漏写标识均不渲染）
 *
 * 维护提示：新增 Element Plus 组件时，在下方组件数组与样式引入中各补一行，
 * 否则会出现"组件未解析"告警且界面元素不渲染（e2e-admin-login.cjs 已包含该断言）。
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'

// ---- Element Plus 组件（模板中仍写 <el-button> 等标签）----
import {
  ElAside,
  ElAvatar,
  ElBreadcrumb,
  ElBreadcrumbItem,
  ElButton,
  ElCard,
  ElCheckbox,
  ElCol,
  ElConfigProvider,
  ElContainer,
  ElDatePicker,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElDrawer,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElForm,
  ElFormItem,
  ElHeader,
  ElIcon,
  ElInput,
  ElInputNumber,
  ElLoading,
  ElMain,
  ElMenu,
  ElMenuItem,
  ElOption,
  ElPagination,
  ElRadio,
  ElRadioGroup,
  ElRow,
  ElScrollbar,
  ElSelect,
  ElSubMenu,
  ElTable,
  ElTableColumn,
  ElTag,
  ElTree,
  ElTreeSelect,
  ElUpload,
} from 'element-plus'

// ---- Element Plus 样式（按组件引入；各组件样式自带 base 变量）----
import 'element-plus/es/components/aside/style/css'
import 'element-plus/es/components/avatar/style/css'
import 'element-plus/es/components/breadcrumb/style/css'
import 'element-plus/es/components/breadcrumb-item/style/css'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/card/style/css'
import 'element-plus/es/components/checkbox/style/css'
import 'element-plus/es/components/col/style/css'
import 'element-plus/es/components/config-provider/style/css'
import 'element-plus/es/components/container/style/css'
import 'element-plus/es/components/date-picker/style/css'
import 'element-plus/es/components/descriptions/style/css'
import 'element-plus/es/components/descriptions-item/style/css'
import 'element-plus/es/components/dialog/style/css'
import 'element-plus/es/components/drawer/style/css'
import 'element-plus/es/components/dropdown/style/css'
import 'element-plus/es/components/dropdown-item/style/css'
import 'element-plus/es/components/dropdown-menu/style/css'
import 'element-plus/es/components/form/style/css'
import 'element-plus/es/components/form-item/style/css'
import 'element-plus/es/components/header/style/css'
import 'element-plus/es/components/icon/style/css'
import 'element-plus/es/components/input/style/css'
import 'element-plus/es/components/input-number/style/css'
import 'element-plus/es/components/loading/style/css'
import 'element-plus/es/components/main/style/css'
import 'element-plus/es/components/menu/style/css'
import 'element-plus/es/components/menu-item/style/css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/option/style/css'
import 'element-plus/es/components/pagination/style/css'
import 'element-plus/es/components/radio/style/css'
import 'element-plus/es/components/radio-group/style/css'
import 'element-plus/es/components/row/style/css'
import 'element-plus/es/components/scrollbar/style/css'
import 'element-plus/es/components/select/style/css'
import 'element-plus/es/components/sub-menu/style/css'
import 'element-plus/es/components/table/style/css'
import 'element-plus/es/components/table-column/style/css'
import 'element-plus/es/components/tag/style/css'
import 'element-plus/es/components/tree/style/css'
import 'element-plus/es/components/tree-select/style/css'
import 'element-plus/es/components/upload/style/css'

// ---- 图标：仅注册菜单按名称动态渲染所需的图标 ----
import {
  ArrowDown,
  Avatar as IconAvatar,
  DataLine,
  Document,
  Finished,
  Grid,
  HomeFilled,
  Medal,
  Menu as IconMenu,
  Monitor,
  OfficeBuilding,
  Operation,
  User,
  UserFilled,
} from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { permission } from './directives/permission'
// 样式引入顺序：设计 token（数值唯一出处） -> 组件库变量映射 -> 组件级精修 -> 全局基础样式
import './styles/tokens.css'
import './styles/theme.css'
import './styles/components.css'
import './assets/main.css'

const app = createApp(App)

// Element Plus 组件：逐个注册（各组件自带 install，可按需引入）
// 维护提示：新增组件时在此补一行 + 在样式区补一行；组件不再使用时两处一起删除
//（E5 已按此规则移除 el-alert 与 el-result：改用自有的说明行与 StatusPage 组件）
const elementComponents = [
  ElAside, ElAvatar, ElBreadcrumb, ElBreadcrumbItem, ElButton, ElCard, ElCheckbox,
  ElCol, ElConfigProvider, ElContainer,
  // 审计页新增：日期范围筛选、明细抽屉与描述列表（样式见上方 style 导入）
  ElDatePicker, ElDescriptions, ElDescriptionsItem, ElDrawer,
  ElDialog, ElDropdown, ElDropdownItem, ElDropdownMenu,
  ElForm, ElFormItem, ElHeader, ElIcon, ElInput, ElInputNumber, ElMain, ElMenu, ElMenuItem,
  ElOption, ElPagination, ElRadio, ElRadioGroup, ElRow, ElScrollbar, ElSelect,
  ElSubMenu, ElTable, ElTableColumn, ElTag, ElTree, ElTreeSelect, ElUpload,
]
for (const component of elementComponents) {
  app.use(component)
}
// v-loading 指令与服务
app.use(ElLoading)

// 菜单图标按名称动态渲染（MenuItem 中 <component :is="'HomeFilled'" />），需全局注册
const menuIcons = {
  ArrowDown,
  HomeFilled,
  Operation,
  User,
  UserFilled,
  Finished,
  Menu: IconMenu,
  DataLine,
  Avatar: IconAvatar,
  Grid,
  Medal,
  Document,
  Monitor,
  OfficeBuilding,
}
for (const [name, component] of Object.entries(menuIcons)) {
  app.component(name, component)
}

// 全局注册按钮级权限指令：v-perm="'sysAuthUser:addSysAuthUser'"
app.directive('perm', permission)

app.use(createPinia())
app.use(router)

// 渲染异常兜底：记录完整错误，再跳转到 500 页面；错误页自身异常时不再递归跳转
app.config.errorHandler = (err, instance, info) => {
  console.error('[app] 未处理的渲染异常：', err, info)
  if (router.currentRoute.value.path !== '/500') {
    router.replace('/500').catch(() => {})
  }
}

app.mount('#app')
