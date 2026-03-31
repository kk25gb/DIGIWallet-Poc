import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/',
      component: () => import('@/layout/index.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/index.vue'),
        },
        {
          path: 'did/register',
          name: 'DidRegister',
          component: () => import('@/views/did-register/index.vue'),
          meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] },
        },
        {
          path: 'settings',
          name: 'Settings',
          component: () => import('@/views/settings/index.vue'),
          meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] },
        },
        {
          path: 'credential-types',
          name: 'CredentialTypes',
          component: () => import('@/views/credential-types/index.vue'),
          meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] },
        },
        {
          path: 'credential-types/create',
          name: 'CredentialTypeCreate',
          component: () => import('@/views/credential-types/create.vue'),
          meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] },
        },
        {
          path: 'issue',
          name: 'IssueCredential',
          component: () => import('@/views/issue-credential/index.vue'),
          meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] },
        },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth !== false && !authStore.isAuthenticated) {
    next({ name: 'Login' })
  } else if (to.name === 'Login' && authStore.isAuthenticated) {
    next({ name: 'Dashboard' })
  } else if (to.meta.roles) {
    const requiredRoles = to.meta.roles as string[]
    const hasRole = requiredRoles.some((role) =>
      authStore.account?.authorities?.includes(role),
    )
    if (!hasRole) {
      next({ name: 'Dashboard' })
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
