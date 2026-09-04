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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Parsed representation of a Thinreports layout file (.tlf).
 *
 * <p>The tlf format (schema version 0.9.x) is a JSON document composed of a
 * {@code report} section (paper type / orientation / margin), an ordered list
 * of {@code items} (the drawable elements) and optional {@code settings}.</p>
 */
public class Report {

    private final JSONObject root;
    private final String paperType;
    private final String orientation;
    private final float marginTop;
    private final float marginRight;
    private final float marginBottom;
    private final float marginLeft;
    private final List<Item> items;

    public Report(JSONObject root) {
        this.root = root;
        JSONObject report = root.optJSONObject("report");
        if (report == null) {
            report = new JSONObject();
        }
        this.paperType = report.optString("paper-type", "A4");
        this.orientation = report.optString("orientation", "portrait");

        JSONArray margin = report.optJSONArray("margin");
        if (margin != null && margin.length() >= 4) {
            this.marginTop = (float) margin.optDouble(0, 0);
            this.marginRight = (float) margin.optDouble(1, 0);
            this.marginBottom = (float) margin.optDouble(2, 0);
            this.marginLeft = (float) margin.optDouble(3, 0);
        } else {
            this.marginTop = this.marginRight = this.marginBottom = this.marginLeft = 0f;
        }

        this.items = new ArrayList<>();
        JSONArray arr = root.optJSONArray("items");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) {
                    items.add(new Item(o));
                }
            }
        }
    }

    public JSONObject raw() {
        return root;
    }

    public JSONObject settings() {
        return root.optJSONObject("settings");
    }

    public String getPaperType() {
        return paperType;
    }

    public String getOrientation() {
        return orientation;
    }

    public boolean isLandscape() {
        return "landscape".equals(orientation);
    }

    public float getMarginTop() {
        return marginTop;
    }

    public float getMarginRight() {
        return marginRight;
    }

    public float getMarginBottom() {
        return marginBottom;
    }

    public float getMarginLeft() {
        return marginLeft;
    }

    public List<Item> getItems() {
        return items;
    }
}
