import { PaletteCollection } from "../services/Palette.js";
import { Api } from "../services/Api.js";
import { Router } from "../services/Router.js";

const template = document.createElement("template");

template.innerHTML = `
    <style>

    </style>    

    <sl-menu style="display: none">
        <sl-menu-item id="logoutButton">Log Out</sl-menu-item>
        <sl-divider></sl-divider>
        <sl-menu-label>Palette</sl-menu-label>
        <div id="paletteMenuSection">
            <sl-menu-item>Post-It</sl-menu-item>
            <sl-menu-item>Shade</sl-menu-item>
            <sl-menu-item>High Contrast</sl-menu-item>
        </div>
        <sl-divider></sl-divider>
        <sl-menu-item id="flipMatrixButton">Flip Matrix</sl-menu-item>
    </sl-menu>
`;

/**
 * Display the collapsable settings menu at the bottom of the matrix and manage the storage and change of the settings
 */
export class MatrixSettings extends HTMLElement {
    paletteCollection;
    flipMatrix;

    constructor() {
        super();
        const shadowRoot = this.attachShadow({ mode: "open" });
        shadowRoot.appendChild(template.content.cloneNode(true));

        this.paletteCollection = new PaletteCollection();
        this.flipMatrix = localStorage.getItem("flipMatrix") === "true";
    }

    connectedCallback() {
        this.initLogoutButton();
        this.initPaletteSelector();
        this.initFlipMatrixButton();
    }

    /**
     * Make the log-out button make an API call and redirect to website home
     */
    initLogoutButton() {
        this.shadowRoot.getElementById("logoutButton")
            .addEventListener("click", () => {
                Api.auth.logout().then(Router.goToHome);
            });
    }

    /**
     * Bind all the available palettes to the selector
     */
    initPaletteSelector() {
        this.refreshPalettesMenu();

        const paletteMenuSection = this.shadowRoot.getElementById("paletteMenuSection");
        for (let paletteButton of paletteMenuSection.children) {
            const buttonPaletteName = paletteButton.innerHTML.trim();
            paletteButton.addEventListener("click", () => {
                this.paletteCollection.activePaletteName = buttonPaletteName;
                this.refreshPalettesMenu();
                this.dispatchEvent(new Event("paletteSelectionChanged"));
            })
        }
    }

    /**
     * Refresh the active palette selection
     */
    refreshPalettesMenu() {
        const activePaletteName = this.paletteCollection.activePaletteName;
        const paletteMenuSection = this.shadowRoot.getElementById("paletteMenuSection");
        for (let paletteButton of paletteMenuSection.children) {
            const buttonPaletteName = paletteButton.innerHTML.trim();
            paletteButton.checked = buttonPaletteName === activePaletteName;
            paletteButton.render();
        }
    }

    /**
     * Show or hide the settings menu
     */
    toggleMenu() {
        const menu = this.shadowRoot.querySelector("sl-menu");
        menu.style.display = menu.style.display === "none" ? "block" : "none";
    }

    /**
     * Flip the vertical axis of the matrix
     */
    initFlipMatrixButton() {
        const button = this.shadowRoot.getElementById("flipMatrixButton");
        button.checked = this.flipMatrix;

        button.addEventListener("click", () => {
            this.flipMatrix = !this.flipMatrix;
            button.checked = this.flipMatrix;
            localStorage.setItem("flipMatrix", this.flipMatrix);
            this.dispatchEvent(new Event("flipMatrix"));
        });
    }
}

window.customElements.define("eisenhower-matrix-settings", MatrixSettings);