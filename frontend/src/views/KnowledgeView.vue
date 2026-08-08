<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Download, Plus, Refresh, RefreshRight } from '@element-plus/icons-vue'
import { api, type ApiResponse } from '../api'
import { useAuthStore } from '../stores/auth'

interface Document { id: string; title: string; description?: string; originalFilename: string; fileSize: number; status: string; chunkCount: number; failureReason?: string; createdAt: string }
const auth = useAuthStore(), isAdmin = computed(() => auth.user?.role === 'ADMIN')
const docs = ref<Document[]>([]), loading = ref(false), dialog = ref(false), file = ref<File | null>(null), uploading = ref(false)
const form = reactive({ title: '', description: '' })
async function load() { loading.value = true; try { docs.value = (await api.get<ApiResponse<Document[]>>('/knowledge/documents')).data.data } finally { loading.value = false } }
onMounted(load)
function choose(uploadFile: any) { file.value = uploadFile.raw; if (!form.title) form.title = uploadFile.name.replace(/\.pdf$/i, '') }
async function upload() { if (!file.value) return; uploading.value = true; try { const body = new FormData(); body.append('title', form.title); body.append('description', form.description); body.append('file', file.value); await api.post('/admin/knowledge/documents', body); ElMessage.success('文档已上传，正在异步建立索引'); dialog.value = false; Object.assign(form, { title:'', description:'' }); file.value = null; await load() } finally { uploading.value = false } }
async function reindex(doc: Document) { await api.post(`/admin/knowledge/documents/${doc.id}/reindex`); ElMessage.success('已开始重新索引'); await load() }
async function remove(doc: Document) { await ElMessageBox.confirm(`确认删除“${doc.title}”及其索引？`, '删除文档', { type:'warning' }); await api.delete(`/admin/knowledge/documents/${doc.id}`); ElMessage.success('文档已删除'); await load() }
async function download(doc: Document) { const response = await api.get(`/knowledge/documents/${doc.id}/download`, { responseType:'blob' }); const url = URL.createObjectURL(response.data); const link = window.document.createElement('a'); link.href=url; link.download=doc.originalFilename; link.click(); URL.revokeObjectURL(url) }
const size = (n:number) => n < 1024*1024 ? `${(n/1024).toFixed(1)} KB` : `${(n/1024/1024).toFixed(1)} MB`
const tagType = (s:string) => s === 'INDEXED' ? 'success' : s === 'FAILED' ? 'danger' : 'warning'
</script>

<template>
  <div><header class="page-header"><div><h1>Knowledge base</h1><p>Public graduation project references and PDF documents</p></div><el-button v-if="isAdmin" type="primary" :icon="Plus" @click="dialog=true">Upload PDF</el-button></header><section class="surface"><div class="toolbar"><el-button :icon="Refresh" circle title="Refresh" @click="load" /><span class="muted">{{ docs.length }} documents</span></div><el-table v-loading="loading" :data="docs" empty-text="No documents"><el-table-column label="Document" min-width="230"><template #default="{ row }"><strong>{{ row.title }}</strong><div class="doc-meta">{{ row.originalFilename }} · {{ size(row.fileSize) }}</div></template></el-table-column><el-table-column prop="description" label="Description" min-width="200" show-overflow-tooltip /><el-table-column label="Status" width="125"><template #default="{ row }"><el-tag :type="tagType(row.status)" size="small">{{ row.status }}</el-tag><div v-if="row.failureReason" class="failure">{{ row.failureReason }}</div></template></el-table-column><el-table-column prop="chunkCount" label="Chunks" width="80" /><el-table-column label="Actions" width="150" fixed="right"><template #default="{ row }"><el-button :icon="Download" text circle title="Download" @click="download(row)" /><template v-if="isAdmin"><el-button :icon="RefreshRight" text circle title="Reindex" @click="reindex(row)" /><el-button :icon="Delete" text circle type="danger" title="Delete" @click="remove(row)" /></template></template></el-table-column></el-table></section>
    <el-dialog v-model="dialog" title="Upload knowledge base PDF" width="540px"><el-form label-position="top"><el-form-item label="Title" required><el-input v-model="form.title" /></el-form-item><el-form-item label="Description"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item><el-form-item label="PDF file" required><el-upload :auto-upload="false" :limit="1" accept="application/pdf,.pdf" :on-change="choose"><el-button>Select file</el-button><template #tip><div class="el-upload__tip">PDF only, maximum 20 MB</div></template></el-upload></el-form-item></el-form><template #footer><el-button @click="dialog=false">Cancel</el-button><el-button type="primary" :loading="uploading" :disabled="!form.title || !file" @click="upload">Upload and index</el-button></template></el-dialog>
  </div>
</template>

<style scoped>
.doc-meta,.failure { margin-top:5px; color:var(--muted); font-size:12px; }.failure { color:#b42318; max-width:120px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
</style>
