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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import com.github.naofum.thinreports.layout.PageContext;
import com.github.naofum.thinreports.model.Item;
import com.github.naofum.thinreports.model.ListBand;
import com.github.naofum.thinreports.model.ListItem;

/**
 * Renders a {@code list} element: the header band once per page, the detail
 * band repeated for each data row (each shifted down by the detail height),
 * and optional page-footer / footer bands, with automatic page breaks.
 *
 * <p>The renderer draws only within the current page's content stream. Page
 * breaks are signalled back to the caller through {@link PageBreakCallback} so
 * the orchestrator can create a fresh page and content stream.</p>
 */
public class ListRenderer {

    /** Requests a new page and returns the content stream to draw on. */
    public interface PageBreakCallback {
        PDPageContentStream newPage() throws IOException;
    }

    /**
     * Computes per-page page-footer values. Invoked once for each page just
     * before its page-footer band is drawn, receiving the 1-based page number
     * (within this list) and the detail rows that were placed on that page.
     * The returned map (may be null) is merged over any static page-footer
     * values, so callbacks can override or add fields such as a page subtotal.
     */
    public interface PageFooterCallback {
        Map<String, Object> valuesFor(int pageNumber, List<Map<String, Object>> pageRows);
    }

    /**
     * Computes footer (grand total) values. Invoked once, just before the footer
     * band is drawn on the final page, receiving all detail rows for the list.
     * The returned map (may be null) is merged over any static footer values,
     * so callbacks can override or add fields such as a grand total.
     */
    public interface FooterCallback {
        Map<String, Object> valuesFor(List<Map<String, Object>> allRows);
    }

    private final ItemRenderer itemRenderer;

    public ListRenderer(ItemRenderer itemRenderer) {
        this.itemRenderer = itemRenderer;
    }

    /**
     * @param cs               current content stream
     * @param ctx              page geometry
     * @param list             the list element
     * @param rows             detail row data (each map keyed by child item id)
     * @param pageFooterValues static values for the page-footer band (may be null)
     * @param footerValues     values for the footer band (may be null)
     * @param pageBreak        callback to obtain a new page's content stream
     * @return the content stream in effect after rendering (may differ from
     *         {@code cs} if page breaks occurred)
     */
    public PDPageContentStream render(PDPageContentStream cs, PageContext ctx, ListItem list,
            List<Map<String, Object>> rows, Map<String, Object> pageFooterValues,
            Map<String, Object> footerValues, PageBreakCallback pageBreak) throws IOException {
        return render(cs, ctx, list, rows, pageFooterValues, footerValues, null, null, pageBreak);
    }

    /**
     * Render entry point supporting a per-page page-footer callback.
     *
     * @param pageFooterCallback optional callback computing per-page page-footer
     *                           values (merged over {@code pageFooterValues})
     */
    public PDPageContentStream render(PDPageContentStream cs, PageContext ctx, ListItem list,
            List<Map<String, Object>> rows, Map<String, Object> pageFooterValues,
            Map<String, Object> footerValues, PageFooterCallback pageFooterCallback,
            PageBreakCallback pageBreak) throws IOException {
        return render(cs, ctx, list, rows, pageFooterValues, footerValues, pageFooterCallback,
                null, pageBreak);
    }

    /**
     * Full render entry point supporting both a per-page page-footer callback
     * and a footer (grand total) callback.
     *
     * @param pageFooterCallback optional callback computing per-page page-footer
     *                           values (merged over {@code pageFooterValues})
     * @param footerCallback     optional callback computing footer values from
     *                           all rows (merged over {@code footerValues})
     */
    public PDPageContentStream render(PDPageContentStream cs, PageContext ctx, ListItem list,
            List<Map<String, Object>> rows, Map<String, Object> pageFooterValues,
            Map<String, Object> footerValues, PageFooterCallback pageFooterCallback,
            FooterCallback footerCallback, PageBreakCallback pageBreak) throws IOException {
        ListBand header = list.getHeader();
        ListBand detail = list.getDetail();
        ListBand pageFooter = list.getPageFooter();
        ListBand footer = list.getFooter();

        float detailHeight = detail.getHeight();
        // The detail rows may occupy down to the content bottom, but must leave
        // room for the page-footer (drawn at the bottom of every page).
        float pageFooterHeight = pageFooter.isEnabled() ? pageFooter.getHeight() : 0f;
        float contentBottom = list.getY() + list.getContentHeight();
        float detailLimit = contentBottom - pageFooterHeight;
        // The page-footer band sits just above the content bottom.
        float pageFooterTop = contentBottom - pageFooterHeight;

        // Draw header on the first page.
        drawBand(cs, ctx, header, 0f, null);

        // First detail row starts right after the header.
        float startY = list.getY() + (header.isEnabled() ? header.getHeight() : 0f);
        float cursorY = startY;

        int pageNumber = 1;
        java.util.List<Map<String, Object>> pageRows = new java.util.ArrayList<>();

        if (rows != null) {
            for (Map<String, Object> row : rows) {
                if (list.isAutoPageBreak() && cursorY + detailHeight > detailLimit) {
                    // Draw the page-footer at the bottom of the finished page.
                    drawBandAt(cs, ctx, pageFooter, pageFooterTop,
                            pageFooterValuesFor(pageFooterValues, pageFooterCallback, pageNumber, pageRows));
                    // Page break: new page, redraw header, reset cursor and page state.
                    cs = pageBreak.newPage();
                    drawBand(cs, ctx, header, 0f, null);
                    cursorY = startY;
                    pageNumber++;
                    pageRows = new java.util.ArrayList<>();
                }
                float bandTop = firstItemTop(detail, cursorY);
                float offset = cursorY - bandTop;
                drawBand(cs, ctx, detail, offset, row);
                cursorY += detailHeight;
                pageRows.add(row);
            }
        }

        // page-footer on the final page.
        drawBandAt(cs, ctx, pageFooter, pageFooterTop,
                pageFooterValuesFor(pageFooterValues, pageFooterCallback, pageNumber, pageRows));

        // footer (grand total) at the end, after the last detail row.
        if (footer.isEnabled()) {
            drawBandAt(cs, ctx, footer, cursorY,
                    footerValuesFor(footerValues, footerCallback, rows));
        }
        return cs;
    }

