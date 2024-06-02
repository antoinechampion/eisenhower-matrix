import "https://cdn.jsdelivr.net/npm/interactjs/dist/interact.min.js"

import { Api } from "../services/Api.js";
import { Router } from "../services/Router.js";
import "./SyncStatus.js";
import "./BackgroundCanvas.js";
import "./MatrixSettings.js";

const template = document.createElement("template");
template.innerHTML = `
    <style>
        :host-context(eisenhower-background-canvas) .background {
            position:absolute;
            left:0;
            top:0;
            z-index:-1;
        }
        
        #matrix-container {
            position: absolute;
            left:0;
            top:0;
            height: 100%;
            width: 100%;
        }
        
        .note {
            position: absolute;
            height: max(11%, 75px);
            width: max(15%, 150px);
            
            border-radius: 3px;
            border-width: 2px;
            padding: 6px 12px;
            font-size: 16px;
            font-family: var(--sl-font-sans);
            touch-action: none;
            box-sizing: border-box;
        }
        
        .buttons-bar {
            position: absolute;
            z-index: 1000;
            bottom: 20px;
            right: 50%;
            transform: translate(50%, 0);
        }
        
        svg {
            margin: 16px;
            fill: #1c6ca144;
            transition: all 0.3s;
        }
        
        svg:hover {
            margin: 16px;
            fill: #1c6ca1;
            cursor: pointer;
            transform: scale(1.3);
        }
        
        #settingsMenu {
            max-width: 200px;
            margin-left: auto;
        }
    </style>    

    <eisenhower-background-canvas></eisenhower-background-canvas>
    
    <div class="buttons-bar">
        <eisenhower-matrix-settings></eisenhower-matrix-settings>
        <svg id="button-add" xmlns="http://www.w3.org/2000/svg" width="36" height="36" fill="blue" viewBox="0 0 16 16">
            <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zM8.5 4.5a.5.5 0 0 0-1 0v3h-3a.5.5 0 0 0 0 1h3v3a.5.5 0 0 0 1 0v-3h3a.5.5 0 0 0 0-1h-3v-3z"></path>
        </svg>
        <svg id="button-remove" xmlns="http://www.w3.org/2000/svg" width="36" height="36" fill="blue" viewBox="0 0 16 16">
            <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zM4.5 7.5a.5.5 0 0 0 0 1h7a.5.5 0 0 0 0-1h-7z"/>
        </svg>
        <svg id="button-document" xmlns="http://www.w3.org/2000/svg" width="36" height="36" fill="blue" viewBox="0 0 16 16">
            <path d="M4 0h5.293A1 1 0 0 1 10 .293L13.707 4a1 1 0 0 1 .293.707V14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2zm5.5 1.5v2a1 1 0 0 0 1 1h2l-3-3z"/>
        </svg>
        <svg id="button-settings" xmlns="http://www.w3.org/2000/svg" width="36" height="36" fill="blue" viewBox="0 0 24 24">
            <path d="m9.25 22l-.4-3.2q-.325-.125-.612-.3q-.288-.175-.563-.375L4.7 19.375l-2.75-4.75l2.575-1.95Q4.5 12.5 4.5 12.337v-.675q0-.162.025-.337L1.95 9.375l2.75-4.75l2.975 1.25q.275-.2.575-.375q.3-.175.6-.3l.4-3.2h5.5l.4 3.2q.325.125.613.3q.287.175.562.375l2.975-1.25l2.75 4.75l-2.575 1.95q.025.175.025.337v.675q0 .163-.05.338l2.575 1.95l-2.75 4.75l-2.95-1.25q-.275.2-.575.375q-.3.175-.6.3l-.4 3.2Zm2.8-6.5q1.45 0 2.475-1.025Q15.55 13.45 15.55 12q0-1.45-1.025-2.475Q13.5 8.5 12.05 8.5q-1.475 0-2.488 1.025Q8.55 10.55 8.55 12q0 1.45 1.012 2.475Q10.575 15.5 12.05 15.5Z"/>
        </svg>
    </div>

    <div id="matrix-container">
    </div>
    
    <eisenhower-sync-status></eisenhower-sync-status>
`;

/**
 * Displays and manages the notes in a board
 */
