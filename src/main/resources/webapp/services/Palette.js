/**
 * Stores all the palette colors for a single note
 */
export class NoteColors {
    background = [];
    border = [];
    iterator;

    constructor() {
        this.iterator = this.colorGenerator();
    }

    /**
     * Returns the next colors to be used in the palette
     */
    get nextColors() {
        return this.iterator.next().value;
    }

    /**
     * Adds a new note color for the palette
     */
    add(background, border) {
        this.background.push(background);
        this.border.push(border);
    }

    /**
     * Note colors iterator for the current palette
     */
    * colorGenerator() {
        let idx = 0;
        while (true) {
            if (idx >= this.background.length) idx = 0;
            yield {
                background: this.background[idx],
                border: this.border[idx++]
            };
        }
    }
}

/**
 * Stores the colors for the 4 background quadrants
 */
export class QuadrantColors {
    q1; q2; q3; q4;
    constructor(q1, q2, q3, q4) {
        this.q1 = q1;
        this.q2 = q2;
        this.q3 = q3;
        this.q4 = q4;
    }
}

/**
 * Stores all the colors for a single palette: quadrants, arrows, background text and notes
 */
export class Palette {
    noteColors;
    quadrantColors;
    arrowsColor;
    textColor;

    constructor(noteColors, quadrantColors, arrowColors, textColor) {
        this.noteColors = noteColors;
        this.quadrantColors = quadrantColors;
        this.arrowsColor = arrowColors;
        this.textColor = textColor;
    }
}

/**
 * References all the palettes that are available
 */
export class PaletteCollection {
    palettes = new Map();
    _activePalette;
    _activePaletteName;

    constructor() {
        this.palettes.set("Post-It", this.postItPalette);
        this.palettes.set("Shade", this.shadePalette);
        this.palettes.set("High Contrast", this.highContrastPalette);
        const paletteCookie =  ('; '+document.cookie).split(`; palette=`).pop().split(';')[0];
        this.activePaletteName = decodeURIComponent(paletteCookie);
    }

    get activePalette() {
        return this._activePalette;
    }

    get activePaletteName() {
        return this._activePaletteName;
    }

    set activePaletteName(paletteName) {
        if (!paletteName || !this.palettes.has(paletteName)) {
            console.warn(`Palette ${paletteName} does not exist`);
            this._activePaletteName = this.defaultPaletteName
        }
        else {
            this._activePaletteName = paletteName;
        }
        this._activePalette = this.palettes.get(this._activePaletteName);

        document.cookie = `${encodeURIComponent("palette")}=${encodeURIComponent(paletteName)}; path=/; max-age=31536000";`;
    }

    get defaultPaletteName() {
        return "Post-It";
    }

    get postItPalette() {
        const noteColors = new NoteColors();
        noteColors.add("hsla(0, 90%, 89%, 0.7)", "hsl(0, 70%, 69%)");
        noteColors.add("hsla(33, 90%, 87%, 0.7)", "hsl(33, 70%, 67%)");
        noteColors.add("hsla(62, 90%, 91%, 0.7)", "hsl(62, 70%, 71%)");
        noteColors.add("hsla(110, 90%, 92%, 0.7)", "hsl(110, 70%, 72%)");
        noteColors.add("hsla(185, 90%, 90%, 0.7)", "hsl(185, 70%, 65%)");
        noteColors.add("hsla(217, 90%, 86%, 0.7)", "hsl(217, 70%, 66%)");
        noteColors.add("hsla(249, 90%, 91%, 0.7)", "hsl(249, 70%, 70%)");
        noteColors.add("hsla(300, 90%, 94%, 0.7)", "hsl(300, 70%, 74%)");

        const quadrantColors = new QuadrantColors(
            "hsl(8, 67%, 95%)",
            "hsl(92, 52%, 95%)",
            "hsl(274, 57%, 95%)",
            "hsl(46, 100%, 95%)",
        );

        const arrowsColor = "hsl(0, 0%, 10%)";
        const textColor = "hsl(0, 0%, 80%)";

        return new Palette(noteColors, quadrantColors, arrowsColor, textColor);
    }

    get shadePalette() {
        const noteColors = new NoteColors();
        noteColors.add("hsla(300, 100%, 92%, 0.7)", "hsl(300, 100%, 92%)");
        noteColors.add("hsla(275, 100%, 89%, 0.7)", "hsl(275, 100%, 89%)");
        noteColors.add("hsla(255, 100%, 86%, 0.7)", "hsl(255, 100%, 86%)");
        noteColors.add("hsla(233, 100%, 86%, 0.7)", "hsl(233, 100%, 86%)");
        noteColors.add("hsla(221, 100%, 87%, 0.7)", "hsl(221, 100%, 87%)");

        const quadrantColors = new QuadrantColors(
            "hsl(260, 33%, 94%)",
            "hsl(260, 33%, 96%)",
            "hsl(260, 33%, 92%)",
            "hsl(260, 33%, 90%)",
        );

        const arrowsColor = "hsl(251, 20%, 67%)";
        const textColor = "hsl(251, 20%, 67%)";

        return new Palette(noteColors, quadrantColors, arrowsColor, textColor);
    }

    get highContrastPalette() {
        const noteColors = new NoteColors();
        noteColors.add("hsl(360, 100%, 100%)", "hsl(360, 100%, 0%)");

        const quadrantColors = new QuadrantColors(
            "hsl(360, 100%, 100%)",
            "hsl(360, 100%, 100%)",
            "hsl(360, 100%, 100%)",
            "hsl(360, 100%, 100%)",
        );

        const arrowsColor = "hsl(360, 100%, 0%)";
        const textColor = "hsl(360, 100%, 0%)";

        return new Palette(noteColors, quadrantColors, arrowsColor, textColor);
    }
}