    /**
     * Merge static footer values with any callback-provided values computed from
     * all rows. Callback values take precedence.
     */
    private Map<String, Object> footerValuesFor(Map<String, Object> staticValues,
            FooterCallback callback, List<Map<String, Object>> allRows) {
        if (callback == null) {
            return staticValues;
        }
        List<Map<String, Object>> rows = allRows != null ? allRows : java.util.List.of();
        Map<String, Object> dynamic = callback.valuesFor(rows);
        if (dynamic == null) {
            return staticValues;
        }
        Map<String, Object> merged = new java.util.HashMap<>();
        if (staticValues != null) {
            merged.putAll(staticValues);
        }
        merged.putAll(dynamic);
        return merged;
    }

    /**
     * Merge static page-footer values with any callback-provided values for the
     * given page. Callback values take precedence.
     */
    private Map<String, Object> pageFooterValuesFor(Map<String, Object> staticValues,
            PageFooterCallback callback, int pageNumber, List<Map<String, Object>> pageRows) {
        if (callback == null) {
            return staticValues;
        }
        Map<String, Object> dynamic = callback.valuesFor(pageNumber, pageRows);
        if (dynamic == null) {
            return staticValues;
        }
        Map<String, Object> merged = new java.util.HashMap<>();
        if (staticValues != null) {
            merged.putAll(staticValues);
        }
        merged.putAll(dynamic);
        return merged;
    }

    /**
     * Reserved row-map key holding a {@code Set<String>} of item ids to hide
     * for that row. Enables group-rows style layouts (e.g. hiding the group
     * separator line on continuation rows).
     */
    public static final String HIDDEN_IDS_KEY = "__hidden__";

    private void drawBand(PDPageContentStream cs, PageContext ctx, ListBand band, float yOffset,
            Map<String, Object> row) throws IOException {
        if (band == null || !band.isEnabled()) {
            return;
        }
        java.util.Set<?> hidden = null;
        if (row != null && row.get(HIDDEN_IDS_KEY) instanceof java.util.Set<?> s) {
            hidden = s;
        }
        for (Item item : band.getItems()) {
            if (!item.isDisplay()) {
                continue;
            }
            if (hidden != null && !item.getId().isEmpty() && hidden.contains(item.getId())) {
                continue;
            }
            itemRenderer.render(cs, ctx, item, row, yOffset);
        }
    }

    /**
     * Draw a band so that its top edge lands at the given tlf Y coordinate.
     */
    private void drawBandAt(PDPageContentStream cs, PageContext ctx, ListBand band, float targetTop,
            Map<String, Object> row) throws IOException {
        if (band == null || !band.isEnabled() || band.getItems().isEmpty()) {
            return;
        }
        float offset = targetTop - firstItemTop(band, targetTop);
        drawBand(cs, ctx, band, offset, row);
    }

    /**
     * The topmost Y among a band's items, used as the band's reference origin
     * so the whole band can be translated to {@code cursorY}.
     */
    private float firstItemTop(ListBand band, float fallback) {
        float top = Float.MAX_VALUE;
        for (Item item : band.getItems()) {
            float y = item.getType().equals("line") ? Math.min(item.getY1(), item.getY2()) : item.getY();
            if (y < top) {
                top = y;
            }
        }
        return top == Float.MAX_VALUE ? fallback : top;
    }
}
