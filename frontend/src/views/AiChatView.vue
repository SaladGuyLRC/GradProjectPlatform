<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { CircleCheck, Close, Promotion, RefreshRight } from '@element-plus/icons-vue'
import { api, type ApiResponse } from '../api'

interface Citation { documentId: string; title: string; originalFilename: string; chunkIndex: number; pageStart: number; pageEnd: number }
interface Action { type: string; entityId: string; summary: string }
interface TaskDraft {
  draftId: string
  status: 'PENDING_CONFIRMATION'|'CONFIRMED'|'CANCELLED'|'EXPIRED'
  title: string
  description?: string
  assigneeName: string
  type: string
  priority: string
  deadlineAt: string
  deadlineDisplay: string
  deadlineWasDefaulted: boolean
  expiresAt: string
  taskId?: string
  pendingAction?: 'confirm'|'cancel'
  error?: string
}
interface ChatResponse {
  conversationId: string
  answer: string
  citations: Citation[]
  actions: Action[]
  taskDraft?: TaskDraft
  issues: Array<{ field?: string; code: string; message: string }>
}
interface ChatMessage {
  role: 'user'|'assistant'
  content: string
  citations?: Citation[]
  actions?: Action[]
  taskDraft?: TaskDraft
  error?: boolean
}

const conversationId = ref(localStorage.getItem('ph_conversation_id') || crypto.randomUUID())
const input = ref('')
const loading = ref(false)
const messages = ref<ChatMessage[]>([])
const stream = ref<HTMLElement | null>(null)

onMounted(() => localStorage.setItem('ph_conversation_id', conversationId.value))

function newConversation() {
  conversationId.value = crypto.randomUUID()
  localStorage.setItem('ph_conversation_id', conversationId.value)
  messages.value = []
}

async function send() {
  // 先把输入追加到本地消息并立即清空输入框；只有后端成功才追加 AI 回复。
  const text = input.value.trim()
  if (!text || loading.value) return
  messages.value.push({ role: 'user', content: text })
  input.value = ''
  loading.value = true
  await scrollToLatest()
  try {
    const { data } = await api.post<ApiResponse<ChatResponse>>('/ai/chat',
      { message: text, conversationId: conversationId.value }, { timeout: 60000 })
    conversationId.value = data.data.conversationId
    localStorage.setItem('ph_conversation_id', conversationId.value)
    messages.value.push({ role: 'assistant', content: data.data.answer, citations: data.data.citations,
      actions: data.data.actions, taskDraft: data.data.taskDraft })
  } catch {
    messages.value.push({ role: 'assistant', content: 'Send failed. Check the AI configuration or try again later.', error: true })
  } finally {
    loading.value = false
    await scrollToLatest()
  }
}

async function confirmDraft(draft: TaskDraft) {
  // 草稿确认由用户显式触发，后端会再次校验权限、时间和幂等性。
  if (draft.pendingAction || draft.status !== 'PENDING_CONFIRMATION') return
  draft.pendingAction = 'confirm'
  draft.error = undefined
  try {
    const { data } = await api.post<ApiResponse<{ status: 'CONFIRMED'; task: { id: string } }>>(
      `/ai/task-drafts/${draft.draftId}/confirm`)
    draft.status = data.data.status
    draft.taskId = data.data.task.id
  } catch (error) {
    applyDraftError(draft, error)
  } finally {
    draft.pendingAction = undefined
  }
}

async function cancelDraft(draft: TaskDraft) {
  // 取消只改变临时草稿状态，不会创建或删除真实任务。
  if (draft.pendingAction || draft.status !== 'PENDING_CONFIRMATION') return
  draft.pendingAction = 'cancel'
  draft.error = undefined
  try {
    await api.post<ApiResponse<{ status: 'CANCELLED'; draftId: string }>>(
      `/ai/task-drafts/${draft.draftId}/cancel`)
    draft.status = 'CANCELLED'
  } catch (error) {
    applyDraftError(draft, error)
  } finally {
    draft.pendingAction = undefined
  }
}

function applyDraftError(draft: TaskDraft, error: unknown) {
  const response = (error as { response?: { data?: { code?: string; message?: string } } }).response?.data
  draft.error = response?.message || 'The request could not be completed. No task was created.'
  if (response?.code === 'DRAFT_EXPIRED') draft.status = 'EXPIRED'
  if (response?.code === 'DRAFT_CANCELLED') draft.status = 'CANCELLED'
}

