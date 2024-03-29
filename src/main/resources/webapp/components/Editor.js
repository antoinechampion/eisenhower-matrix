import "https://cdn.jsdelivr.net/npm/@tinymce/tinymce-webcomponent@1/dist/tinymce-webcomponent.min.js"

import { Api } from "../services/Api.js";
import { EditorSharingButton } from "./EditorSharingButton.js";
import "./SyncStatus.js";
import { Router } from "../services/Router.js";

// TinyMCE requires global callbacks for event handling
window.setupTinyMce = (editor) => document.querySelector("eisenhower-editor").onSetupTinyMce(editor);
window.initTinyMce = (event) => document.querySelector("eisenhower-editor").onInitTinyMce(event);
window.keyUpTinyMce = () => document.querySelector("eisenhower-editor").onKeyUpTinyMce();

const template = document.createElement("template");
template.innerHTML = `
    <style>
        #fileTooLargeAlert {    
            z-index: 9999;
            position: absolute;
            top: 0;
            left: 0;
            display: block;
        }
        #sharing-footer {
            background-color: LemonChiffon;
            position: absolute;
            bottom: 0;
            left: 0;
            right: 0;
            padding: 10px;
            z-index: 9999;
            font-family: sans-serif;
            visibility: hidden;
        }
    </style>

    <p id="loading">Loading...</p>
    
    <tinymce-editor id="editor"
        style="display: none; height: 100%"
        menubar="false"
        content_style="body { font-family:Helvetica,Arial,sans-serif; font-size:14px }",
        plugins="preview importcss searchreplace autolink directionality code visualblocks visualchars fullscreen image link media template codesample table charmap pagebreak nonbreaking anchor insertdatetime advlist lists wordcount help charmap quickbars emoticons"
        toolbar="undo redo | bold italic underline strikethrough | fontfamily blocks | alignleft aligncenter alignright alignjustify | outdent indent | numlist bullist | forecolor backcolor removeformat | print | media link codesample | sharingButton"

        setup="setupTinyMce"
        on-Init="initTinyMce"
        on-KeyUp="keyUpTinyMce"
    >
    </tinymce-editor>
    
    <div id="sharing-footer">This document is being shared in read-only mode</div>
    
    <eisenhower-sync-status></eisenhower-sync-status>
`;

/**
 * One-page editor for attached documents
 */
export class Editor extends HTMLElement {
    noteId = "";
    initialContent = "";
    changedContent = null;
    saving = false;
    editorWrapper;
    editor;
    editorSharingButton;
    isGuestUser;

    constructor() {
        super();
        const shadowRoot = this.attachShadow({ mode: "open" });
        shadowRoot.appendChild(template.content.cloneNode(true));
        this.editorWrapper = shadowRoot.getElementById("editor");
    }

    connectedCallback() {
        if (!this.parseQueryParams()) {
            return;
        }
        this.loadDocument()
            .then(success => {
                if (success) {
                    this.shadowRoot.getElementById("loading").style.display = "none";
                    this.editorWrapper.style.display = "block";
                    this.editor.setContent(this.initialContent);
                    this.editorSharingButton.noteId = this.noteId;
                }
            })
            .then(() => {
                if (this.isGuestUser)  {
                    this.editor.getBody().setAttribute('contenteditable', false);
                    this.editor.execCommand("ToggleToolbarDrawer");
                    this.shadowRoot.getElementById("sharing-footer").style.visibility = "visible";
                }
            })
    }

    /**
     * Retrieves the noteId to determine the attached document from the query params
     * @returns {boolean} true on success
     */
    parseQueryParams() {
        const urlParams = new URLSearchParams(window.location.search);
        if (!urlParams.has("noteId")) {
            console.error("Missing required query param 'noteId'");
            return false;
        }
        this.noteId = urlParams.get("noteId");
        return true;
    }

    /**
     * Creates a document with empty content for the active note
     */
    createNewDocument() {
        const content = "<p>Write your content here...</p>";
        return Api.attachedDocuments.upsert(this.noteId, content)
            .then(() => Api.attachedDocuments.get(this.noteId));
    }

    /**
     * Fetches the document for the current note and if it does not exist yet, creates it.
     * Then, initialize the editor
     */
    loadDocument() {
        return Api.notes.get(this.noteId)
            .then((resp) => {
                if (resp.id) {
                    document.title = resp.text;
                }
                return Api.attachedDocuments.get(this.noteId);
            })
            .then((resp) => {
                if (resp.status && resp.status === 404) {
                    console.log(`Attached document for note ${this.noteId} can't be retrieved. Creating a new document`);
                    return this.createNewDocument();
                }
                else if (resp.status <= 302) {
                    return resp;
                }
                else {
                    Router.goToHome();
                }
            })
            .then(resp => {
                this.isGuestUser = resp.headers.has("X-Document-Guest");
                return resp;
            })
            .then(resp => resp.text())
            .then(content => {
                this.initialContent = content;
                if (this.isGuestUser) {
                    document.title = this.initialContent.substring(0, 50).replace(/<[^>]+>/g, '') + "...";
                }
                return true;
            })
            .catch(() => false);
    }

    /**
     * Fallthrough callback for the 'setup' action
     * @param editor
     */
    onSetupTinyMce(editor) {
        this.editor = editor;
        this.editorSharingButton = new EditorSharingButton(this.editor);
    }

    /**
     * Fallthrough callback for the 'Init' event
     */
    onInitTinyMce(event) {
        this.editor.setContent(this.initialContent);
        this.editor.execCommand("mceFullScreen");
        setInterval(this.saveContentCron.bind(this), 1000);
    }

    /**
     * Fallthrough callback for the 'KeyUp' event
     */
    onKeyUpTinyMce() {
        this.changedContent = this.editor.getContent();
        this.updateSyncStatus();
    }

    /**
     * Update modified document on a scheduled basis
     */
    saveContentCron() {
        if (this.changedContent && !this.saving) {
            const changedContentBackup = this.changedContent;
            this.saving = true;
            // Set changedContent to null before sending the request
            // as the user could change the content while the request is sending
            this.changedContent = null;
            Api.attachedDocuments.upsert(this.noteId, changedContentBackup)
                .then((resp) => {
                    if (resp.status === 413) {
                        this.displayDocumentTooLargeAlert();
                    }
                    this.saving = false;
                    this.updateSyncStatus();
                })
                .catch(() => {
                    if (!this.changedContent) {
                        // The user did not modify the content while the request was being sent
                        // hence we need to trigger a retry
                        this.changedContent = changedContentBackup;
                    }
                    console.error("Can't update attached document");
                    this.saving = false;
                });
        }
    }

    /**
     * Displays a toast alert to warn that the current document is too large to be saved
     */
    displayDocumentTooLargeAlert() {
        this.editor.notificationManager.open({
            text: "The current document is too large and can't be saved. " +
                "If you have images, try to make them smaller or store them externally.",
            type: "error"
        });
    }

    /**
     * Sets the sync icon to be displayed if there are changes that have not been persisted
     */
    updateSyncStatus() {
        const syncStatus = this.shadowRoot.querySelector("eisenhower-sync-status");
        syncStatus.setSyncStatus(this.changedContent !== null);
    }
}

window.customElements.define("eisenhower-editor", Editor);