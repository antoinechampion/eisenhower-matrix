import {Api} from "../services/Api.js";

export class EditorSharingButton {
    editor;
    noteId;

    constructor(editor) {
        this.editor = editor;
        this.setupSharingComponents();
    }

    set noteId(id) {
        this.noteId = id;
    }

    get sharingLink() {
        return `/editor?noteId=${this.noteId}`;
    }

    get panelConfiguration() {
        return {
            title: 'Share your document',
            body: {
                type: 'panel',
                items: [
                    {
                        type: 'htmlpanel', // component type
                        html: `<h3>Copy your sharing link below:</h3><br><a href="${this.sharingLink}">${this.sharingLink}</a>`
                    }
                ],
            },
            buttons: [],
        };
    }

    setupSharingComponents() {
        this.editor.ui.registry.addButton('sharingButton', {
            text: 'Share',
            onAction: this.onSharingButtonClick.bind(this)
        });
    }

    onSharingButtonClick() {
        Api.attachedDocuments.share(this.noteId);
        this.editor.windowManager.open(this.panelConfiguration);
    }
}
