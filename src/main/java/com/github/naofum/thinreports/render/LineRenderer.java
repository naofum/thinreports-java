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
 * Draws {@code line} elements as a straight stroked segment between two
 * absolute tlf coordinates.
 */
public class LineRenderer {

    public void render(PDPageContentStream cs, PageContext ctx, Item item) throws IOException {
        JSONObject style = item.getStyle();
        if (style == null) {
            style = new JSONObject();
        }
        Color border = RenderSupport.parseColor(style.optString("border-color", ""));
        if (border == null) {
            border = Color.BLACK;
        }
        float borderWidth = (float) style.optDouble("border-width", 1);
        String borderStyle = style.optString("border-style", "solid");

        float x1 = item.getX1();
        float y1 = ctx.toPdfY(item.getY1());
        float x2 = item.getX2();
        float y2 = ctx.toPdfY(item.getY2());

        cs.setStrokingColor(border);
        cs.setLineWidth(borderWidth);
        float[] dash = RenderSupport.dashPattern(borderStyle);
        if (dash != null) {
            cs.setLineDashPattern(dash, 0);
        } else {
            cs.setLineDashPattern(new float[] {}, 0);
        }
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }
}
