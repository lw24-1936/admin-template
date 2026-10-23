/**
 * 站点内容单一出处
 *
 * 换品牌只改这个文件：名称、导航、文案、功能、价格、FAQ、联系方式。
 * 视图里不写死文案，方便把一个官网模版复制成多个产品站。
 */
export const site = {
  name: 'Nova',
  tagline: '让团队协作回归简单',
  description: '文档、任务、知识与自动化集中在一处，减少工具切换。',
  contact: {
    email: 'hello@nova.example',
    phone: '+86 400-000-0000',
    address: '上海市徐汇区漕河泾开发区',
    hours: '工作日 09:00–18:00',
  },

  nav: [
    { label: '功能', to: '/features' },
    { label: '价格', to: '/pricing' },
    { label: '关于', to: '/about' },
    { label: '联系', to: '/contact' },
  ],

  actions: {
    primary: { label: '免费开始', to: '/pricing' },
    secondary: { label: '预约演示', to: '/contact' },
    login: { label: '登录', href: 'https://app.example.com' },
  },

  hero: {
    eyebrow: '全新 3.0 · 已支持团队知识库',
    title: ['把分散的协作，', '收进一个空间'],
    highlight: '一个空间',
    description:
      'Nova 把文档、任务、知识与自动化放在同一处。团队少切换一次工具，就多一段专注时间。',
    stats: [
      { value: 12000, suffix: '+', label: '团队正在使用' },
      { value: 38, suffix: '%', label: '平均会议时长下降' },
      { value: 99.98, suffix: '%', label: '过去 12 个月可用性', decimals: 2 },
    ],
  },

  logos: ['Acme', 'Linear', 'Vercel', 'Notion', 'Figma', 'Stripe'],

  features: [
    {
      icon: 'FileText',
      title: '文档即工作台',
      desc: '需求、方案与复盘写在同一个空间，评论和任务自动关联，不再靠链接串联。',
      span: 'lg:col-span-3',
    },
    {
      icon: 'ListChecks',
      title: '任务自动流转',
      desc: '把文档里的待办一键变成任务，状态变化自动同步到看板与周报。',
      span: 'lg:col-span-3',
    },
    {
      icon: 'Sparkles',
      title: 'AI 摘要与检索',
      desc: '基于团队知识库回答问题，给出引用来源，减少"这个决定在哪"的追问。',
      span: 'lg:col-span-2',
    },
    {
      icon: 'Workflow',
      title: '无代码自动化',
      desc: '用触发器和条件拼出审批、提醒与归档流程，不用等排期。',
      span: 'lg:col-span-2',
    },
    {
      icon: 'ShieldCheck',
      title: '权限到段落',
      desc: '空间、文档、段落三级权限，外部协作者只看该看的部分。',
      span: 'lg:col-span-2',
    },
  ],

  steps: [
    { no: '01', title: '导入现有内容', desc: '从 Notion、Confluence 或 Markdown 批量迁移，结构保持不变。' },
    { no: '02', title: '连接团队流程', desc: '选一个模板开始：产品需求、周报、客户反馈或 OKR。' },
    { no: '03', title: '让知识自己长大', desc: '沉淀的文档会被检索、引用和复用，而不是躺在文件夹里。' },
  ],

  testimonials: [
    {
      quote: '以前找一个季度前的决策要翻三个工具，现在在 Nova 里搜一句话就有出处。',
      name: '林晚',
      role: '产品负责人 · 某 SaaS 公司',
      initials: 'LW',
    },
    {
      quote: '自动化把每周的版本同步从两小时压到十分钟，团队终于不用手工对齐。',
      name: '周野',
      role: '研发经理 · 跨境电商',
      initials: 'ZY',
    },
    {
      quote: '权限粒度足够细，外部供应商只看得到交付文档，法务审核一次就过了。',
      name: '陈一',
      role: '运营总监 · 咨询行业',
      initials: 'CY',
    },
  ],

  pricing: {
    note: '所有价格均为示例，接入真实支付前请替换为你的定价。',
    plans: [
      {
        name: 'Free',
        price: 0,
        yearlyPrice: 0,
        desc: '适合个人与小团队试水',
        cta: '免费开始',
        features: ['最多 5 名成员', '10 个文档空间', '基础检索', '社区支持'],
      },
      {
        name: 'Pro',
        price: 68,
        yearlyPrice: 54,
        desc: '适合成长期团队',
        cta: '开始 14 天试用',
        highlight: true,
        features: ['成员不限', '无限文档与任务', 'AI 摘要与检索', '无代码自动化', '优先支持'],
      },
      {
        name: 'Enterprise',
        price: null,
        yearlyPrice: null,
        desc: '适合有合规要求的组织',
        cta: '联系销售',
        features: ['SSO / SCIM', '审计日志与数据驻留', '专属客户成功', 'SLA 99.99%', '私有化部署'],
      },
    ],
  },

  faq: [
    {
      q: '可以从 Notion / Confluence 迁移吗？',
      a: '可以。支持导入 Markdown、HTML 与常见导出包，保留目录层级、图片与评论；迁移过程可分批进行，不影响现有使用。',
    },
    {
      q: 'AI 检索会使用我们的数据训练模型吗？',
      a: '不会。团队数据默认不用于训练，AI 回答只基于你有权限访问的内容，并附带引用来源；企业版可选择关闭 AI 功能。',
    },
    {
      q: '支持私有化部署吗？',
      a: '企业版支持 Docker / Kubernetes 私有化部署，提供离线镜像与升级包；需要单独评估服务器与运维方案。',
    },
    {
      q: '按什么维度计费？',
      a: '按"成员数 × 计费周期"计费，文档、任务与访客不额外收费；年付相比月付约 8 折。',
    },
  ],

  cta: {
    title: '把下一个项目，放进同一个空间',
    desc: '14 天试用，不需要信用卡。可以先导入一个团队的真实文档试试。',
    primary: { label: '免费开始', to: '/pricing' },
    secondary: { label: '和团队聊聊', to: '/contact' },
  },

  about: {
    title: '我们相信，协作工具应该减少噪音',
    desc: 'Nova 由一群做过协作工具的产品与工程师组成。我们厌倦了在聊天、文档、看板之间来回跳转，于是做了一个把上下文放在一起的地方。',
    values: [
      { title: '上下文优先', desc: '任何一条信息都应该能追溯到它的来处，而不是孤立的碎片。' },
      { title: '克制的设计', desc: '默认安静，需要时才出现。动效服务于理解，而不是炫技。' },
      { title: '可迁移', desc: '数据随时可以完整导出，不用被工具绑架。' },
    ],
    milestones: [
      { year: '2023', text: '团队成立，发布内测版' },
      { year: '2024', text: '开放注册，超过 3,000 个团队使用' },
      { year: '2025', text: '发布 AI 检索与自动化，完成 A 轮融资' },
    ],
  },
}
