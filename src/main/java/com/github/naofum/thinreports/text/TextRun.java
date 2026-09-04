/*
 * Copyright 2015 Naofumi Fukue
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.naofum.thinreports.text;

import java.awt.Color;

/**
 * A run of text sharing the same style, produced by {@link MarkupParser}.
 */
public class TextRun {

    private final String text;
    private final boolean bold;
    private final boolean italic;
    private final boolean underline;
    private final boolean linethrough;
    private final Color color;

    public TextRun(String text, boolean bold, boolean italic, boolean underline,
            boolean linethrough, Color color) {
        this.text = text;
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.linethrough = linethrough;
        this.color = color;
    }

    public String getText() {
        return text;
    }

    public boolean isBold() {
        return bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public boolean isUnderline() {
        return underline;
    }

    public boolean isLinethrough() {
        return linethrough;
    }

    /** May be {@code null} to mean "inherit the element's color". */
    public Color getColor() {
        return color;
    }
}
