const template = document.createElement("template");
template.innerHTML = `
    <canvas class="#background"></canvas>
`;

/**
 * Renderer for the quadrants and arrows background of a matrix
 */
export class BackgroundCanvas extends HTMLElement {
    canvas;
    ctx;
    settings;

    constructor() {
        super();
        const shadowRoot = this.attachShadow({ mode: "open" });
        shadowRoot.appendChild(template.content.cloneNode(true));
        this.canvas = shadowRoot.querySelector("canvas");
        this.canvas.width = window.innerWidth;
        this.canvas.height = window.innerHeight;
        this.ctx = this.canvas.getContext("2d");
    }

    set matrixSettings(settings) {
        this.settings = settings;
        this.settings.addEventListener("paletteSelectionChanged", () => this.drawBackground());
        this.settings.addEventListener("flipMatrix", () => this.drawBackground());
    }

    drawArrow(fromx, tox, fromy, toy, arrowWidth) {
        const headlen = 10;
        const angle = Math.atan2(toy - fromy, tox - fromx);

        this.ctx.strokeStyle = this.settings.paletteCollection.activePalette.arrowsColor;

        this.ctx.beginPath();
        this.ctx.moveTo(fromx, fromy);
        this.ctx.lineTo(tox, toy);
        this.ctx.lineWidth = arrowWidth;
        this.ctx.stroke();

        this.ctx.beginPath();
        this.ctx.moveTo(tox, toy);
        this.ctx.lineTo(tox - headlen * Math.cos(angle - Math.PI / 7),
            toy - headlen * Math.sin(angle - Math.PI / 7));

        this.ctx.lineTo(tox - headlen * Math.cos(angle + Math.PI / 7),
            toy - headlen * Math.sin(angle + Math.PI / 7));

        this.ctx.lineTo(tox, toy);
        this.ctx.lineTo(tox - headlen * Math.cos(angle - Math.PI / 7),
            toy - headlen * Math.sin(angle - Math.PI / 7));

        this.ctx.stroke();
        this.ctx.restore();
    }

    drawQuadrants(quadrants) {
        for (let q of quadrants) {
            this.ctx.fillStyle = q.color;
            this.ctx.fillRect(q.x, q.y, this.canvas.width / 2, this.canvas.height / 2);
        }
        this.ctx.restore();
    }

    drawText() {
        this.ctx.font = "24px 'Arial Black'";
        this.ctx.fillStyle = this.settings.paletteCollection.activePalette.textColor;

        const quadrantPositions = [
            {x: 20, y: 40},
            {x: this.canvas.width - 180, y: 40},
            {x: 20, y: this.canvas.height - 60},
            {x: this.canvas.width - 180, y: this.canvas.height - 60},
        ];
        const lineJump = 30;

        let quadrantTexts;
        if (this.settings.flipMatrix) {
            quadrantTexts = [
                ["IMPORTANT", "NOT URGENT"],
                ["IMPORTANT", "URGENT"],
                ["NOT IMPORTANT", "NOT URGENT"],
                ["NOT IMPORTANT", "URGENT"],
            ];
        }
        else {
            quadrantTexts = [
                ["IMPORTANT", "URGENT"],
                ["IMPORTANT", "NOT URGENT"],
                ["NOT IMPORTANT", "URGENT"],
                ["NOT IMPORTANT", "NOT URGENT"],
            ];
        }

        for (let i = 0; i < 4; i++) {
            this.ctx.fillText(quadrantTexts[i][0], quadrantPositions[i].x, quadrantPositions[i].y);
            this.ctx.fillText(quadrantTexts[i][1], quadrantPositions[i].x, quadrantPositions[i].y + lineJump);
        }
    }

    drawBackground() {
        this.drawQuadrants([
            {
                x: 0,
                y: 0,
                color: this.settings.paletteCollection.activePalette.quadrantColors.q1
            },
            {
                x: this.canvas.width / 2,
                y: 0,
                color: this.settings.paletteCollection.activePalette.quadrantColors.q2
            },
            {
                x: 0,
                y: this.canvas.height / 2,
                color: this.settings.paletteCollection.activePalette.quadrantColors.q3
            },
            {
                x: this.canvas.width / 2,
                y: this.canvas.height / 2,
                color: this.settings.paletteCollection.activePalette.quadrantColors.q4
            },
        ]);
        const stroke = 2;
        if (this.settings.flipMatrix) {
            this.drawArrow(0, this.canvas.width - stroke, this.canvas.height / 2,
                this.canvas.height / 2, stroke);
        }
        else {
            this.drawArrow(this.canvas.width - stroke, 0, this.canvas.height / 2,
                this.canvas.height / 2, stroke);
        }
        this.drawArrow(this.canvas.width / 2, this.canvas.width / 2,
            this.canvas.height, stroke, stroke);
        this.drawText();
    }
}

window.customElements.define("eisenhower-background-canvas", BackgroundCanvas);