export class Matrix extends HTMLElement {
    notesToSaveQueue = new Set();
    saving = false;
    activeBoard;
    lastSelectedNote = null;
    matrixContainer;
    background;
    settings;

    constructor() {
        super();
        const shadowRoot = this.attachShadow({ mode: "open" });
        shadowRoot.appendChild(template.content.cloneNode(true));
        this.matrixContainer = shadowRoot.getElementById("matrix-container");
        this.background = shadowRoot.querySelector("eisenhower-background-canvas");
        this.settings = shadowRoot.querySelector("eisenhower-matrix-settings");
        this.background.paletteCollection = this.settings.paletteCollection;
    }

    connectedCallback() {
        this.background.drawBackground();
        this.background.canvas
            .addEventListener("click", () => {
                this.lastSelectedNote = null;
            });
        this.settings.addEventListener("paletteSelectionChanged", () => this.onPaletteSelectionChanged());

        this.initInteractJs();
        this.initBoardNotes();
        this.initButtons();

        setInterval(this.saveNoteCron.bind(this), 1000);
    }

    /**
     * Initializes the library that makes notes draggable and resizable
     */
    initInteractJs() {
        interact(".note")
            .resizable({
                edges: {
                    left: true,
                    right: true,
                    bottom: true,
                    top: true
                },

                listeners: {
                    move: this.onNoteResize.bind(this)
                },
                modifiers: [
                    interact.modifiers.restrictEdges({
                        outer: "parent"
                    }),

                    interact.modifiers.restrictSize({
                        min: {
                            width: 100,
                            height: 50
                        }
                    })
                ],

                inertia: true
            })
            .draggable({
                listeners: {
                    move: this.onNoteMove.bind(this)
                }
            });
    }

    /**
     * Handles a note that has been resize
     */
    onNoteResize(event) {
        let target = event.target
        let x = (parseFloat(target.getAttribute("data-x")) || 0);
        let y = (parseFloat(target.getAttribute("data-y")) || 0);

        target.style.width = event.rect.width + "px";
        target.style.height = event.rect.height + "px";

        x += event.deltaRect.left;
        y += event.deltaRect.top;

        target.style.transform = `translate(${x}px, ${y}px)`;

        target.setAttribute("data-x", x);
        target.setAttribute("data-y", y);
    }

    /**
     * Handles a note that has been moved on the board
     */
    onNoteMove(event) {
        const target = event.target
        const x = (parseFloat(target.getAttribute("data-x")) || 0) + event.dx;
        const y = (parseFloat(target.getAttribute("data-y")) || 0) + event.dy;

        target.style.transform =
            `translate(${x}px, ${y}px)`;

        target.setAttribute("data-x", x);
        target.setAttribute("data-y", y);

        this.notesToSaveQueue.add(target);
        this.updateSyncStatus();
        this.refreshNotesZIndex();
    }

    /**
     * Sets the sync icon to be displayed if there are modified notes which have not been persisted
     */
    updateSyncStatus() {
        const syncStatus = this.shadowRoot.querySelector("eisenhower-sync-status");
        syncStatus.setSyncStatus(this.notesToSaveQueue.size > 0);
    }

    /**
     * Create a new note on the board
     */
    createNote(note) {
        let noteDom = document.createElement("textarea");
        const colors = this.settings.paletteCollection.activePalette.noteColors.nextColors;
        noteDom.style["background-color"] = colors.background;
        noteDom.style["border-color"] = colors.border;
        noteDom.classList.add("note");
        noteDom.value = note.text;

        const x = note.urgency * this.background.canvas.offsetWidth;
        const y = note.importance * this.background.canvas.offsetHeight;
        noteDom.style["transform"] = `translate(${x}px, ${y}px)`;
        noteDom.setAttribute("data-x", x.toString());
        noteDom.setAttribute("data-y", y.toString());

        noteDom.setAttribute("data-id", note.id);
        noteDom.addEventListener("input", () => {
            this.notesToSaveQueue.add(noteDom);
            this.updateSyncStatus();
        });
        noteDom.addEventListener("focus", () => {
            this.lastSelectedNote = noteDom;
        });

        this.matrixContainer.append(noteDom);
        this.refreshNotesZIndex();
    }

