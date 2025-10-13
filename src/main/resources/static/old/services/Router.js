/**
 * Simple routing class to navigate within the application
 */
export class Router {
    static goToHome() {
        Router.goToPage("/old/index.html");
    }

    static goToMatrix(boardId) {
        const url = boardId ? `/old/matrix.html?boardId=${boardId}` : "/old/matrix.html";
        Router.goToPage(url);
    }

    static goToEditor(noteId) {
        window.open(`/old/editor.html?noteId=${noteId}`, "_blank");
    }

    static goToPage(url) {
        window.location.assign(url);
    }
}