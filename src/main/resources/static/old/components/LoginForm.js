import { Api } from "../services/Api.js";
import { Router } from "../services/Router.js";

const template = document.createElement("template");
template.innerHTML = `
<style>
    .form-container {
        display: flex;
        margin: 24px;
    }
    sl-card {
        margin: 0 auto;
    }
</style>

<div class="form-container">
    <sl-card class="card-basic">
        <sl-progress-bar style="--height: 4px; display: none;" indeterminate></sl-progress-bar>
        
        <sl-alert id="errorAlert" variant="danger">
            <sl-icon slot="icon" name="exclamation-octagon"></sl-icon>
            <strong><span id="errorMessage"></span></strong><br />
            <span id="errorDetails"></span>
        </sl-alert>
        
        <sl-tab-group>
            <sl-tab slot="nav" panel="register">Register</sl-tab>
            <sl-tab slot="nav" panel="login">Log In</sl-tab>
            
            <sl-tab-panel name="register">
                <h3>Create your matrix</h3>
                <form class="input-validation-required" name="registerForm">
                    <sl-input name="email" label="Your email address" required></sl-input>
                    <br />
                    <sl-input name="password" label="Your password" type="password" password-toggle required></sl-input>
                    <br /><br />
                    <sl-button type="submit" variant="primary">Register</sl-button>
                </form>
            </sl-tab-panel>
            <sl-tab-panel name="login">
                <h3>Resume your matrix</h3>
                <form class="input-validation-required" name="loginForm">
                    <sl-input name="email" label="Your email address" required></sl-input>
                    <br />
                    <sl-input name="password" label="Your password" type="password" password-toggle required></sl-input>
                    <br /><br />
                    <sl-button type="submit" variant="primary">Log In</sl-button>
                </form>
            </sl-tab-panel>
        </sl-tab-group>
    </sl-card>
</div>
`;

/**
 * Registration and log-in form
 */
export class LoginForm extends HTMLElement {
    forms;
    errorAlert;
    progressBar;

    constructor() {
        super();
        const shadowRoot = this.attachShadow({ mode: "open" });
        shadowRoot.appendChild(template.content.cloneNode(true));
        this.forms = shadowRoot.querySelectorAll("form");
        this.forms.forEach(f => f.addEventListener("submit", event => {
            event.preventDefault();
            this.formSubmit(f);
        }));
        this.errorAlert = shadowRoot.querySelector("#errorAlert");
        this.progressBar = shadowRoot.querySelector("sl-progress-bar");
    }

    /**
     * Hides the error modal
     */
    hideError() {
        this.errorAlert.open = false;
    }

    /**
     * Displays the error modal with a given message
     */
    showError(errorMessage, errorDetails) {
        const errorMessageTag = this.errorAlert.querySelector("#errorMessage");
        errorMessageTag.innerHTML = errorMessage;
        const errorDetailsTag = this.errorAlert.querySelector("#errorDetails");
        errorDetailsTag.innerHTML = errorDetails;
        this.errorAlert.open = true;
    }

    /**
     * Callback for a registration or login form submission
     * @param form
     */
    formSubmit(form) {
        const email = form.querySelector("sl-input[name='email']").value;
        const password = form.querySelector("sl-input[name='password']").value;

        this.progressBar.style.display = "block";
        this.hideError();
        let requestPromise;
        if (form.getAttribute("name") === "registerForm") {
            requestPromise = this.register(email, password);
        }
        else if (form.getAttribute("name") === "loginForm") {
            requestPromise = this.login(email, password);
        }
        requestPromise.then(() =>
            this.progressBar.style.display = "none");
    }

    /**
     * Handles a registration submission and redirects to the matrix page if success
     */
    register(email, password) {
        return Api.auth.register(email, password)
            .catch((err) => {
                this.showError("We can't create your account", "An internal error happened");
            })
            .then(res => {
                if (res.status === 409) { // Conflict
                    this.showError("An account already exists with this email", "Please log in to your account");
                } else {
                    return Api.boards.create("My personal board").then(() => Router.goToMatrix());
                }
            })
            .catch(() => {
                this.showError("We can't create a matrix for your account", "An internal error happened");
            });
    }

    /**
     * Handles a log-in submission and redirects to the matrix page if success
     */
    login(email, password) {
        return Api.auth.login(email, password)
            .catch(() => {
                this.showError("We can't create log you in", "An internal error happened");
            })
            .then(res => {
                if (res.status === 401) { // Unauthorized
                    this.showError("Wrong email or password", "Please try again");
                }
                else {
                    Router.goToMatrix();
                }
            });
    }
}

window.customElements.define("eisenhower-login-form", LoginForm);