    /**
     * Update and persists the modified notes on a scheduled basis
     */
    saveNoteCron() {
        if (this.notesToSaveQueue.size > 0 && !this.saving) {
            let notesDto = [];
            let notesToSaveQueueBackup = new Set();
            this.saving = true;

            for (let noteDom of this.notesToSaveQueue) {
                notesDto.push({
                    id: noteDom.getAttribute("data-id"),
                    text: noteDom.value,
                    urgency: noteDom.getAttribute("data-x") / this.background.canvas.width,
                    importance: noteDom.getAttribute("data-y") / this.background.canvas.height,
                });
                notesToSaveQueueBackup.add(noteDom);
            }
            console.log("Updating " + this.notesToSaveQueue.size + " notes");

            this.notesToSaveQueue.clear();
            Api.notes.batchUpdate(notesDto)
                .then(() => {
                    this.saving = false;
                    this.updateSyncStatus();
                }).catch((e) => {
                    if (this.notesToSaveQueue.size === 0) {
                        this.notesToSaveQueue = notesToSaveQueueBackup;
                    }
                    console.error("Failed to save notes: " + e);
                    this.saving = false;
                });
            }
    }

    /**
     * Initializes the matrix with all the notes in the current board
     */
    initBoardNotes() {
        Api.boards.list()
            .then((resp) => {
                this.activeBoard = resp[0].id;
                return Api.notes.list(this.activeBoard);
            })
            .then(resp => {
                for (let noteDto of resp) {
                    this.createNote(noteDto);
                }
            })
    }

    /**
     * Initializes the button bars and the buttons callbacks
     */
    initButtons() {
        this.shadowRoot.getElementById("button-add")
            .addEventListener("click", () => {
                const noteDto = { text: "", urgency: 0.1, importance: 0.1, boardId: this.activeBoard };
                Api.notes.create(noteDto).then((resp) => {
                    this.createNote(resp);
                })
            });
        this.shadowRoot.getElementById("button-remove")
            .addEventListener("click", () => {
                if (!this.lastSelectedNote) {
                    return;
                }
                if (!confirm("Remove note with content '" + this.lastSelectedNote.value + "'?")) {
                    return;
                }
                const noteId = this.lastSelectedNote.getAttribute("data-id");
                Api.notes.delete(noteId).then(() => {
                    this.lastSelectedNote.remove();
                    this.lastSelectedNote = null;
                })
            });
        this.shadowRoot.getElementById("button-document")
            .addEventListener("click", () => {
                if (!this.lastSelectedNote) {
                    return;
                }
                const noteId = this.lastSelectedNote.getAttribute("data-id");
                Router.goToEditor(noteId);
            });
        this.shadowRoot.getElementById("button-settings")
            .addEventListener("click", () => this.settings.toggleMenu());
    }

    /**
     * Updates the z-index of all the notes in the board based on their position
     */
    refreshNotesZIndex() {
        const hasPriorityOver = (noteA, noteB) =>
            parseInt(noteA.getAttribute("data-x")) + parseInt(noteA.getAttribute("data-y"))
            > parseInt(noteB.getAttribute("data-x")) + parseInt(noteB.getAttribute("data-y"));

        let notes = [...this.shadowRoot.querySelectorAll(".note")];
        let sorted;
        do {
            sorted = true;
            for (let i = 0; i < notes.length - 1; i++) {
                if (hasPriorityOver(notes[i], notes[i+1])) {
                    const tmp = notes[i];
                    notes[i] = notes[i+1];
                    notes[i+1] = tmp;
                    sorted = false;
                }
            }
        } while (!sorted);

        for (let i = 0; i < notes.length; i++) {
            notes[i].style["z-index"] = 100 + i;
        }
    }

    /**
     * Updates the color of the notes and background when the palette changed
     */
    onPaletteSelectionChanged() {
        this.background.drawBackground();
        const notesDom = this.shadowRoot.querySelectorAll(".note");
        for (let noteDom of notesDom) {
            let colors = this.settings.paletteCollection.activePalette.noteColors.nextColors;
            noteDom.style["background-color"] = colors.background;
            noteDom.style["border-color"] = colors.border;
        }
    }
}

window.customElements.define("eisenhower-matrix", Matrix);