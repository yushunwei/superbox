import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/modules/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/apps/auth/LoginPage.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      name: 'Home',
      component: () => import('@/components/home/HomePage.vue'),
      meta: { requiresAuth: true },
    },
    // App routes wrapped in AppShell
    {
      path: '/app',
      component: () => import('@/components/shell/AppShell.vue'),
      meta: { requiresAuth: true },
      children: [
        // Task Manager
        {
          path: 'task-manager',
          name: 'TaskList',
          component: () => import('@/apps/taskManager/pages/TaskListPage.vue'),
          meta: { title: '任务管理' },
        },
        {
          path: 'task-manager/:id',
          name: 'TaskDetail',
          component: () => import('@/apps/taskManager/pages/TaskDetailPage.vue'),
          meta: { title: '任务详情' },
        },
        // Legacy redirects
        { path: 'tasks', redirect: '/app/task-manager' },
        { path: 'tasks/:id', redirect: to => `/app/task-manager/${to.params.id}` },
        // Knowledge Base
        {
          path: 'knowledge-base',
          name: 'KnowledgeList',
          component: () => import('@/apps/knowledgeBase/pages/KnowledgeListPage.vue'),
          meta: { title: '知识库' },
        },
        {
          path: 'knowledge-base/upload',
          name: 'KnowledgeUpload',
          component: () => import('@/apps/knowledgeBase/pages/KnowledgeUploadPage.vue'),
          meta: { title: '知识库 - 上传' },
        },
        {
          path: 'knowledge-base/:id',
          name: 'KnowledgeDetail',
          component: () => import('@/apps/knowledgeBase/pages/KnowledgeDetailPage.vue'),
          meta: { title: '知识详情' },
        },
        // Legacy redirects
        { path: 'knowledge', redirect: '/app/knowledge-base' },
        { path: 'knowledge/upload', redirect: '/app/knowledge-base/upload' },
        { path: 'knowledge/:id', redirect: to => `/app/knowledge-base/${to.params.id}` },
        // AI Chat
        {
          path: 'ai-chat',
          name: 'AIChat',
          component: () => import('@/apps/aiChat/pages/AIChatPage.vue'),
          meta: { title: 'AI 问答' },
        },
        {
          path: 'ai-chat/:conversationId',
          name: 'AIChatConversation',
          component: () => import('@/apps/aiChat/pages/AIChatPage.vue'),
          meta: { title: 'AI 问答' },
        },
        // Legacy redirects
        { path: 'chat', redirect: '/app/ai-chat' },
        { path: 'chat/:conversationId', redirect: to => `/app/ai-chat/${to.params.conversationId}` },
        // Tag Manager
        {
          path: 'tag-manager',
          name: 'TagManager',
          component: () => import('@/apps/tagManager/pages/TagManagerPage.vue'),
          meta: { title: '标签管理' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()

  if (to.meta.public) {
    next()
    return
  }

  if (to.meta.requiresAuth) {
    if (!userStore.token) {
      next('/login')
      return
    }
    if (!userStore.user) {
      try {
        await userStore.fetchUser()
      } catch {
        userStore.logout()
        next('/login')
        return
      }
    }
  }

  next()
})

export default router
