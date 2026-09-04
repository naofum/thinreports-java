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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.json.JSONObject;

import com.github.naofum.thinreports.layout.PageContext;
import com.github.naofum.thinreports.model.Item;

/**
 * Renders {@code image} and {@code image-block} elements. The image bytes come
 * either from an embedded base64 {@code data} block or from the data map keyed
 * by the element id (String base64 or {@link BufferedImage}).
 *
 * <p>The image is scaled to fit inside the element box preserving aspect ratio,
 * then positioned according to {@code position-x}/{@code position-y}.</p>
 */
public class ImageRenderer {

    private final PDDocument document;

    public ImageRenderer(PDDocument document) {
        this.document = document;
    }

    public void render(PDPageContentStream cs, PageContext ctx, Item item, Map<String, Object> map)
            throws IOException {
        float x = item.getX();
        float y = item.getY();
        float w = item.getWidth();
        float h = item.getHeight();

        JSONObject style = item.getStyle();
        if (style == null) {
            style = new JSONObject();
        }
        String positionX = style.optString("position-x", "");
        String positionY = style.optString("position-y", "");

        BufferedImage image = loadImage(item, map);
        if (image == null) {
            return;
        }

        // Fit inside the box preserving aspect ratio.
        float scale = Math.min(w / image.getWidth(), h / image.getHeight());
        if (scale > 1f) {
            scale = 1f;
        }
        float drawW = image.getWidth() * scale;
        float drawH = image.getHeight() * scale;

        float leftMargin = 0f;
        if ("center".equals(positionX)) {
            leftMargin = (w - drawW) / 2f;
        } else if ("right".equals(positionX)) {
            leftMargin = w - drawW;
        }
        float topMargin = 0f;
        if ("middle".equals(positionY)) {
            topMargin = (h - drawH) / 2f;
        } else if ("bottom".equals(positionY)) {
            topMargin = h - drawH;
        }

        float drawX = x + leftMargin;
        // PDF y is the bottom edge of the drawn image.
        float drawY = ctx.toPdfY(y + topMargin) - drawH;

        PDImageXObject xobject = LosslessFactory.createFromImage(document, image);
        cs.drawImage(xobject, drawX, drawY, drawW, drawH);
    }

    private BufferedImage loadImage(Item item, Map<String, Object> map) throws IOException {
        JSONObject data = item.getData();
        if (data != null) {
            String base64 = data.optString("base64", "");
            if (!base64.isEmpty()) {
                return decode(base64);
            }
        }
        String id = item.getId();
        if (id != null && !id.isEmpty() && map != null && map.containsKey(id)) {
            Object o = map.get(id);
            if (o instanceof BufferedImage) {
                return (BufferedImage) o;
            }
            if (o instanceof String) {
                return decode((String) o);
            }
        }
        return null;
    }

    private BufferedImage decode(String base64) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(base64);
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            return ImageIO.read(in);
        }
    }
}
