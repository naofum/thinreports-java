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
package com.github.naofum.thinreports.render;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.json.JSONArray;
import org.json.JSONObject;

import com.github.naofum.thinreports.font.FontResolver;
import com.github.naofum.thinreports.layout.PageContext;
import com.github.naofum.thinreports.model.Item;
import com.github.naofum.thinreports.text.MarkupParser;
import com.github.naofum.thinreports.text.TextRun;

/**
 * Renders {@code text} and {@code text-block} elements.
 *
 * <p>Word wrapping, horizontal/vertical alignment and font-style decoration
 * (bold / italic / underline / line-through / color) are implemented directly
 * against the PDFBox low-level API since no external layout engine is used.</p>
 */
public class TextRenderer {

    private final FontResolver fontResolver;

    public TextRenderer(FontResolver fontResolver) {
        this.fontResolver = fontResolver;
    }

    /**
     * @param cs    active content stream
     * @param ctx   page geometry
     * @param item  the tlf element
     * @param lines already-resolved lines of text (value substitution and
     *              formatting are done by the caller)
     */
    public void render(PDPageContentStream cs, PageContext ctx, Item item, List<String> lines)
            throws IOException {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        JSONObject style = item.getStyle();
        if (style == null) {
            style = new JSONObject();
        }

        float fontSize = (float) style.optDouble("font-size", 11);
        Color color = RenderSupport.parseColor(style.optString("color", "#000000"));
        if (color == null) {
            color = Color.BLACK;
        }
        String textAlign = style.optString("text-align", "left");
        String verticalAlign = style.optString("vertical-align", "top");

        String family = firstFontFamily(style.optJSONArray("font-family"));
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean linethrough = false;
        JSONArray fontStyle = style.optJSONArray("font-style");
        if (fontStyle != null) {
            for (int i = 0; i < fontStyle.length(); i++) {
                switch (fontStyle.optString(i, "")) {
                    case "bold":
                        bold = true;
                        break;
                    case "italic":
                        italic = true;
                        break;
                    case "underline":
                        underline = true;
                        break;
                    case "linethrough":
                        linethrough = true;
                        break;
                    default:
                        break;
                }
            }
        }

        PDFont font = fontResolver.resolve(family, bold, italic);

        float x = item.getX();
        float y = item.getY();
        float width = item.getWidth();
        float height = item.getHeight();

        // Line box: use font size as the leading baseline for a single line and
        // 1.2x font size for wrapping.
        float lineHeight = fontSize * 1.2f;

        // Build styled runs per source line, honoring markup, then wrap each
        // source line to the element width at the run/character level. The
        // element-level font-style flags become the defaults for plain runs.
        List<List<TextRun>> visualLines = new ArrayList<>();
        for (String line : lines) {
            List<TextRun> runs;
            if (MarkupParser.hasMarkup(line)) {
                runs = MarkupParser.parse(line, color);
                // Markup runs already carry their own decoration flags, but the
                // element may add always-on underline/linethrough; merge those.
                if (underline || linethrough) {
                    runs = applyBaseDecoration(runs, underline, linethrough);
                }
            } else {
                runs = new ArrayList<>();
                runs.add(new TextRun(line, bold, italic, underline, linethrough, color));
            }
            visualLines.addAll(wrapRuns(runs, family, fontSize, width));
        }

        float totalTextHeight = visualLines.size() * lineHeight;
        float topPdf = ctx.toPdfY(y); // top edge in PDF coords

        // Vertical alignment offset from the top edge.
        float vOffset;
        switch (verticalAlign) {
            case "middle":
                vOffset = (height - totalTextHeight) / 2f;
                break;
            case "bottom":
                vOffset = height - totalTextHeight;
                break;
            case "top":
            default:
                vOffset = 0f;
                break;
        }
        if (vOffset < 0) {
            vOffset = 0f;
        }

        // Baseline of the first line (approx: top - ascent). Use fontSize as a
        // reasonable ascent proxy scaled by the font's ascent metric.
        float ascent = font.getFontDescriptor() != null
                ? font.getFontDescriptor().getAscent() / 1000f * fontSize
                : fontSize * 0.8f;

        float baselineY = topPdf - vOffset - ascent;
        for (List<TextRun> visualLine : visualLines) {
            drawRunLine(cs, visualLine, family, fontSize, color, textAlign, x, width, baselineY);
            baselineY -= lineHeight;
        }
    }

