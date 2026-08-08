<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { api, type ApiResponse } from '../api'

interface Node { id: string; name: string; studentNo?: string; majors?: Node[]; mentors?: Node[]; students?: Node[]; children?: Node[] }
const tree = ref<Node[]>([]), loading = ref(false), query = ref('')
function normalize(nodes: Node[], level = 0): Node[] {
  return nodes.map(node => ({ ...node, children: normalize(node.majors || node.mentors || node.students || [], level + 1) }))
}
async function load() { loading.value = true; try { tree.value = normalize((await api.get<ApiResponse<Node[]>>('/members/tree')).data.data) } finally { loading.value = false } }
onMounted(load)
const filterNode = (value: string, data: Node) => !value || data.name.toLowerCase().includes(value.toLowerCase()) || Boolean(data.studentNo?.includes(value))
</script>

<template>
  <div><header class="page-header"><div><h1>Members</h1><p>Colleges, majors, mentors and students</p></div></header><section class="surface"><div class="toolbar"><el-input v-model="query" placeholder="Search name or student number" clearable style="max-width:320px" @input="($refs.treeRef as any)?.filter(query)" /><el-button :icon="Refresh" circle title="Refresh" @click="load" /></div><el-tree ref="treeRef" v-loading="loading" :data="tree" node-key="id" :props="{ label: 'name', children: 'children' }" :filter-node-method="filterNode" default-expand-all class="member-tree"><template #default="{ data }"><span class="tree-node"><span>{{ data.name }}</span><small v-if="data.studentNo">{{ data.studentNo }}</small></span></template></el-tree></section></div>
</template>

<style scoped>
.member-tree { --el-tree-node-hover-bg-color: #f1f7f4; }.member-tree :deep(.el-tree-node__content) { height: 42px; border-bottom: 1px solid #f0f3f1; }.tree-node { display:flex; align-items:center; gap:10px; font-size:14px; }.tree-node small { color: var(--muted); font-family: ui-monospace,monospace; }
</style>
