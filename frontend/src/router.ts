import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from './components/AppLayout.vue'
import LoginView from './views/LoginView.vue'
import DashboardView from './views/DashboardView.vue'
import TasksView from './views/TasksView.vue'
import ReportsView from './views/ReportsView.vue'
import MembersView from './views/MembersView.vue'
import KnowledgeView from './views/KnowledgeView.vue'
import AiChatView from './views/AiChatView.vue'
import ProjectView from './views/ProjectView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView },
    {
      path: '/', component: AppLayout,
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', component: DashboardView },
        { path: 'tasks', component: TasksView },
        { path: 'reports', component: ReportsView },
        { path: 'project', component: ProjectView, meta: { roles: ['STUDENT', 'MENTOR'] } },
        { path: 'members', component: MembersView },
        { path: 'knowledge', component: KnowledgeView },
        { path: 'ai', component: AiChatView }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const loggedIn = Boolean(localStorage.getItem('ph_token'))
  if (to.path !== '/login' && !loggedIn) return '/login'
  if (to.path === '/login' && loggedIn) return '/dashboard'
  const roles = to.meta.roles as string[] | undefined
  const user = JSON.parse(localStorage.getItem('ph_user') || 'null')
  if (roles && !roles.includes(user?.role)) return '/dashboard'
})

export default router