async function scrollToLatest() {
  await nextTick()
  stream.value?.scrollTo({ top: stream.value.scrollHeight, behavior: 'smooth' })
}

function label(value: string) {
  return value.charAt(0) + value.slice(1).toLowerCase().replaceAll('_', ' ')
}
</script>

<template>
  <div class="chat-page">
    <header class="page-header">
      <div><h1>AI assistant</h1><p>Ask about projects, weekly reports, tasks and the public knowledge base</p></div>
      <el-button :icon="RefreshRight" @click="newConversation">New conversation</el-button>
    </header>
    <section class="chat-shell surface">
      <div ref="stream" class="chat-stream">
        <div v-if="!messages.length" class="chat-welcome">
          <span class="ai-mark">AI</span><h2>How can I help?</h2>
          <p>Ask “What are my recent tasks?”, “What is my graduation project?” or search for thesis guidelines.</p>
          <div class="suggestions">
            <button @click="input='What are my recent tasks?'; send()">What are my recent tasks?</button>
            <button @click="input='What is my graduation project?'; send()">What is my graduation project?</button>
            <button @click="input='Search the thesis formatting requirements'; send()">Search thesis requirements</button>
          </div>
        </div>
        <article v-for="(m, i) in messages" :key="i" :class="['message', m.role, m.error ? 'error' : '']">
          <div class="message-role">{{ m.role === 'user' ? 'You' : 'AI' }}</div>
          <div class="bubble">
            <p>{{ m.content }}</p>
            <div v-if="m.taskDraft" class="task-draft">
              <div class="draft-heading">
                <strong>{{ m.taskDraft.status === 'CONFIRMED' ? 'Task created' : m.taskDraft.status === 'CANCELLED' ? 'Task draft cancelled' : m.taskDraft.status === 'EXPIRED' ? 'Task draft expired' : 'Task draft' }}</strong>
                <span :class="['draft-status', m.taskDraft.status.toLowerCase()]">{{ label(m.taskDraft.status) }}</span>
              </div>
              <dl>
                <div><dt>Title</dt><dd>{{ m.taskDraft.title }}</dd></div>
                <div><dt>Assignee</dt><dd>{{ m.taskDraft.assigneeName }}</dd></div>
                <div><dt>Type</dt><dd>{{ label(m.taskDraft.type) }}</dd></div>
                <div><dt>Priority</dt><dd>{{ label(m.taskDraft.priority) }}</dd></div>
                <div><dt>Deadline</dt><dd>{{ m.taskDraft.deadlineDisplay }}</dd></div>
                <div v-if="m.taskDraft.description"><dt>Description</dt><dd>{{ m.taskDraft.description }}</dd></div>
              </dl>
              <p v-if="m.taskDraft.deadlineWasDefaulted && m.taskDraft.status === 'PENDING_CONFIRMATION'" class="draft-note">No deadline was provided, so it was set to 24 hours from now.</p>
              <p v-if="m.taskDraft.error" class="draft-error">{{ m.taskDraft.error }}</p>
              <div v-if="m.taskDraft.status === 'PENDING_CONFIRMATION'" class="draft-actions">
                <el-button :icon="Close" :loading="m.taskDraft.pendingAction === 'cancel'" :disabled="!!m.taskDraft.pendingAction" @click="cancelDraft(m.taskDraft)">Cancel</el-button>
                <el-button type="primary" :icon="CircleCheck" :loading="m.taskDraft.pendingAction === 'confirm'" :disabled="!!m.taskDraft.pendingAction" @click="confirmDraft(m.taskDraft)">Confirm</el-button>
              </div>
            </div>
            <div v-if="m.citations?.length" class="evidence"><strong>Sources</strong><span v-for="c in m.citations" :key="`${c.documentId}-${c.chunkIndex}`">{{ c.originalFilename || c.title }} · {{ c.pageStart > 0 ? (c.pageStart === c.pageEnd ? `Page ${c.pageStart}` : `Pages ${c.pageStart}-${c.pageEnd}`) : 'Page unavailable' }}</span></div>
            <div v-if="m.actions?.length" class="evidence actions"><strong>Actions</strong><span v-for="a in m.actions" :key="a.entityId">{{ a.type }}: {{ a.summary }}</span></div>
          </div>
        </article>
        <article v-if="loading" class="message assistant"><div class="message-role">AI</div><div class="bubble typing"><i></i><i></i><i></i></div></article>
      </div>
      <div class="composer">
        <el-input v-model="input" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }" resize="none" placeholder="Ask a question..." @keydown.enter.exact.prevent="send" />
        <el-button type="primary" :icon="Promotion" circle :loading="loading" :disabled="!input.trim()" title="Send" @click="send" />
      </div>
      <small class="conversation-label">Conversation ID: {{ conversationId }}</small>
    </section>
  </div>
