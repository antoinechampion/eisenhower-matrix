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
    _paletteCollection;

    constructor() {
        super();
        const shadowRoot = this.attachShadow({ mode: "open" });
        shadowRoot.appendChild(template.content.cloneNode(true));
        this.canvas = shadowRoot.querySelector("canvas");
        this.canvas.width = window.innerWidth;
        this.canvas.height = window.innerHeight;
        this.ctx = this.canvas.getContext("2d");
    }

    set paletteCollection(other) {
        this._paletteCollection = other;
    }

    drawArrow(fromx, tox, fromy, toy, arrowWidth) {
        const headlen = 10;
        const angle = Math.atan2(toy - fromy, tox - fromx);

        this.ctx.strokeStyle = this._paletteCollection.activePalette.arrowsColor;

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
        this.ctx.fillStyle = this._paletteCollection.activePalette.textColor;
        this.ctx.fillText("IMPORTANT", 20, 40);
        this.ctx.fillText("NOT URGENT", 20, 70);
        this.ctx.fillText("IMPORTANT", this.canvas.width - 180, 40);
        this.ctx.fillText("URGENT", this.canvas.width - 180, 70);
        this.ctx.fillText("NOT IMPORTANT", 20, this.canvas.height - 60);
        this.ctx.fillText("NOT URGENT", 20, this.canvas.height - 30);
        this.ctx.fillText("NOT IMPORTANT", this.canvas.width - 240, this.canvas.height - 60);
        this.ctx.fillText("URGENT", this.canvas.width - 240, this.canvas.height - 30);
    }

    drawBackground() {
        this.drawQuadrants([
            {
                x: 0,
                y: 0,
                color: this._paletteCollection.activePalette.quadrantColors.q1
            },
            {
                x: this.canvas.width / 2,
                y: 0,
                color: this._paletteCollection.activePalette.quadrantColors.q2
            },
            {
                x: 0,
                y: this.canvas.height / 2,
                color: this._paletteCollection.activePalette.quadrantColors.q3
            },
            {
                x: this.canvas.width / 2,
                y: this.canvas.height / 2,
                color: this._paletteCollection.activePalette.quadrantColors.q4
            },
        ]);
        const stroke = 2;
        this.drawArrow(0, this.canvas.width - stroke, this.canvas.height / 2,
            this.canvas.height / 2, stroke);
        this.drawArrow(this.canvas.width / 2, this.canvas.width / 2,
            this.canvas.height, stroke, stroke);
        this.drawText();
    }
}

window.customElements.define("eisenhower-background-canvas", BackgroundCanvas);