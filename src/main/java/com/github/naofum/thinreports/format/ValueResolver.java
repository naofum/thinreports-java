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
package com.github.naofum.thinreports.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;

import com.github.naofum.thinreports.model.Item;

/**
 * Produces the display lines for a text / text-block element by combining the
 * static {@code texts} of the template with dynamic values from the data map.
 */
public final class ValueResolver {

    private ValueResolver() {
    }

    /**
     * @param item the text element
     * @param map  data values keyed by element id (may be null)
     * @return the lines to render (never null; may be empty)
     */
    public static List<String> resolveLines(Item item, Map<String, Object> map) {
        String id = item.getId();
        // Dynamic value from the data map takes precedence for text-block.
        if (id != null && !id.isEmpty() && map != null && map.containsKey(id)) {
            Object value = map.get(id);
            String formatted = ValueFormatter.apply(value, item.getFormat());
            return splitLines(formatted);
        }
        // Static texts from the template.
        JSONArray texts = item.getTexts();
        if (texts != null && texts.length() > 0) {
            List<String> lines = new ArrayList<>();
            for (int i = 0; i < texts.length(); i++) {
                lines.addAll(splitLines(texts.optString(i, "")));
            }
            return lines;
        }
        return new ArrayList<>();
    }

    private static List<String> splitLines(String s) {
        if (s == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(s.split("\n", -1)));
    }
}