</template>

<style scoped>
.chat-shell { display:flex; flex-direction:column; height:calc(100vh - 138px); min-height:530px; padding:0; overflow:hidden; }
.chat-stream { flex:1; overflow:auto; padding:28px clamp(18px,5vw,70px); }
.chat-welcome { max-width:620px; margin:60px auto; text-align:center; }
.ai-mark { display:grid; place-items:center; width:48px; height:48px; margin:0 auto; color:white; background:var(--green); border-radius:7px; font-weight:800; }
.chat-welcome h2 { margin:20px 0 8px; }
.chat-welcome p { color:var(--muted); line-height:1.7; }
.suggestions { display:flex; flex-wrap:wrap; justify-content:center; gap:8px; margin-top:24px; }
.suggestions button { padding:9px 13px; border:1px solid var(--line); border-radius:6px; color:#33453f; background:white; cursor:pointer; }
.suggestions button:hover { border-color:var(--green); color:var(--green); }
.message { display:grid; grid-template-columns:34px minmax(0,1fr); gap:11px; max-width:780px; margin:0 auto 22px; }
.message.user { direction:rtl; }
.message.user>* { direction:ltr; }
.message-role { width:34px; height:34px; display:grid; place-items:center; border-radius:6px; color:white; background:#64736e; font-size:11px; font-weight:700; }
.assistant .message-role { background:var(--green); }
.bubble { width:fit-content; max-width:90%; padding:12px 15px; border:1px solid var(--line); border-radius:7px; background:white; }
.user .bubble { margin-left:auto; color:white; background:#23493d; border-color:#23493d; }
.bubble>p { margin:0; line-height:1.7; white-space:pre-wrap; }
.message.error .bubble { color:#8f241d; background:#fff3f2; border-color:#f3cbc7; }
.task-draft { min-width:min(430px, 70vw); margin-top:13px; padding-top:13px; border-top:1px solid var(--line); }
.draft-heading { display:flex; align-items:center; justify-content:space-between; gap:14px; }
.draft-status { padding:3px 7px; border-radius:5px; color:#44534e; background:#edf1ef; font-size:11px; font-weight:700; }
.draft-status.confirmed { color:#12603f; background:#e5f4ec; }
.draft-status.cancelled,.draft-status.expired { color:#78413d; background:#faecea; }
.task-draft dl { display:grid; gap:8px; margin:13px 0 0; }
.task-draft dl div { display:grid; grid-template-columns:90px minmax(0, 1fr); gap:10px; }
.task-draft dt { color:var(--muted); font-size:12px; }
.task-draft dd { margin:0; overflow-wrap:anywhere; font-size:13px; }
.draft-note,.draft-error { margin-top:11px!important; padding:8px 10px; border-radius:5px; font-size:12px; line-height:1.5!important; }
.draft-note { color:#70591d; background:#fff8df; }
.draft-error { color:#8f241d; background:#fff0ee; }
.draft-actions { display:flex; justify-content:flex-end; gap:8px; margin-top:14px; }
.evidence { display:grid; gap:5px; margin-top:12px; padding-top:10px; border-top:1px solid #dce4e0; font-size:12px; color:var(--muted); }
.actions { color:#176b4d; }
.typing i { display:inline-block; width:6px; height:6px; margin:0 2px; border-radius:50%; background:#8a9994; animation:pulse 1s infinite alternate; }
.typing i:nth-child(2){animation-delay:.2s}.typing i:nth-child(3){animation-delay:.4s}
@keyframes pulse{to{opacity:.25;transform:translateY(-2px)}}
.composer { display:flex; align-items:flex-end; gap:10px; padding:15px 18px 8px; border-top:1px solid var(--line); background:#fafcfb; }
.composer .el-button { flex:0 0 auto; width:42px; height:42px; }
.conversation-label { display:block; padding:0 20px 11px; color:#9aa6a2; background:#fafcfb; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.chat-page .page-header { margin-bottom:16px; }
@media(max-width:800px){.chat-shell{height:calc(100vh - 150px)}.chat-stream{padding:20px 12px}.chat-welcome{margin:30px auto}.bubble{max-width:96%}.task-draft{min-width:0}.task-draft dl div{grid-template-columns:78px minmax(0,1fr)}}
</style>
