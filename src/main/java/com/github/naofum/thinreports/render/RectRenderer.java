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

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.json.JSONObject;

import com.github.naofum.thinreports.layout.PageContext;
import com.github.naofum.thinreports.model.Item;

/**
 * Draws {@code rect} elements (optionally with rounded corners) directly onto
 * a {@link PDPageContentStream} at absolute coordinates.
 */
public class RectRenderer {

    private static final float KAPPA = 0.5522847498f;

    public void render(PDPageContentStream cs, PageContext ctx, Item item) throws IOException {
        float x = item.getX();
        float y = item.getY();
        float w = item.getWidth();
        float h = item.getHeight();
        float radius = item.getBorderRadius();

        JSONObject style = item.getStyle();
        if (style == null) {
            style = new JSONObject();
        }
        Color border = RenderSupport.parseColor(style.optString("border-color", ""));
        float borderWidth = (float) style.optDouble("border-width", 1);
        String borderStyle = style.optString("border-style", "solid");
        Color fill = RenderSupport.parseColor(style.optString("fill-color", ""));

        float pdfY = ctx.toPdfYForBox(y, h);

        if (radius <= 0) {
            cs.addRect(x, pdfY, w, h);
        } else {
            appendRoundedRect(cs, x, pdfY, w, h, Math.min(radius, Math.min(w, h) / 2f));
        }

        paint(cs, fill, border, borderWidth, borderStyle);
    }

    private void appendRoundedRect(PDPageContentStream cs, float x, float y,
            float w, float h, float r) throws IOException {
        float c = KAPPA * r;
        // Start at the top edge, just right of the top-left corner.
        cs.moveTo(x + r, y + h);
        cs.lineTo(x + w - r, y + h);
        cs.curveTo(x + w - r + c, y + h, x + w, y + h - r + c, x + w, y + h - r);
        cs.lineTo(x + w, y + r);
        cs.curveTo(x + w, y + r - c, x + w - r + c, y, x + w - r, y);
        cs.lineTo(x + r, y);
        cs.curveTo(x + r - c, y, x, y + r - c, x, y + r);
        cs.lineTo(x, y + h - r);
        cs.curveTo(x, y + h - r + c, x + r - c, y + h, x + r, y + h);
        cs.closePath();
    }

    private void paint(PDPageContentStream cs, Color fill, Color border,
            float borderWidth, String borderStyle) throws IOException {
        boolean hasFill = fill != null;
        boolean hasBorder = border != null && borderWidth > 0;

        if (hasFill) {
            cs.setNonStrokingColor(fill);
        }
        if (hasBorder) {
            cs.setStrokingColor(border);
            cs.setLineWidth(borderWidth);
            float[] dash = RenderSupport.dashPattern(borderStyle);
            if (dash != null) {
                cs.setLineDashPattern(dash, 0);
            } else {
                cs.setLineDashPattern(new float[] {}, 0);
            }
        }

        if (hasFill && hasBorder) {
            cs.fillAndStroke();
        } else if (hasFill) {
            cs.fill();
        } else if (hasBorder) {
            cs.stroke();
        } else {
            // Nothing to paint; discard the current path.
            cs.stroke();
        }
    }
}
