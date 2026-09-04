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
package com.github.naofum.thinreports.model;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A single drawable element of a tlf layout (text, text-block, image,
 * image-block, rect, ellipse or line).
 *
 * <p>This is a thin, null-safe accessor over the underlying {@link JSONObject}
 * so renderers do not have to repeat {@code has()/opt()} boilerplate.</p>
 */
public class Item {

    private final JSONObject json;

    public Item(JSONObject json) {
        this.json = json;
    }

    public JSONObject raw() {
        return json;
    }

    public String getId() {
        return json.optString("id", "");
    }

    public String getType() {
        return json.optString("type", "");
    }

    public boolean isDisplay() {
        return json.optBoolean("display", false);
    }

    public float getX() {
        return (float) json.optDouble("x", 0);
    }

    public float getY() {
        return (float) json.optDouble("y", 0);
    }

    public float getWidth() {
        return (float) json.optDouble("width", 0);
    }

    public float getHeight() {
        return (float) json.optDouble("height", 0);
    }

    // Line coordinates
    public float getX1() {
        return (float) json.optDouble("x1", 0);
    }

    public float getY1() {
        return (float) json.optDouble("y1", 0);
    }

    public float getX2() {
        return (float) json.optDouble("x2", 0);
    }

    public float getY2() {
        return (float) json.optDouble("y2", 0);
    }

    // Ellipse coordinates
    public float getCx() {
        return (float) json.optDouble("cx", 0);
    }

    public float getCy() {
        return (float) json.optDouble("cy", 0);
    }

    public float getRx() {
        return (float) json.optDouble("rx", 0);
    }

    public float getRy() {
        return (float) json.optDouble("ry", 0);
    }

    public float getBorderRadius() {
        return (float) json.optDouble("border-radius", 0);
    }

    public JSONObject getStyle() {
        return json.optJSONObject("style");
    }

    public JSONArray getTexts() {
        return json.optJSONArray("texts");
    }

    public JSONObject getFormat() {
        return json.optJSONObject("format");
    }

    public JSONObject getData() {
        return json.optJSONObject("data");
    }
}
