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
          <el-menu-item index="/ai-export">
            <el-icon><Cpu /></el-icon>
            <span>AI 规则导出</span>
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
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- 右侧内容区 -->
    <el-container>
      <el-header class="app-header">
        <div class="header-right">
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
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useProjectStore } from '@/stores/project'

const route = useRoute()
const projectStore = useProjectStore()

// 当前激活的菜单项，与路由路径同步
const activeMenu = computed(() => route.path)

onMounted(() => {
  projectStore.loadProjects()
})

// 切换项目
const handleProjectChange = (val: number | null) => {
  projectStore.setCurrentProjectById(val)
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
  justify-content: flex-end;
  padding: 0 20px;
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
