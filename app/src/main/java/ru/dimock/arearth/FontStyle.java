package ru.dimock.arearth;

import android.graphics.Typeface;

public enum FontStyle {
    Normal(Typeface.NORMAL),
    Bold(Typeface.BOLD),
    Italic(Typeface.ITALIC),
    BoldItalic(Typeface.BOLD_ITALIC);

    private int value;

    FontStyle(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }
}
