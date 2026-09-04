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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the Thinreports text-block dynamic-style markup into styled
 * {@link TextRun} segments. Supported syntax (compatible with the Ruby
 * generator / the original prototype):
 *
 * <ul>
 *   <li>{@code {color:#rrggbb}} — set the current color (until changed)</li>
 *   <li>{@code *bold*} — toggle bold</li>
 *   <li>{@code _italic_} — toggle italic</li>
 *   <li>{@code __underline__} — toggle underline</li>
 *   <li>{@code __{0.25:}line-through__} — line-through</li>
 * </ul>
 *
 * <p>Markers are toggles: the same marker opens and later closes the style.
 * The {@code __{n:}...__} form (used by Thinreports for line-through) is
 * treated as line-through rather than underline.</p>
 */
public final class MarkupParser {

    private static final Pattern COLOR = Pattern.compile("^\\{color:(#[0-9a-fA-F]{6})\\}");
    private static final Pattern LINETHROUGH_OPEN = Pattern.compile("^__\\{[0-9.]+:[^}]*\\}");

    private MarkupParser() {
    }

    /**
     * @param markup       the raw markup for a single logical line
     * @param defaultColor the element color to use before any {@code {color:}}
     * @return styled runs (never null; may be empty)
     */
    public static List<TextRun> parse(String markup, Color defaultColor) {
        List<TextRun> runs = new ArrayList<>();
        if (markup == null || markup.isEmpty()) {
            return runs;
        }

        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean linethrough = false;
        Color color = defaultColor;

        StringBuilder buf = new StringBuilder();
        int i = 0;
        int n = markup.length();
        while (i < n) {
            String rest = markup.substring(i);

            // {color:#rrggbb}
            Matcher cm = COLOR.matcher(rest);
            if (cm.find()) {
                flush(runs, buf, bold, italic, underline, linethrough, color);
                try {
                    color = Color.decode(cm.group(1));
                } catch (NumberFormatException ignored) {
                    // keep current color
                }
                i += cm.end();
                continue;
            }

            // __{n:...}  line-through open
            Matcher lm = LINETHROUGH_OPEN.matcher(rest);
            if (lm.find()) {
                flush(runs, buf, bold, italic, underline, linethrough, color);
                linethrough = true;
                i += lm.end();
                continue;
            }

            // __ underline / line-through close
            if (rest.startsWith("__")) {
                flush(runs, buf, bold, italic, underline, linethrough, color);
                if (linethrough) {
                    linethrough = false;
                } else {
                    underline = !underline;
                }
                i += 2;
                continue;
            }

            // * bold
            if (rest.charAt(0) == '*') {
                flush(runs, buf, bold, italic, underline, linethrough, color);
                bold = !bold;
                i += 1;
                continue;
            }

            // _ italic
            if (rest.charAt(0) == '_') {
                flush(runs, buf, bold, italic, underline, linethrough, color);
                italic = !italic;
                i += 1;
                continue;
            }

            buf.append(markup.charAt(i));
            i += 1;
        }
        flush(runs, buf, bold, italic, underline, linethrough, color);
        return runs;
    }

    private static void flush(List<TextRun> runs, StringBuilder buf, boolean bold, boolean italic,
            boolean underline, boolean linethrough, Color color) {
        if (buf.length() == 0) {
            return;
        }
        runs.add(new TextRun(buf.toString(), bold, italic, underline, linethrough, color));
        buf.setLength(0);
    }

    /**
     * @return true if the string contains any recognised markup marker
     */
    public static boolean hasMarkup(String s) {
        if (s == null) {
            return false;
        }
        return s.indexOf('*') >= 0 || s.indexOf('_') >= 0 || s.contains("{color:");
    }
}
