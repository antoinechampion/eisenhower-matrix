/**
 * Simple routing class to navigate within the application
 */
export class Router {
    static goToHome() {
        Router.goToPage("/");
    }

    static goToMatrix(boardId) {
        const url = boardId ? `/matrix?boardId=${boardId}` : "/matrix";
        Router.goToPage(url);
    }

    static goToEditor(noteId) {
        window.open(`/editor?noteId=${noteId}`, "_blank");
    }

    static goToPage(url) {
        window.location.assign(url);
    }
}