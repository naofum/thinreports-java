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
package com.github.naofum.thinreports.layout;

import org.apache.pdfbox.pdmodel.common.PDRectangle;

import com.github.naofum.thinreports.model.Report;

/**
 * Resolves the media box for a page and converts tlf coordinates (top-left
 * origin, points) into PDFBox coordinates (bottom-left origin).
 *
 * <p>Thinreports places every element by absolute coordinates, so no flow
 * layout is performed: this class only provides the geometric mapping.</p>
 */
public class PageContext {

    private final PDRectangle mediaBox;

    public PageContext(Report report) {
        PDRectangle base = paperTypeToRectangle(report.getPaperType());
        if (report.isLandscape()) {
            this.mediaBox = new PDRectangle(base.getHeight(), base.getWidth());
        } else {
            this.mediaBox = base;
        }
    }

    public PDRectangle getMediaBox() {
        return mediaBox;
    }

    public float getPageWidth() {
        return mediaBox.getWidth();
    }

    public float getPageHeight() {
        return mediaBox.getHeight();
    }

    /**
     * Convert a tlf Y coordinate (measured from the top edge downwards) to a
     * PDF Y coordinate (measured from the bottom edge upwards).
     *
     * @param tlfY tlf top-origin Y in points
     * @return PDF bottom-origin Y in points
     */
    public float toPdfY(float tlfY) {
        return getPageHeight() - tlfY;
    }

    /**
     * Convert the top-left corner of a tlf element to the bottom-left corner
     * expected by most PDFBox drawing primitives.
     *
     * @param tlfY   tlf top-origin Y of the element's top edge
     * @param height element height in points
     * @return PDF bottom-origin Y of the element's bottom edge
     */
    public float toPdfYForBox(float tlfY, float height) {
        return getPageHeight() - tlfY - height;
    }

    private static PDRectangle paperTypeToRectangle(String paperType) {
        if (paperType == null) {
            return PDRectangle.A4;
        }
        switch (paperType.toUpperCase(java.util.Locale.ROOT)) {
            case "A0":
                return PDRectangle.A0;
            case "A1":
                return PDRectangle.A1;
            case "A2":
                return PDRectangle.A2;
            case "A3":
                return PDRectangle.A3;
            case "A5":
                return PDRectangle.A5;
            case "A6":
                return PDRectangle.A6;
            // ISO 216 B series (as used by Thinreports), millimetres.
            case "B0":
                return mm(1000f, 1414f);
            case "B1":
                return mm(707f, 1000f);
            case "B2":
                return mm(500f, 707f);
            case "B3":
                return mm(353f, 500f);
            case "B4":
                return mm(250f, 353f);
            case "B5":
                return mm(176f, 250f);
            case "B6":
                return mm(125f, 176f);
            case "LETTER":
                return PDRectangle.LETTER;
            case "LEGAL":
                return PDRectangle.LEGAL;
            case "A4":
            default:
                return PDRectangle.A4;
        }
    }

    /** Build a portrait {@link PDRectangle} from a size given in millimetres. */
    private static PDRectangle mm(float widthMm, float heightMm) {
        float pointsPerMm = 72f / 25.4f;
        return new PDRectangle(widthMm * pointsPerMm, heightMm * pointsPerMm);
    }
}
