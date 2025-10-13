/**
 * Wrapper for `fetch` which sets requests headers and parses response body
 */
const $fetch = (input, init) => {
    if (!init) {
        init = { method: "GET" };
    }
    if (!init.headers) {
        init.headers = new Headers();
    }
    if (!init.headers.has("Content-Type") && !(init.body instanceof FormData)) {
        init.headers.set("Content-Type", "application/json");
    }
    init.credentials = "same-origin";

    return fetch(input, init)
        .then(resp => {
            if (resp.status < 299 && resp.headers.has("Content-Type") && resp.headers.get("Content-Type").endsWith("json")) {
                return resp.json();
            }
            else {
                return resp;
            }
        });
}

/**
 * Direct binding of the Authentication back-end API
 */
export class Auth {
    static login (email, password) { 
        return $fetch("/auth/login", {
            method: "POST",
            body: JSON.stringify({
                email: email,
                password: password
            }),
        });
    }
    static register (email, password) {
        return  $fetch("/auth/register", {
            method: "POST",
            body: JSON.stringify({
                email: email,
                password: password
            }),
        });
    };
    static logout () {
        return $fetch("/auth/logout", {
            method: "POST",
        });
    }
}

/**
 * Direct binding of the Boards back-end API
 */
export class Boards {
    static list () {
        return $fetch("/api/board/list/");
    }
    static create (boardTitle) {
        return $fetch("/api/board/", {
            method: "POST",
            body: JSON.stringify({title: boardTitle}),
        });
    }
}

/**
 * Direct binding of the Notes back-end API
 */
export class Notes {
    static list(boardId) {
        return $fetch(`/api/note/list/?boardId=${boardId}`);
    }

    static get(noteId) {
        return $fetch(`/api/note/${noteId}`);
    }

    static create(note) {
        return $fetch("/api/note/", {
            method: "POST",
            body: JSON.stringify(note),
        });
    }

    static delete (noteId) {
        return $fetch(`/api/note/${noteId}`, {
            method: "DELETE"
        });
    }

    static batchUpdate(notes) {
        return $fetch("/api/note/batch/update", {
            method: "PUT",
            body: JSON.stringify(notes),
        });
    }
}

/**
 * Direct binding of the AttachedDocuments back-end API
 */
export class AttachedDocuments {
    static get (noteId) {
        return $fetch(`/api/attached-document/${noteId}`);
    }

    static upsert (noteId, content) {
        const form = new FormData();
        const blob = new Blob([content], {
            type: 'text/plain'
        });
        form.append('file', blob, 'file');
        return $fetch(`/api/attached-document/${noteId}`, {
            method: "POST",
            body: form,
        });
    };

    static share (noteId) {
        return $fetch(`/api/attached-document/${noteId}/share`, {
            method: "POST",
        });
    }
}

/**
 * Static collection of all the back-end APIs
 */
export class Api {
    static get auth() { return Auth; }
    static get boards() { return Boards; }
    static get notes() { return Notes; }
    static get attachedDocuments() { return AttachedDocuments; }
}

