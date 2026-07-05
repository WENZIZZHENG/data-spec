<template>
  <el-dialog v-model="visible" width="760px" title="命令面板" class="command-palette-dialog" @open="handleOpen">
    <div class="command-palette">
      <el-input
        v-model="keyword"
        placeholder="搜索页面、最近记录或常用动作"
        clearable
        autofocus
        class="command-search"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-alert
        v-if="!projectId"
        type="warning"
        :closable="false"
        show-icon
        title="未选择项目，项目内命令会在选择项目后可用"
      />

      <el-empty v-if="filteredGroups.length === 0" description="没有匹配的命令" />
      <div v-else class="command-groups" v-loading="loading">
        <section v-for="group in filteredGroups" :key="group.name" class="command-group">
          <div class="group-title">{{ group.name }}</div>
          <button
            v-for="item in group.items"
            :key="item.id"
            type="button"
            class="command-item"
            :class="{ disabled: item.disabled }"
            @click="executeCommand(item)"
          >
            <span class="item-main">
              <span class="item-title">{{ item.title }}</span>
              <span class="item-description">{{ item.disabledReason || item.description || '-' }}</span>
            </span>
            <el-tag v-if="item.projectRequired" size="small" effect="plain">项目</el-tag>
          </button>
        </section>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { listAiJobs } from '@/api/aiJob'
import { listLintRecords } from '@/api/lint'
import { listReverseImportDecisions } from '@/api/reverseImport'
import {
  buildCommandPaletteItems,
  commandPaletteStorageKey,
  commandToLocalRecentEntry,
  filterCommandPaletteItems,
  groupLabel,
  readRecentCommandEntries,
  writeRecentCommandEntry
} from '@/utils/commandPalette'
import type { AiJobRecordListItem, ReverseImportDecision, SqlCheckRecord } from '@/types'
import type { CommandPaletteItem, RecentCommandEntry } from '@/utils/commandPalette'

const visible = defineModel<boolean>({ default: false })

const props = defineProps<{
  projectId?: number | null
}>()

const router = useRouter()
const keyword = ref('')
const loading = ref(false)
const lintRecords = shallowRef<SqlCheckRecord[]>([])
const reverseDecisions = shallowRef<ReverseImportDecision[]>([])
const aiJobs = shallowRef<AiJobRecordListItem[]>([])
const localRecentEntries = shallowRef<RecentCommandEntry[]>([])
const recentRequestSeq = ref(0)

const items = computed(() => buildCommandPaletteItems({
  projectId: props.projectId,
  lintRecords: lintRecords.value,
  reverseDecisions: reverseDecisions.value,
  aiJobs: aiJobs.value,
  localRecentEntries: localRecentEntries.value
}))
const filteredItems = computed(() => filterCommandPaletteItems(items.value, keyword.value))
const filteredGroups = computed(() => {
  const groups = new Map<string, CommandPaletteItem[]>()
  for (const item of filteredItems.value) {
    const label = groupLabel(item.group)
    groups.set(label, [...(groups.get(label) ?? []), item])
  }
  return Array.from(groups, ([name, groupItems]) => ({ name, items: groupItems }))
})

async function handleOpen() {
  keyword.value = ''
  localRecentEntries.value = readRecentCommandEntries()
  await loadRemoteRecent()
}

async function loadRemoteRecent() {
  const projectId = props.projectId
  const requestSeq = ++recentRequestSeq.value
  lintRecords.value = []
  reverseDecisions.value = []
  aiJobs.value = []
  if (!projectId) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    const [lintPage, decisions, aiPage] = await Promise.allSettled([
      listLintRecords(projectId, 1, 5),
      listReverseImportDecisions(projectId, undefined, 30),
      listAiJobs(projectId, 1, 5)
    ])
    if (recentRequestSeq.value !== requestSeq || props.projectId !== projectId) {
      return
    }
    lintRecords.value = lintPage.status === 'fulfilled' ? lintPage.value.records ?? [] : []
    reverseDecisions.value = decisions.status === 'fulfilled' ? decisions.value : []
    aiJobs.value = aiPage.status === 'fulfilled' ? aiPage.value.records ?? [] : []
  } finally {
    if (recentRequestSeq.value === requestSeq) {
      loading.value = false
    }
  }
}

async function executeCommand(item: CommandPaletteItem) {
  if (item.disabled) {
    ElMessage.warning(item.disabledReason || '当前命令不可用')
    return
  }
  writeRecentCommandEntry(undefined, {
    ...commandToLocalRecentEntry(item),
    title: item.title
  })
  localRecentEntries.value = readRecentCommandEntries()
  visible.value = false
  await router.push(item.route)
}

defineExpose({
  storageKey: commandPaletteStorageKey()
})
</script>

<style scoped>
.command-palette {
  display: grid;
  gap: 12px;
}

.command-search {
  width: 100%;
}

.command-groups {
  display: grid;
  gap: 14px;
  max-height: 58vh;
  overflow-y: auto;
}

.command-group {
  display: grid;
  gap: 6px;
}

.group-title {
  color: #6b7280;
  font-size: 12px;
  font-weight: 600;
}

.command-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  min-height: 52px;
  padding: 8px 10px;
  cursor: pointer;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  color: #1f2937;
  text-align: left;
  transition: background-color 0.16s ease, border-color 0.16s ease;
}

.command-item:hover {
  border-color: #409eff;
  background: #f8fbff;
}

.command-item.disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.item-main {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.item-title {
  font-size: 14px;
  font-weight: 600;
}

.item-description {
  overflow: hidden;
  color: #6b7280;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