    /** Return copies of {@code runs} with element-level decoration OR-ed in. */
    private List<TextRun> applyBaseDecoration(List<TextRun> runs, boolean underline,
            boolean linethrough) {
        List<TextRun> out = new ArrayList<>(runs.size());
        for (TextRun r : runs) {
            out.add(new TextRun(r.getText(), r.isBold(), r.isItalic(),
                    r.isUnderline() || underline, r.isLinethrough() || linethrough, r.getColor()));
        }
        return out;
    }

    /**
     * Draw a single visual line composed of styled runs, honoring horizontal
     * alignment across the whole line.
     */
    private void drawRunLine(PDPageContentStream cs, List<TextRun> runs, String family,
            float fontSize, Color defaultColor, String textAlign, float x, float width,
            float baselineY) throws IOException {
        float total = 0f;
        for (TextRun run : runs) {
            PDFont f = fontResolver.resolve(family, run.isBold(), run.isItalic());
            total += stringWidth(f, fontSize, run.getText());
        }
        float cursorX = alignStartX(textAlign, x, width, total);

        for (TextRun run : runs) {
            if (run.getText().isEmpty()) {
                continue;
            }
            PDFont f = fontResolver.resolve(family, run.isBold(), run.isItalic());
            float runWidth = stringWidth(f, fontSize, run.getText());
            Color c = run.getColor() != null ? run.getColor() : defaultColor;

            cs.setNonStrokingColor(c);
            cs.setStrokingColor(c);
            cs.beginText();
            cs.setFont(f, fontSize);
            cs.newLineAtOffset(cursorX, baselineY);
            cs.showText(run.getText());
            cs.endText();

            if (run.isUnderline()) {
                drawDecoration(cs, cursorX, baselineY - fontSize * 0.12f, runWidth, fontSize * 0.06f);
            }
            if (run.isLinethrough()) {
                drawDecoration(cs, cursorX, baselineY + fontSize * 0.28f, runWidth, fontSize * 0.06f);
            }
            cursorX += runWidth;
        }
    }

    private float alignStartX(String textAlign, float x, float width, float lineWidth) {
        switch (textAlign) {
            case "center":
                return x + (width - lineWidth) / 2f;
            case "right":
                return x + (width - lineWidth);
            case "left":
            case "justify":
            default:
                return x;
        }
    }

    private void drawDecoration(PDPageContentStream cs, float x, float y, float width, float thickness)
            throws IOException {
        cs.setLineWidth(thickness);
        cs.setLineDashPattern(new float[] {}, 0);
        cs.moveTo(x, y);
        cs.lineTo(x + width, y);
        cs.stroke();
    }

    /**
     * Wrap a sequence of styled runs (one source line) into one or more visual
     * lines that each fit within {@code maxWidth}, preserving each character's
     * run style so markup decoration survives the break.
     *
     * <p>Wrapping prefers word boundaries: breaks are taken after whitespace and
     * after CJK characters (which may break anywhere). A single token that is by
     * itself wider than {@code maxWidth} is split at the character level as a
     * fallback. A {@code maxWidth <= 0} disables wrapping.</p>
     */
    private List<List<TextRun>> wrapRuns(List<TextRun> runs, String family, float fontSize,
            float maxWidth) throws IOException {
        List<List<TextRun>> lines = new ArrayList<>();
        if (runs == null || runs.isEmpty()) {
            lines.add(new ArrayList<>());
            return lines;
        }
        if (maxWidth <= 0) {
            lines.add(new ArrayList<>(runs));
            return lines;
        }

        // Break the runs into atomic tokens at break opportunities. Each token
        // keeps its owning run's style. A token is either a run of non-breaking
        // characters (a "word", possibly with trailing spaces) or a single CJK
        // character.
        List<TextRun> tokens = tokenize(runs);

        List<TextRun> currentLine = new ArrayList<>();
        float lineWidth = 0f;
        for (TextRun token : tokens) {
            PDFont f = fontResolver.resolve(family, token.isBold(), token.isItalic());
            float tokenWidth = stringWidth(f, fontSize, token.getText());

            if (tokenWidth > maxWidth) {
                // Token alone exceeds the width: flush the line then split the
                // token at the character level.
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine);
                    currentLine = new ArrayList<>();
                    lineWidth = 0f;
                }
                lineWidth = splitLongToken(lines, token, f, fontSize, maxWidth);
                // splitLongToken leaves the remainder as the new current line.
                currentLine = lines.remove(lines.size() - 1);
                continue;
            }

