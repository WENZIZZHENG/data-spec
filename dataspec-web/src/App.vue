<template>
  <a
    class="skip-link"
    href="#main-content"
    :aria-hidden="isMobile && mobileNavOpen ? 'true' : undefined"
    :inert="isMobile && mobileNavOpen ? true : undefined"
  >跳到主内容</a>
  <el-container class="app-container">
    <!-- 侧边栏导航 -->
    <el-aside
      id="primary-navigation"
      width="220px"
      class="app-aside"
      :class="{ 'mobile-open': mobileNavOpen }"
      role="navigation"
      aria-label="主导航"
      :aria-hidden="isMobile && !mobileNavOpen ? 'true' : undefined"
      :inert="isMobile && !mobileNavOpen ? true : undefined"
      @keydown.tab="trapMobileNavigationFocus"
    >
      <div class="app-logo">
        <el-icon :size="24"><DataAnalysis /></el-icon>
        <span class="logo-text">DataSpec</span>
        <el-button
          id="mobile-nav-close"
          class="mobile-nav-close"
          text
          aria-label="关闭主导航"
          title="关闭主导航"
          @click="closeMobileNavigation"
        >
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="app-menu"
        background-color="#1d1e2c"
        text-color="#a3a6b4"
        active-text-color="#409eff"
        @select="handleMobileMenuSelect"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>工作台</span>
        </el-menu-item>

        <!-- 基础数据 -->
        <el-sub-menu index="basic">
          <template #title>
            <el-icon><Files /></el-icon>
            <span>基础数据</span>
          </template>
          <el-menu-item index="/projects">
            <el-icon><Folder /></el-icon>
            <span>项目列表</span>
          </el-menu-item>
          <el-menu-item index="/fields">
            <el-icon><List /></el-icon>
            <span>标准字段库</span>
          </el-menu-item>
          <el-menu-item index="/business-glossary">
            <el-icon><Collection /></el-icon>
            <span>业务术语表</span>
          </el-menu-item>
          <el-menu-item index="/usage-examples">
            <el-icon><Collection /></el-icon>
            <span>示例与反例库</span>
          </el-menu-item>
          <el-menu-item index="/standard-qa">
            <el-icon><Search /></el-icon>
            <span>标准问答</span>
          </el-menu-item>
          <el-menu-item index="/standard-candidates">
            <el-icon><Collection /></el-icon>
            <span>标准候选</span>
          </el-menu-item>
          <el-menu-item index="/field-quality">
            <el-icon><TrendCharts /></el-icon>
            <span>字段质量</span>
          </el-menu-item>
          <el-menu-item index="/standard-health">
            <el-icon><TrendCharts /></el-icon>
            <span>标准健康</span>
          </el-menu-item>
          <el-menu-item index="/field-conflicts">
            <el-icon><Warning /></el-icon>
            <span>字段冲突</span>
          </el-menu-item>
          <el-menu-item index="/domains">
            <el-icon><Grid /></el-icon>
            <span>数据域管理</span>
          </el-menu-item>
          <el-menu-item index="/enums">
            <el-icon><Collection /></el-icon>
            <span>枚举字典</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- 模板与规则 -->
        <el-sub-menu index="template">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>模板与规则</span>
          </template>
          <el-menu-item index="/templates">
            <el-icon><Notebook /></el-icon>
            <span>表模板</span>
          </el-menu-item>
          <el-menu-item index="/rules">
            <el-icon><Setting /></el-icon>
            <span>规则配置</span>
          </el-menu-item>
          <el-menu-item index="/rule-exemptions">
            <el-icon><Warning /></el-icon>
            <span>规则例外</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- 校验与生成 -->
        <el-sub-menu index="tools">
          <template #title>
            <el-icon><Tools /></el-icon>
            <span>校验与生成</span>
          </template>
          <el-menu-item index="/sql-lint">
            <el-icon><Edit /></el-icon>
            <span>SQL 校验</span>
          </el-menu-item>
          <el-menu-item index="/generator">
            <el-icon><MagicStick /></el-icon>
            <span>生成器</span>
          </el-menu-item>
          <el-menu-item index="/requirement-draft">
            <el-icon><MagicStick /></el-icon>
            <span>需求草案</span>
          </el-menu-item>
          <el-menu-item index="/ai-export">
            <el-icon><Cpu /></el-icon>
            <span>AI 规则导出</span>
          </el-menu-item>
          <el-menu-item index="/ai-profiles">
            <el-icon><Operation /></el-icon>
            <span>AI 任务模式</span>
          </el-menu-item>
          <el-menu-item index="/ai-replay">
            <el-icon><Clock /></el-icon>
            <span>AI 回放</span>
          </el-menu-item>
          <el-menu-item index="/ai-batches">
            <el-icon><DataAnalysis /></el-icon>
            <span>AI 批量任务</span>
          </el-menu-item>
          <el-menu-item index="/ai-handoff">
            <el-icon><DocumentChecked /></el-icon>
            <span>AI 交接证据</span>
          </el-menu-item>
          <el-menu-item index="/ai-feedback">
            <el-icon><DataAnalysis /></el-icon>
            <span>AI 反馈</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- 数据管理 -->
        <el-sub-menu index="data">
          <template #title>
            <el-icon><FolderOpened /></el-icon>
            <span>数据管理</span>
          </template>
          <el-menu-item index="/import-export">
            <el-icon><Download /></el-icon>
            <span>导入导出</span>
          </el-menu-item>
          <el-menu-item index="/project-backup">
            <el-icon><Files /></el-icon>
            <span>项目备份</span>
          </el-menu-item>
          <el-menu-item index="/standard-reuse-packs">
            <el-icon><Files /></el-icon>
            <span>标准复用包</span>
          </el-menu-item>
          <el-menu-item index="/reverse-import">
            <el-icon><Search /></el-icon>
            <span>反向导入</span>
          </el-menu-item>
          <el-menu-item index="/field-coverage">
            <el-icon><DataAnalysis /></el-icon>
            <span>覆盖率报告</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- 系统设置 -->
        <el-sub-menu index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </template>
          <el-menu-item index="/tokens">
            <el-icon><Key /></el-icon>
            <span>API Token</span>
          </el-menu-item>
          <el-menu-item index="/standard-snapshots">
            <el-icon><Clock /></el-icon>
            <span>标准快照</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <div
      v-if="isMobile && mobileNavOpen"
      class="mobile-nav-backdrop"
      aria-hidden="true"
      @click="closeMobileNavigation"
    />

    <!-- 右侧内容区 -->
    <el-container
      class="app-content"
      :aria-hidden="isMobile && mobileNavOpen ? 'true' : undefined"
      :inert="isMobile && mobileNavOpen ? true : undefined"
    >
      <el-header class="app-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">工作台</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.path !== '/dashboard'">
              {{ routeTitle }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-button
            id="mobile-menu-button"
            class="mobile-menu-button"
            aria-label="打开主导航"
            title="打开主导航"
            aria-controls="primary-navigation"
            :aria-expanded="mobileNavOpen"
            @click="openMobileNavigation"
          >
            <el-icon><Menu /></el-icon>
          </el-button>
          <el-button
            size="small"
            class="compact-header-action"
            aria-label="打开命令面板"
            aria-keyshortcuts="Control+K Meta+K"
            @click="openCommandPalette"
          >
            <el-icon><Search /></el-icon>
            <span class="header-action-label">命令面板</span>
          </el-button>
          <el-tag v-if="authStore.operatorName" effect="plain" type="success">
            {{ authStore.operatorName }}
          </el-tag>
          <el-button
            size="small"
            class="compact-header-action"
            aria-label="API Token"
            @click="openLoginDialog"
          >
            <el-icon><Lock /></el-icon>
            <span class="header-action-label">API Token</span>
          </el-button>
          <el-button
            v-if="authStore.hasToken"
            size="small"
            text
            aria-label="退出 API Token 登录"
            title="退出 API Token 登录"
            @click="handleLogout"
          >
            <el-icon><SwitchButton /></el-icon>
          </el-button>
          <span class="project-label">当前项目：</span>
          <el-select
            v-model="projectSelectValue"
            class="project-select"
            placeholder="请选择项目"
            :loading="projectStore.loading"
            aria-label="当前项目"
          >
            <el-option label="（未选择）" :value="0" />
            <el-option
              v-for="project in projectStore.projects"
              :key="project.id"
              :label="project.name"
              :value="project.id"
            />
          </el-select>
        </div>
      </el-header>
      <el-main
        id="main-content"
        class="app-main"
        role="main"
        tabindex="-1"
        :aria-label="routeTitle || '工作台'"
      >
        <router-view />
      </el-main>
    </el-container>

    <CommandPaletteDialog
      v-model="commandPaletteVisible"
      :project-id="projectStore.currentProjectId"
    />

    <el-dialog
      v-model="authStore.loginDialogVisible"
      title="API Token"
      width="420px"
      @closed="authDialogFocus.restoreFocus"
    >
      <el-form @submit.prevent="handleLogin">
        <el-form-item label="Token">
          <el-input
            v-model="tokenInput"
            type="password"
            show-password
            autocomplete="off"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="authStore.loginDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="authStore.loading" @click="handleLogin">登录</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, toRef, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { useAuthStore } from '@/stores/auth'
import { AUTH_CLEARED_EVENT } from '@/api/authStorage'
import { useDialogFocusReturn } from '@/composables/useDialogFocusReturn'
import { readPositiveIntQuery, replaceRouteQuery } from '@/utils/urlState'
import CommandPaletteDialog from '@/components/CommandPaletteDialog.vue'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()
const authStore = useAuthStore()
const tokenInput = ref('')
const commandPaletteVisible = ref(false)
const isMobile = ref(false)
const mobileNavOpen = ref(false)
const authDialogFocus = useDialogFocusReturn(toRef(authStore, 'loginDialogVisible'))
let mobileMediaQuery: MediaQueryList | null = null

// 当前激活的菜单项，与路由路径同步
const activeMenu = computed(() => route.path)
const routeTitle = computed(() => String(route.meta.title || ''))
// Element Plus option value 不接受 null；下拉层用 0 代表未选择，store 和 URL 仍保留 null 语义。
const projectSelectValue = computed({
  get: () => projectStore.currentProjectId ?? 0,
  set: (value: number) => handleProjectChange(value > 0 ? value : null)
})

onMounted(async () => {
  mobileMediaQuery = window.matchMedia('(max-width: 720px)')
  updateMobileViewport(mobileMediaQuery)
  mobileMediaQuery.addEventListener('change', updateMobileViewport)
  authStore.restore()
  await projectStore.loadProjects()
  applyRouteProjectId()
  window.addEventListener(AUTH_CLEARED_EVENT, authStore.handleAuthCleared)
  window.addEventListener('keydown', handleCommandPaletteShortcut)
})

onBeforeUnmount(() => {
  mobileMediaQuery?.removeEventListener('change', updateMobileViewport)
  window.removeEventListener(AUTH_CLEARED_EVENT, authStore.handleAuthCleared)
  window.removeEventListener('keydown', handleCommandPaletteShortcut)
})

// 切换项目
const handleProjectChange = (val: number | null) => {
  projectStore.setCurrentProjectById(val)
  void syncProjectQuery(projectStore.currentProjectId)
}

watch(
  () => route.query.projectId,
  () => applyRouteProjectId()
)

watch(
  () => route.path,
  () => {
    mobileNavOpen.value = false
    void focusMainContent()
  }
)

async function focusMainContent() {
  await nextTick()
  document.getElementById('main-content')?.focus({ preventScroll: true })
}

function applyRouteProjectId() {
  const routeProjectId = readPositiveIntQuery(route.query, 'projectId')
  if (!routeProjectId || projectStore.loading) {
    return
  }
  const project = projectStore.projects.find((item) => item.id === routeProjectId)
  if (project) {
    projectStore.setCurrentProject(project)
    return
  }
  if (projectStore.projects.length > 0) {
    ElMessage.warning('链接中的项目不可访问，请重新选择项目')
    void syncProjectQuery(projectStore.currentProjectId)
  }
}

async function syncProjectQuery(projectId: number | null) {
  await replaceRouteQuery(router, route, { projectId })
}

function openCommandPalette() {
  commandPaletteVisible.value = true
}

async function openMobileNavigation() {
  mobileNavOpen.value = true
  await nextTick()
  document.getElementById('mobile-nav-close')?.focus({ preventScroll: true })
}

async function closeMobileNavigation() {
  mobileNavOpen.value = false
  await nextTick()
  document.getElementById('mobile-menu-button')?.focus({ preventScroll: true })
}

function handleMobileMenuSelect(index: string) {
  if (!isMobile.value) {
    return
  }
  // 选择当前路由不会触发 route watcher，必须在这里恢复菜单按钮焦点。
  if (index === route.path) {
    void closeMobileNavigation()
  } else {
    mobileNavOpen.value = false
  }
}

function trapMobileNavigationFocus(event: KeyboardEvent) {
  if (!mobileNavOpen.value) {
    return
  }
  const navigation = document.getElementById('primary-navigation')
  if (!navigation) {
    return
  }
  // Element Plus 菜单项采用 roving tabindex；抽屉打开时手动循环，避免焦点落到遮罩后的页面。
  const focusable = Array.from(navigation.querySelectorAll<HTMLElement>(
    'button:not([disabled]), [role="menuitem"]'
  )).filter((element) => element.offsetParent !== null && element.getAttribute('aria-disabled') !== 'true')
  if (focusable.length === 0) {
    return
  }
  const currentIndex = focusable.indexOf(document.activeElement as HTMLElement)
  const nextIndex = event.shiftKey
    ? (currentIndex <= 0 ? focusable.length - 1 : currentIndex - 1)
    : (currentIndex < 0 || currentIndex === focusable.length - 1 ? 0 : currentIndex + 1)
  event.preventDefault()
  focusable[nextIndex]?.focus({ preventScroll: true })
}

function updateMobileViewport(media: MediaQueryList | MediaQueryListEvent) {
  isMobile.value = media.matches
  if (!media.matches) {
    mobileNavOpen.value = false
  }
}

function openLoginDialog() {
  authDialogFocus.rememberFocus()
  authStore.openLoginDialog()
}

function handleCommandPaletteShortcut(event: KeyboardEvent) {
  if (event.key === 'Escape' && mobileNavOpen.value) {
    event.preventDefault()
    void closeMobileNavigation()
    return
  }
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    openCommandPalette()
  }
}

