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
 * Draws {@code ellipse} elements. The tlf model uses a center point
 * ({@code cx}, {@code cy}) plus radii ({@code rx}, {@code ry}); the ellipse is
 * approximated with four cubic Bezier segments.
 */
public class EllipseRenderer {

    private static final float KAPPA = 0.5522847498f;

    public void render(PDPageContentStream cs, PageContext ctx, Item item) throws IOException {
        float cx = item.getCx();
        float cy = item.getCy();
        float rx = item.getRx();
        float ry = item.getRy();

        JSONObject style = item.getStyle();
        if (style == null) {
            style = new JSONObject();
        }
        Color border = RenderSupport.parseColor(style.optString("border-color", ""));
        float borderWidth = (float) style.optDouble("border-width", 1);
        String borderStyle = style.optString("border-style", "solid");
        Color fill = RenderSupport.parseColor(style.optString("fill-color", ""));

        float pcx = cx;
        float pcy = ctx.toPdfY(cy);
        float ox = rx * KAPPA;
        float oy = ry * KAPPA;

        cs.moveTo(pcx - rx, pcy);
        cs.curveTo(pcx - rx, pcy + oy, pcx - ox, pcy + ry, pcx, pcy + ry);
        cs.curveTo(pcx + ox, pcy + ry, pcx + rx, pcy + oy, pcx + rx, pcy);
        cs.curveTo(pcx + rx, pcy - oy, pcx + ox, pcy - ry, pcx, pcy - ry);
        cs.curveTo(pcx - ox, pcy - ry, pcx - rx, pcy - oy, pcx - rx, pcy);
        cs.closePath();

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
        } else {
            cs.stroke();
        }
    }
}
