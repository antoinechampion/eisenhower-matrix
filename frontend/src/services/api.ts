const $fetch = (input: string, init?: RequestInit) => {
  if (!init) {
    init = { method: 'GET' }
  }
  if (!init.headers) {
    init.headers = new Headers()
  }
  const headers = init.headers as Headers
  if (!headers.has('Content-Type') && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  init.credentials = 'same-origin'

  return fetch(input, init).then((resp) => {
    if (
      resp.status < 299 &&
      resp.headers.has('Content-Type') &&
      resp.headers.get('Content-Type')!.endsWith('json')
    ) {
      return resp.json()
    }
    return resp
  })
}

export const Auth = {
  login: (email: string, password: string) =>
    $fetch('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  register: (email: string, password: string) =>
    $fetch('/auth/register', { method: 'POST', body: JSON.stringify({ email, password }) }),
  logout: () => $fetch('/auth/logout', { method: 'POST' }),
  forgotPassword: (email: string) =>
    $fetch('/auth/forgot-password', { method: 'POST', body: JSON.stringify({ email }) }),
  resetPassword: (token: string, newPassword: string) =>
    $fetch('/auth/reset-password', { method: 'POST', body: JSON.stringify({ token, newPassword }) }),
}

export const Boards = {
  list: () => $fetch('/api/board/list/'),
  create: (title: string) =>
    $fetch('/api/board/', { method: 'POST', body: JSON.stringify({ title }) }),
}

export const Notes = {
  list: (boardId: string) => $fetch(`/api/note/list/?boardId=${boardId}`),
  get: (noteId: string) => $fetch(`/api/note/${noteId}`),
  create: (note: object) =>
    $fetch('/api/note/', { method: 'POST', body: JSON.stringify(note) }),
  delete: (noteId: string) => $fetch(`/api/note/${noteId}`, { method: 'DELETE' }),
  batchUpdate: (notes: object[]) =>
    $fetch('/api/note/batch/update', { method: 'PUT', body: JSON.stringify(notes) }),
}

export const AttachedDocuments = {
  get: (noteId: string) => $fetch(`/api/attached-document/${noteId}`),
  upsert: (noteId: string, content: string) => {
    const form = new FormData()
    const blob = new Blob([content], { type: 'text/plain' })
    form.append('file', blob, 'file')
    return $fetch(`/api/attached-document/${noteId}`, { method: 'POST', body: form })
  },
  share: (noteId: string) =>
    $fetch(`/api/attached-document/${noteId}/share`, { method: 'POST' }),
}
