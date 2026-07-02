<template>
  <el-container class="app-container">
    <!-- 侧边栏导航 -->
    <el-aside width="220px" class="app-aside">
      <div class="app-logo">
        <el-icon :size="24"><DataAnalysis /></el-icon>
        <span class="logo-text">DataSpec</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="app-menu"
        background-color="#1d1e2c"
        text-color="#a3a6b4"
        active-text-color="#409eff"
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

    <!-- 右侧内容区 -->
    <el-container>
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
          <el-tag v-if="authStore.operatorName" effect="plain" type="success">
            {{ authStore.operatorName }}
          </el-tag>
          <el-button size="small" @click="authStore.openLoginDialog">
            <el-icon><Lock /></el-icon>
            <span>API Token</span>
          </el-button>
          <el-button v-if="authStore.hasToken" size="small" text @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
          </el-button>
          <span class="project-label">当前项目：</span>
          <el-select
            v-model="projectStore.currentProjectId"
            placeholder="请选择项目"
            :loading="projectStore.loading"
            style="width: 200px"
            @change="handleProjectChange"
          >
            <el-option label="（未选择）" :value="null" />
            <el-option
              v-for="project in projectStore.projects"
              :key="project.id"
              :label="project.name"
              :value="project.id"
            />
          </el-select>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>

    <el-dialog v-model="authStore.loginDialogVisible" title="API Token" width="420px">
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
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { useAuthStore } from '@/stores/auth'
import { AUTH_CLEARED_EVENT } from '@/api/authStorage'
import { readPositiveIntQuery, replaceRouteQuery } from '@/utils/urlState'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()
const authStore = useAuthStore()
const tokenInput = ref('')

// 当前激活的菜单项，与路由路径同步
const activeMenu = computed(() => route.path)
const routeTitle = computed(() => String(route.meta.title || ''))

onMounted(async () => {
  authStore.restore()
  await projectStore.loadProjects()
  applyRouteProjectId()
  window.addEventListener(AUTH_CLEARED_EVENT, authStore.handleAuthCleared)
})

onBeforeUnmount(() => {
  window.removeEventListener(AUTH_CLEARED_EVENT, authStore.handleAuthCleared)
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
}

.app-aside {
  background-color: #1d1e2c;
  overflow-y: auto;
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
  letter-spacing: 1px;
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
}

.project-label {
  font-size: 14px;
  color: #606266;
}

.app-main {
  background-color: #f5f7fa;
  padding: 20px;
}
</style>
