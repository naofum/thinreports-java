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

import org.json.JSONObject;

/**
 * A {@code list} element: a repeating detail band with optional header,
 * page-footer and footer bands, plus automatic page-break support.
 */
public class ListItem {

    private final String id;
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final ListBand header;
    private final ListBand detail;
    private final ListBand pageFooter;
    private final ListBand footer;
    private final float contentHeight;
    private final boolean autoPageBreak;

    public ListItem(Item item) {
        JSONObject json = item.raw();
        this.id = item.getId();
        this.x = item.getX();
        this.y = item.getY();
        this.width = item.getWidth();
        this.height = item.getHeight();
        this.header = new ListBand(json.optJSONObject("header"));
        this.detail = new ListBand(json.optJSONObject("detail"));
        this.pageFooter = new ListBand(json.optJSONObject("page-footer"));
        this.footer = new ListBand(json.optJSONObject("footer"));
        this.contentHeight = (float) json.optDouble("content-height", this.height);
        this.autoPageBreak = json.optBoolean("auto-page-break", true);
    }

    public String getId() {
        return id;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public ListBand getHeader() {
        return header;
    }

    public ListBand getDetail() {
        return detail;
    }

    public ListBand getPageFooter() {
        return pageFooter;
    }

    public ListBand getFooter() {
        return footer;
    }

    public float getContentHeight() {
        return contentHeight;
    }

    public boolean isAutoPageBreak() {
        return autoPageBreak;
    }
}