const handleLogin = async () => {
  try {
    await authStore.login(tokenInput.value)
    tokenInput.value = ''
    ElMessage.success('已验证 API token')
    await projectStore.loadProjects()
  } catch {
    // request 拦截器已经展示错误消息，这里只阻止登录失败冒泡成未捕获异常。
  }
}

const handleLogout = () => {
  authStore.logout()
  projectStore.clearCurrentProject()
}
</script>

<style scoped>
.app-container {
  height: 100vh;
  width: 100%;
  max-width: 100vw;
  overflow: hidden;
}

.app-aside {
  background-color: #1d1e2c;
  overflow-y: auto;
}

.app-content {
  min-width: 0;
}

.app-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  border-bottom: 1px solid #2d2e3e;
}

.logo-text {
  letter-spacing: 0;
}

.mobile-menu-button,
.mobile-nav-close,
.mobile-nav-backdrop {
  display: none;
}

.app-menu {
  border-right: none;
}

.app-header {
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 20px;
}

.header-left {
  min-width: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.project-label {
  font-size: 14px;
  color: #606266;
}

.project-select {
  width: 200px;
}

.app-main {
  background-color: #f5f7fa;
  padding: 20px;
}

@media (max-width: 720px) {
  .app-aside {
    position: fixed;
    inset: 0 auto 0 0;
    z-index: 1100;
    width: min(280px, calc(100vw - 48px)) !important;
    transform: translateX(-100%);
    transition: transform 0.2s ease;
  }

  .app-aside.mobile-open {
    transform: translateX(0);
  }

  .app-logo {
    justify-content: flex-start;
    padding: 0 10px 0 18px;
  }

  .logo-text {
    flex: 1;
  }

  .mobile-nav-close,
  .mobile-menu-button {
    display: inline-flex;
    flex: 0 0 auto;
  }

  .mobile-nav-close {
    color: #fff;
  }

  .mobile-nav-backdrop {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 1090;
    width: 100%;
    height: 100%;
    border: 0;
    background: rgb(0 0 0 / 48%);
  }

  .app-header {
    gap: 8px;
    padding: 0 12px;
  }

  .header-left,
  .project-label,
  .header-action-label {
    display: none;
  }

  .header-right {
    flex: 1;
    justify-content: flex-end;
    gap: 6px;
  }

  .compact-header-action {
    flex: 0 0 auto;
    margin-left: 0;
  }

  .project-select {
    flex: 1 1 120px;
    width: auto;
    min-width: 96px;
    max-width: 160px;
  }

  .app-main {
    padding: 12px;
  }
}
</style>