            if (lineWidth + tokenWidth > maxWidth && !currentLine.isEmpty()) {
                lines.add(currentLine);
                currentLine = new ArrayList<>();
                lineWidth = 0f;
            }
            currentLine.add(token);
            lineWidth += tokenWidth;
        }
        lines.add(currentLine);
        return lines;
    }

    /**
     * Split a sequence of styled runs into break-opportunity tokens. Latin words
     * (with trailing spaces) stay intact; CJK characters become individual
     * tokens so they can break anywhere.
     */
    private List<TextRun> tokenize(List<TextRun> runs) {
        List<TextRun> tokens = new ArrayList<>();
        for (TextRun run : runs) {
            String text = run.getText();
            StringBuilder word = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (isCjk(ch)) {
                    if (word.length() > 0) {
                        tokens.add(copyRun(run, word.toString()));
                        word.setLength(0);
                    }
                    tokens.add(copyRun(run, String.valueOf(ch)));
                } else {
                    word.append(ch);
                    // Break after a run of spaces (end of a word).
                    if (ch == ' ' && (i + 1 >= text.length() || text.charAt(i + 1) != ' ')) {
                        tokens.add(copyRun(run, word.toString()));
                        word.setLength(0);
                    }
                }
            }
            if (word.length() > 0) {
                tokens.add(copyRun(run, word.toString()));
            }
        }
        return tokens;
    }

    private boolean isCjk(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }

    /**
     * Split a token wider than {@code maxWidth} at the character level, adding
     * full lines to {@code lines} and leaving the trailing remainder as the last
     * (still-open) line in {@code lines}.
     *
     * @return the width of the trailing remainder line
     */
    private float splitLongToken(List<List<TextRun>> lines, TextRun token, PDFont f,
            float fontSize, float maxWidth) throws IOException {
        String text = token.getText();
        StringBuilder cur = new StringBuilder();
        float lineWidth = 0f;
        List<TextRun> line = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            float chWidth = stringWidth(f, fontSize, String.valueOf(ch));
            if (lineWidth + chWidth > maxWidth && cur.length() > 0) {
                line.add(copyRun(token, cur.toString()));
                lines.add(line);
                line = new ArrayList<>();
                cur.setLength(0);
                lineWidth = 0f;
            }
            cur.append(ch);
            lineWidth += chWidth;
        }
        line.add(copyRun(token, cur.toString()));
        lines.add(line);
        return lineWidth;
    }

    private TextRun copyRun(TextRun run, String text) {
        return new TextRun(text, run.isBold(), run.isItalic(), run.isUnderline(),
                run.isLinethrough(), run.getColor());
    }

    private float stringWidth(PDFont font, float fontSize, String text) throws IOException {
        try {
            return font.getStringWidth(text) / 1000f * fontSize;
        } catch (IllegalArgumentException | IOException e) {
            // Some glyphs may be missing from the font; approximate.
            return text.length() * fontSize * 0.5f;
        }
    }

    private String firstFontFamily(JSONArray fontFamily) {
        if (fontFamily != null && fontFamily.length() > 0) {
            return fontFamily.optString(0, "");
        }
        return "";
    }
}
