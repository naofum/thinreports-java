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
 * A single band (header / detail / page-footer / footer) of a {@code list}
 * element. Each band declares a height, an optional {@code translate} offset
 * and the child items drawn within it.
 */
public class ListBand {

    private final boolean enabled;
    private final float height;
    private final float translateX;
    private final float translateY;
    private final List<Item> items;

    public ListBand(JSONObject band) {
        if (band == null) {
            band = new JSONObject();
        }
        // "detail" has no "enabled" flag; treat presence of items as enabled.
        this.enabled = band.optBoolean("enabled", true);
        this.height = (float) band.optDouble("height", 0);

        JSONObject translate = band.optJSONObject("translate");
        if (translate != null) {
            this.translateX = (float) translate.optDouble("x", 0);
            this.translateY = (float) translate.optDouble("y", 0);
        } else {
            this.translateX = 0f;
            this.translateY = 0f;
        }

        this.items = new ArrayList<>();
        JSONArray arr = band.optJSONArray("items");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) {
                    items.add(new Item(o));
                }
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public float getHeight() {
        return height;
    }

    public float getTranslateX() {
        return translateX;
    }

    public float getTranslateY() {
        return translateY;
    }

    public List<Item> getItems() {
        return items;
    }
}
