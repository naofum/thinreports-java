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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONObject;

/**
 * Applies a tlf {@code format} block to a raw data value, reproducing the
 * Ruby generator's number / padding / datetime formatting.
 */
public final class ValueFormatter {

    private ValueFormatter() {
    }

    /**
     * Format {@code value} according to {@code format}. When {@code format} is
     * null or has no {@code type}, the value's plain string form is returned.
     *
     * @param value  raw value from the data map (String, Number, Date, ...)
     * @param format tlf "format" JSON object (may be null)
     * @return formatted string
     */
    public static String apply(Object value, JSONObject format) {
        if (value == null) {
            return "";
        }
        if (format == null) {
            return String.valueOf(value);
        }
        String type = format.optString("type", "");
        String base = format.optString("base", "");

        String formatted;
        switch (type) {
            case "number":
                formatted = formatNumber(value, format.optJSONObject("number"));
                break;
            case "padding":
                formatted = formatPadding(value, format.optJSONObject("padding"));
                break;
            case "datetime":
                formatted = formatDatetime(value, format.optJSONObject("datetime"));
                break;
            default:
                // No type: still honor a base template if present.
                formatted = String.valueOf(value);
                break;
        }
        return applyBase(base, formatted);
    }

    private static String applyBase(String base, String value) {
        if (base == null || base.isEmpty()) {
            return value;
        }
        return base.replace("{value}", value);
    }

    private static String formatNumber(Object value, JSONObject number) {
        if (number == null) {
            number = new JSONObject();
        }
        String delimiter = number.optString("delimiter", "");
        int precision = number.optInt("precision", 0);

        double d = toDouble(value);
        StringBuilder pattern = new StringBuilder();
        pattern.append(delimiter.isEmpty() ? "0" : "#,##0");
        if (precision > 0) {
            pattern.append('.');
            for (int i = 0; i < precision; i++) {
                pattern.append('0');
            }
        }
        DecimalFormat df = new DecimalFormat(pattern.toString());
        String out = df.format(d);
        // The tlf delimiter is usually "," which matches DecimalFormat's default
        // grouping separator; support an alternative delimiter char if given.
        if (!delimiter.isEmpty() && !",".equals(delimiter)) {
            out = out.replace(",", delimiter);
        }
        return out;
    }

    private static String formatPadding(Object value, JSONObject padding) {
        if (padding == null) {
            padding = new JSONObject();
        }
        String ch = padding.optString("char", " ");
        int length = padding.optInt("length", 0);
        String direction = padding.optString("direction", "L");

        String s = String.valueOf(value);
        if (s.length() >= length || ch.isEmpty()) {
            return s;
        }
        StringBuilder pad = new StringBuilder();
        while (pad.length() < length - s.length()) {
            pad.append(ch);
        }
        String padStr = pad.substring(0, length - s.length());
        // "L" pads on the left (right-aligned value); otherwise pad on the right.
        return "L".equalsIgnoreCase(direction) ? padStr + s : s + padStr;
    }

    private static String formatDatetime(Object value, JSONObject datetime) {
        if (!(value instanceof Date)) {
            return String.valueOf(value);
        }
        if (datetime == null) {
            datetime = new JSONObject();
        }
        String rubyFormat = datetime.optString("format", "%Y/%m/%d");
        String javaPattern = toJavaDatePattern(rubyFormat);
        return new SimpleDateFormat(javaPattern, Locale.ENGLISH).format((Date) value);
    }

    /**
     * Translate a Ruby strftime pattern to a {@link SimpleDateFormat} pattern.
     * Covers the directives commonly used in thinreports templates.
     *
     * <p>Uses a single left-to-right scan so that overlapping directives
     * (e.g. {@code %m} vs {@code %M}) and literal text are handled correctly.
     * Literal characters that are significant to {@link SimpleDateFormat}
     * (letters and quotes) are quoted so they are emitted verbatim.</p>
     */
    static String toJavaDatePattern(String ruby) {
        if (ruby == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        StringBuilder literal = new StringBuilder();
        int i = 0;
        int n = ruby.length();
        while (i < n) {
            char c = ruby.charAt(i);
            if (c == '%' && i + 1 < n) {
                char d = ruby.charAt(i + 1);
                String mapped = mapDirective(d);
                if (mapped != null) {
                    flushLiteral(out, literal);
                    out.append(mapped);
                    i += 2;
                    continue;
                }
                if (d == '%') {
                    literal.append('%');
                    i += 2;
                    continue;
                }
            }
            literal.append(c);
            i++;
        }
        flushLiteral(out, literal);
        return out.toString();
    }

    /**
     * @return the {@link SimpleDateFormat} fragment for a Ruby strftime
     *         directive letter, or {@code null} if it is not recognised
     */
    private static String mapDirective(char d) {
        switch (d) {
            case 'Y':
                return "yyyy";
            case 'y':
                return "yy";
            case 'm':
                return "MM";
            case 'B':
                return "MMMM";
            case 'b':
            case 'h':
                return "MMM";
            case 'd':
                return "dd";
            case 'e':
                // Ruby: day of month, blank-padded. SimpleDateFormat has no
                // blank-padding; a single 'd' is the closest match.
                return "d";
            case 'j':
                return "DDD";
            case 'A':
                return "EEEE";
            case 'a':
                return "EEE";
            case 'H':
                return "HH";
            case 'I':
                return "hh";
            case 'M':
                return "mm";
            case 'S':
                return "ss";
            case 'L':
                return "SSS";
            case 'p':
                return "a";
            case 'Z':
                return "zzz";
            case 'z':
                return "Z";
            default:
                return null;
        }
    }

    /**
     * Append the accumulated literal text to {@code out}, quoting it so that
     * {@link SimpleDateFormat} treats it verbatim.
     */
    private static void flushLiteral(StringBuilder out, StringBuilder literal) {
        if (literal.length() == 0) {
            return;
        }
        String text = literal.toString();
        literal.setLength(0);
        boolean needQuote = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c) || c == '\'') {
                needQuote = true;
                break;
            }
        }
        if (needQuote) {
            // Escape embedded single quotes as '' then wrap in quotes.
            out.append('\'').append(text.replace("'", "''")).append('\'');
        } else {
            out.append(text);
        }
    }

    private static double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0d;
        }
    }
}
