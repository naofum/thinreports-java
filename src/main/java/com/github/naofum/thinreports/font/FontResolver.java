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
package com.github.naofum.thinreports.font;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;

import com.github.naofum.thinreports.Settings;

/**
 * Resolves a tlf {@code font-family} name (optionally with bold/italic) into a
 * {@link PDFont}. Standard-14 fonts are used for the Latin families; the IPA
 * families are loaded and embedded from TTF files referenced by {@link Settings}.
 *
 * <p>Loaded TTF fonts are cached per document to avoid repeated embedding.</p>
 */
public class FontResolver {

    private final PDDocument document;
    private final Settings settings;
    private final Map<String, PDFont> ttfCache = new HashMap<>();

    public FontResolver(PDDocument document, Settings settings) {
        this.document = document;
        this.settings = settings;
    }

    public PDFont resolve(String family, boolean bold, boolean italic) throws IOException {
        if (family == null) {
            family = "";
        }
        switch (family) {
            case "Times New Roman":
                return times(bold, italic);
            case "Courier New":
            case "Courier":
                return courier(bold, italic);
            case "IPAGothic":
                return loadTtf(settings.getIpagochic());
            case "IPAMincho":
                return loadTtf(settings.getIpamincho());
            case "IPAPGothic":
                return loadTtf(settings.getIpapgochic());
            case "IPAPMincho":
                return loadTtf(settings.getIpapmincho());
            case "Helvetica":
            case "":
            default:
                return helvetica(bold, italic);
        }
    }

    private PDFont helvetica(boolean bold, boolean italic) {
        if (bold && italic) {
            return new PDType1Font(FontName.HELVETICA_BOLD_OBLIQUE);
        }
        if (bold) {
            return new PDType1Font(FontName.HELVETICA_BOLD);
        }
        if (italic) {
            return new PDType1Font(FontName.HELVETICA_OBLIQUE);
        }
        return new PDType1Font(FontName.HELVETICA);
    }

    private PDFont times(boolean bold, boolean italic) {
        if (bold && italic) {
            return new PDType1Font(FontName.TIMES_BOLD_ITALIC);
        }
        if (bold) {
            return new PDType1Font(FontName.TIMES_BOLD);
        }
        if (italic) {
            return new PDType1Font(FontName.TIMES_ITALIC);
        }
        return new PDType1Font(FontName.TIMES_ROMAN);
    }

    private PDFont courier(boolean bold, boolean italic) {
        if (bold && italic) {
            return new PDType1Font(FontName.COURIER_BOLD_OBLIQUE);
        }
        if (bold) {
            return new PDType1Font(FontName.COURIER_BOLD);
        }
        if (italic) {
            return new PDType1Font(FontName.COURIER_OBLIQUE);
        }
        return new PDType1Font(FontName.COURIER);
    }

    private PDFont loadTtf(String path) throws IOException {
        PDFont cached = ttfCache.get(path);
        if (cached != null) {
            return cached;
        }
        File file = new File(path);
        if (!file.exists()) {
            // Fall back to Helvetica so a missing CJK font does not abort the
            // whole render; text may show as "?" for non-Latin glyphs.
            return new PDType1Font(FontName.HELVETICA);
        }
        PDFont font = PDType0Font.load(document, file);
        ttfCache.put(path, font);
        return font;
    }
}
