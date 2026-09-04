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

/**
 * Small helpers shared by the element renderers.
 */
public final class RenderSupport {

    private RenderSupport() {
    }

    /**
     * Parse a CSS-style hex color such as {@code #ff0000}. Returns {@code null}
     * for null/empty input so callers can treat it as "no color".
     */
    public static Color parseColor(String hex) {
        if (hex == null || hex.isEmpty()) {
            return null;
        }
        try {
            return Color.decode(hex);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Map a tlf border-style to a PDF dash pattern.
     *
     * @param borderStyle "solid", "dashed" or "dotted"
     * @return dash array, or {@code null} for a solid line
     */
    public static float[] dashPattern(String borderStyle) {
        if ("dashed".equals(borderStyle)) {
            return new float[] { 3f, 3f };
        }
        if ("dotted".equals(borderStyle)) {
            return new float[] { 1f, 3f };
        }
        return null;
    }
}
