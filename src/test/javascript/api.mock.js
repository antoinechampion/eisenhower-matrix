
export const api = {
    auth: {
        login: (email, password) => Promise.resolve({ "token": "", "email": "test" }),
        register: (email, password) => Promise.resolve({ "token": "", "email": "test" }),
        logout: () => Promise.resolve(),
    },

    boards: {
        list: () => Promise.resolve([{"id":"e6585328-eefa-4dc4-b2c2-9be4656d4b75","title":"My personal board"}]),
        create: (boardTitle) => $fetch("/api/board/", {
            method: "POST",
            body: JSON.stringify({title: boardTitle}),
        }),
    },

    notes: {
        list: (boardId) => $fetch(`/api/note/list/?boardId=${boardId}`),
        get: (noteId) => $fetch(`/api/note/${noteId}`),
        create: (note) => $fetch("/api/note/", {
            method: "POST",
            body: JSON.stringify(note),
        }),
        delete: (noteId) => $fetch(`/api/note/${noteId}`, {
            method: "DELETE"
        }),
        batch: {
            update: (notes) => $fetch("/api/note/batch/update", {
                method: "PUT",
                body: JSON.stringify(notes),
            }),
        }
    },
    attachedDocuments: {
        get: (noteId) => $fetch(`/api/attached-document/${noteId}`),
        upsert: (noteId, content) => {
            const form = new FormData();
            const blob = new Blob([content], {
                type: 'text/plain'
            });
            form.append('file', blob, 'file');
            return $fetch(`/api/attached-document/${noteId}`, {
                method: "POST",
                body: form,
            });
        },
    }
}

