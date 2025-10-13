const template = document.createElement("template");
template.innerHTML = `
    <style>
        h1 {
            font-size: var(--sl-font-size-large);
        }
        
        h2 {
            font-weight: var(--sl-font-weight-light);
            font-size: calc(var(--sl-font-size-large) * 0.85);
        }
        
        nav {
            width: 100%;
            overflow: hidden;
            display: flex;
            justify-content: space-around;
        }
        
        .nav__menu {
            display: flex;
            align-items: center;
        }
        
        .nav__item {
            margin: auto 24px;
        }
        
        a.nav__item {
            text-decoration: underline;
            text-decoration-style: dotted;
            text-underline-offset: 2px;
            color: #2a2a2a;
        }
    </style>
    
    <nav>
        <div class="nav__item">
            <h1>Eisenhower Matrix</h1>
            <h2>Free priority matrix editor</h2>
        </div>
        <div class="nav__menu">
            <a class="nav__item" href="/">
                Home
            </a>
            <a class="nav__item" href="/faq">
                FAQ
            </a>
        </div>
    </nav>
    <sl-divider></sl-divider>
`;

/**
 * Navigation bar for the static pages
 */
class NavigationBar extends HTMLElement {
    constructor() {
        super();
        const shadowRoot = this.attachShadow({ mode: "open" });
        shadowRoot.appendChild(template.content.cloneNode(true));
    }
}

window.customElements.define("eisenhower-navigation-bar", NavigationBar);