export interface AppManifest {
  id: string
  icon: string
  label: string
  route: string
  desc?: string
  statKey?: string
  wide?: boolean
}

export const apps: AppManifest[] = [
  {
    id: 'taskManager',
    icon: 'List',
    label: '任务管理',
    route: '/app/task-manager',
    desc: '跟踪和管理日常任务',
    statKey: 'pendingCount',
  },
  {
    id: 'knowledgeBase',
    icon: 'Collection',
    label: '知识库',
    route: '/app/knowledge-base',
    desc: '个人知识管理与检索',
    statKey: 'entryCount',
  },
  {
    id: 'aiChat',
    icon: 'ChatDotRound',
    label: 'AI 问答',
    route: '/app/ai-chat',
    desc: '基于知识库的智能对话 · 深度推理 · 多模型',
    wide: true,
  },
  {
    id: 'aiTranslate',
    icon: 'Document',
    label: 'AI 翻译',
    route: '/app/ai-translate',
    desc: 'AI 智能翻译 · 文本与文档 · 多模型多语言',
  },
]

export const configApps: AppManifest[] = [
  {
    id: 'tagManager',
    icon: 'PriceTag',
    label: '标签管理',
    route: '/app/tag-manager',
    desc: '管理全局跨应用的标签数据',
  },
  {
    id: 'modelManager',
    icon: 'Setting',
    label: 'AI 模型管理',
    route: '/app/model-manager',
    desc: '管理第三方 AI 模型配置',
  },
]
