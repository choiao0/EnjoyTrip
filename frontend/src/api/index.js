import axios from 'axios'

const api = axios.create({
  baseURL: '',
  withCredentials: true
})

export default api

export const authApi = {
  me: () => api.get('/api/auth/me'),
  login: (id, password) => api.post('/api/auth/login', { id, password }),
  logout: () => api.post('/api/auth/logout'),
  register: (id, name, password) => api.post('/api/auth/register', { id, name, password }),
  updateProfile: (name, currentPassword, newPassword) =>
    api.put('/api/auth/me', { name, currentPassword, newPassword }),
  deleteAccount: (currentPassword) => api.delete('/api/auth/me', { data: { currentPassword } }),
  findPassword: (id, name) => api.post('/api/auth/find-password', { id, name })
}

export const attractionApi = {
  sidos: () => api.get('/api/sidos'),
  guguns: (sidoCode) => api.get('/api/guguns', { params: { sidoCode } }),
  search: (params) => api.get('/api/attractions', { params })
}

export const planApi = {
  list: () => api.get('/api/plans'),
  save: (body) => api.post('/api/plans', body),
  delete: (id) => api.delete(`/api/plans/${id}`),
  recommend: (id) => api.post(`/api/plans/${id}/recommend`),
  getDraft: () => api.get('/api/plans/draft'),
  addDraftItem: (item) => api.post('/api/plans/draft/items', item),
  removeDraftItem: (index) => api.delete(`/api/plans/draft/items/${index}`),
  moveDraftItem: (index, direction) => api.put(`/api/plans/draft/items/${index}/move`, { direction }),
  clearDraft: () => api.delete('/api/plans/draft')
}

export const boardApi = {
  list: () => api.get('/api/boards'),
  detail: (id) => api.get(`/api/boards/${id}`),
  create: (title, content) => api.post('/api/boards', { title, content }),
  update: (id, title, content) => api.put(`/api/boards/${id}`, { title, content }),
  delete: (id) => api.delete(`/api/boards/${id}`)
}

export const noticeApi = {
  list: () => api.get('/api/notices'),
  detail: (id) => api.get(`/api/notices/${id}`),
  create: (title, content) => api.post('/api/notices', { title, content }),
  update: (id, title, content) => api.put(`/api/notices/${id}`, { title, content }),
  delete: (id) => api.delete(`/api/notices/${id}`)
}

export const hotplaceApi = {
  list: () => api.get('/api/hotplaces'),
  create: (formData) => api.post('/api/hotplaces', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  delete: (id) => api.delete(`/api/hotplaces/${id}`)
}

export const aiApi = {
  capabilities: () => api.get('/api/ai/capabilities'),
  chat: (body) => api.post('/api/ai/chat', body),
  executeTool: (name, body) => api.post('/api/ai/tools', body, { params: { name } })
}
