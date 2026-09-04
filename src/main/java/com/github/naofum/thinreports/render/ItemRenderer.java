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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.json.JSONObject;

import com.github.naofum.thinreports.format.ValueResolver;
import com.github.naofum.thinreports.layout.PageContext;
import com.github.naofum.thinreports.model.Item;

/**
 * Dispatches a single tlf item to the appropriate element renderer.
 *
 * <p>Supports a vertical offset so the same item definition can be drawn at a
 * shifted position — this is what makes repeating list rows possible without
 * mutating the template.</p>
 */
public class ItemRenderer {

    private final TextRenderer textRenderer;
    private final ImageRenderer imageRenderer;
    private final RectRenderer rectRenderer = new RectRenderer();
    private final EllipseRenderer ellipseRenderer = new EllipseRenderer();
    private final LineRenderer lineRenderer = new LineRenderer();

    public ItemRenderer(TextRenderer textRenderer, ImageRenderer imageRenderer) {
        this.textRenderer = textRenderer;
        this.imageRenderer = imageRenderer;
    }

    public void render(PDPageContentStream cs, PageContext ctx, Item item, Map<String, Object> map)
            throws IOException {
        render(cs, ctx, item, map, 0f);
    }

    /**
     * @param yOffset amount (in tlf points, positive = downward) to shift the
     *                element before drawing
     */
    public void render(PDPageContentStream cs, PageContext ctx, Item item, Map<String, Object> map,
            float yOffset) throws IOException {
        Item target = (yOffset == 0f) ? item : shift(item, yOffset);
        switch (target.getType()) {
            case "text":
            case "text-block": {
                List<String> lines = ValueResolver.resolveLines(target, map);
                textRenderer.render(cs, ctx, target, lines);
                break;
            }
            case "image":
            case "image-block":
                imageRenderer.render(cs, ctx, target, map);
                break;
            case "rect":
                rectRenderer.render(cs, ctx, target);
                break;
            case "ellipse":
                ellipseRenderer.render(cs, ctx, target);
                break;
            case "line":
                lineRenderer.render(cs, ctx, target);
                break;
            default:
                break;
        }
    }

    /**
     * Produce a copy of the item's JSON with all Y coordinates shifted down by
     * {@code yOffset}. Handles box (y), line (y1/y2) and ellipse (cy) forms.
     */
    private Item shift(Item item, float yOffset) {
        JSONObject src = item.raw();
        JSONObject copy = new JSONObject(src.toString());
        if (copy.has("y")) {
            copy.put("y", copy.optDouble("y", 0) + yOffset);
        }
        if (copy.has("y1")) {
            copy.put("y1", copy.optDouble("y1", 0) + yOffset);
        }
        if (copy.has("y2")) {
            copy.put("y2", copy.optDouble("y2", 0) + yOffset);
        }
        if (copy.has("cy")) {
            copy.put("cy", copy.optDouble("cy", 0) + yOffset);
        }
        return new Item(copy);
    }
}
