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
package com.github.naofum.thinreports;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.json.JSONObject;

import com.github.naofum.thinreports.font.FontResolver;
import com.github.naofum.thinreports.format.ValueResolver;
import com.github.naofum.thinreports.layout.PageContext;
import com.github.naofum.thinreports.model.Item;
import com.github.naofum.thinreports.model.ListItem;
import com.github.naofum.thinreports.model.Report;
import com.github.naofum.thinreports.render.ImageRenderer;
import com.github.naofum.thinreports.render.ItemRenderer;
import com.github.naofum.thinreports.render.ListRenderer;
import com.github.naofum.thinreports.render.TextRenderer;

/**
 * Entry point for generating an overlay-style PDF from a Thinreports layout
 * file (.tlf) and a map of data values.
 *
 * <p>This implementation draws directly against the Apache PDFBox low-level API
 * (no external flow-layout engine). Each tlf element is placed by its absolute
 * coordinates.</p>
 */
public class TRGenerator {

    private PDDocument document;
    private Settings settings = new Settings();

    /** Detail rows collected per list id, applied on the next render/save. */
    private final Map<String, List<Map<String, Object>>> listRows = new HashMap<>();

    /** Footer (grand total) values per list id. */
    private final Map<String, Map<String, Object>> footerValues = new HashMap<>();

    /** Page-footer values per list id (applied to every page-footer band). */
    private final Map<String, Map<String, Object>> pageFooterValues = new HashMap<>();

    /** Per-page page-footer callbacks per list id (override static values). */
    private final Map<String, ListRenderer.PageFooterCallback> pageFooterCallbacks = new HashMap<>();

    /** Footer (grand total) callbacks per list id (override static values). */
    private final Map<String, ListRenderer.FooterCallback> footerCallbacks = new HashMap<>();

    /** Page render requests captured until save(). */
    private final List<PageDef> pages = new ArrayList<>();

    /** page-number placements recorded during pass 1, drawn in pass 2. */
    private final List<PageNumberPlacement> pageNumbers = new ArrayList<>();

    /** Guards against rendering twice if save() is called repeatedly. */
    private boolean rendered = false;

    /** A page-number element bound to a specific generated page. */
    private static final class PageNumberPlacement {
        final PDPage page;
        final Item item;
        final PageContext ctx;
        final int pageIndex; // 0-based index among all generated pages

        PageNumberPlacement(PDPage page, Item item, PageContext ctx, int pageIndex) {
            this.page = page;
            this.item = item;
            this.ctx = ctx;
            this.pageIndex = pageIndex;
        }
    }

    /**
     * Add a detail row to the default list. The row map is keyed by the child
     * item ids inside the list's detail band.
     */
    public void addRow(Map<String, Object> row) {
        addRow("default", row);
    }

    /**
     * Add a detail row to the list identified by {@code listId}.
     */
    public void addRow(String listId, Map<String, Object> row) {
        listRows.computeIfAbsent(listId, k -> new ArrayList<>()).add(new HashMap<>(row));
    }

    /** Set footer (grand total) values for the default list. */
    public void setFooterValues(Map<String, Object> values) {
        setFooterValues("default", values);
    }

    /** Set footer (grand total) values for the given list. */
    public void setFooterValues(String listId, Map<String, Object> values) {
        footerValues.put(listId, new HashMap<>(values));
    }

    /** Set page-footer values for the default list (applied to every page). */
    public void setPageFooterValues(Map<String, Object> values) {
        setPageFooterValues("default", values);
    }

    /** Set page-footer values for the given list (applied to every page). */
    public void setPageFooterValues(String listId, Map<String, Object> values) {
        pageFooterValues.put(listId, new HashMap<>(values));
    }

    /**
     * Register a per-page page-footer callback for the default list. The
     * callback is invoked once per page just before its page-footer is drawn,
     * receiving the 1-based page number (within the list) and that page's detail
     * rows; the returned values override any static page-footer values.
     */
    public void setPageFooterCallback(ListRenderer.PageFooterCallback callback) {
        setPageFooterCallback("default", callback);
    }

    /** Register a per-page page-footer callback for the given list. */
    public void setPageFooterCallback(String listId, ListRenderer.PageFooterCallback callback) {
        pageFooterCallbacks.put(listId, callback);
    }

    /**
     * Register a footer (grand total) callback for the default list. The
     * callback is invoked once, just before the footer band is drawn on the
     * final page, receiving all detail rows; the returned values override any
     * static footer values.
     */
    public void setFooterCallback(ListRenderer.FooterCallback callback) {
        setFooterCallback("default", callback);
    }

    /** Register a footer (grand total) callback for the given list. */
    public void setFooterCallback(String listId, ListRenderer.FooterCallback callback) {
        footerCallbacks.put(listId, callback);
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public PDDocument getDocument() {
        return document;
    }

    // ---- newDocument overloads -------------------------------------------------

    public void newDocument(String tlffile, Map<String, Object> map) throws IOException {
        newDocument(readTlf(tlffile), map);
    }

    public void newDocument(JSONObject json, Map<String, Object> map) throws IOException {
        this.document = new PDDocument();
        this.pages.clear();
        this.listRows.clear();
        this.footerValues.clear();
        this.pageFooterValues.clear();
        this.pageFooterCallbacks.clear();
        this.footerCallbacks.clear();
        this.pageNumbers.clear();
        this.rendered = false;
        this.pages.add(new PageDef(json, map));
    }

    public void addPage(String tlffile, Map<String, Object> map) throws IOException {
        addPage(readTlf(tlffile), map);
    }

    public void addPage(JSONObject json, Map<String, Object> map) throws IOException {
        if (document == null) {
            newDocument(json, map);
            return;
        }
        pages.add(new PageDef(json, map));
    }

    /** A page render request captured until {@link #save(String)}. */
    private static final class PageDef {
        final JSONObject json;
        final Map<String, Object> map;

        PageDef(JSONObject json, Map<String, Object> map) {
            this.json = json;
            this.map = map;
        }
    }

    // ---- rendering -------------------------------------------------------------

    private void renderAll() throws IOException {
        int pageIndexBase = 0;
        for (PageDef def : pages) {
            pageIndexBase = renderPage(def.json, def.map, pageIndexBase);
        }
        // Pass 2: draw page numbers now that the total page count is known.
        drawPageNumbers();
        if (!pages.isEmpty()) {
            applyDocumentInformation(pages.get(0).json);
        }
    }

    /**
     * Render one page definition. Returns the running page index after any
     * physical pages (including list page-breaks) produced here.
     */
    private int renderPage(JSONObject json, Map<String, Object> map, int pageIndexBase)
            throws IOException {
        Report report = new Report(json);
        PageContext ctx = new PageContext(report);
        final PDPage[] pageHolder = { new PDPage(ctx.getMediaBox()) };
        document.addPage(pageHolder[0]);

        // Collect page-number elements to place on every physical page produced.
        final List<Item> pageNumberItems = new ArrayList<>();
        for (Item it : report.getItems()) {
            if (it.isDisplay() && "page-number".equals(it.getType())) {
                pageNumberItems.add(it);
            }
        }
        final int[] physicalCount = { 1 };
        recordPageNumbers(pageNumberItems, pageHolder[0], ctx, pageIndexBase);

        FontResolver fontResolver = new FontResolver(document, settings);
        TextRenderer textRenderer = new TextRenderer(fontResolver);
        ImageRenderer imageRenderer = new ImageRenderer(document);
        ItemRenderer itemRenderer = new ItemRenderer(textRenderer, imageRenderer);
        ListRenderer listRenderer = new ListRenderer(itemRenderer);

        PDPageContentStream cs = new PDPageContentStream(document, pageHolder[0]);
        try {
            for (Item item : report.getItems()) {
                if (!item.isDisplay()) {
                    continue;
                }
                String type = item.getType();
                if ("page-number".equals(type)) {
                    continue; // drawn in pass 2
                }
                if ("list".equals(type)) {
                    ListItem list = new ListItem(item);
                    List<Map<String, Object>> rows = listRows.get(list.getId());
                    Map<String, Object> pfValues = pageFooterValues.get(list.getId());
                    Map<String, Object> fValues = footerValues.get(list.getId());
                    ListRenderer.PageFooterCallback pfCallback = pageFooterCallbacks.get(list.getId());
                    ListRenderer.FooterCallback fCallback = footerCallbacks.get(list.getId());
                    final PageContext fctx = ctx;
                    final PDPageContentStream[] csRef = { cs };
                    cs = listRenderer.render(cs, ctx, list, rows, pfValues, fValues, pfCallback,
                            fCallback, () -> {
                        csRef[0].close();
                        PDPage p = new PDPage(fctx.getMediaBox());
                        document.addPage(p);
                        recordPageNumbers(pageNumberItems, p, fctx, pageIndexBase + physicalCount[0]);
                        physicalCount[0]++;
                        csRef[0] = new PDPageContentStream(document, p);
                        return csRef[0];
                    });
                } else {
                    itemRenderer.render(cs, ctx, item, map);
                }
            }
        } finally {
            cs.close();
        }
        return pageIndexBase + physicalCount[0];
    }

    private void recordPageNumbers(List<Item> items, PDPage page, PageContext ctx, int pageIndex) {
        for (Item it : items) {
            pageNumbers.add(new PageNumberPlacement(page, it, ctx, pageIndex));
        }
    }

    private void drawPageNumbers() throws IOException {
        if (pageNumbers.isEmpty()) {
            return;
        }
        int total = document.getNumberOfPages();
        FontResolver fontResolver = new FontResolver(document, settings);
        TextRenderer textRenderer = new TextRenderer(fontResolver);
        for (PageNumberPlacement pn : pageNumbers) {
            String format = pn.item.raw().optString("format", "{page}");
            String text = format
                    .replace("{page}", String.valueOf(pn.pageIndex + 1))
                    .replace("{total}", String.valueOf(total));
            try (PDPageContentStream cs =
                    new PDPageContentStream(document, pn.page,
                            PDPageContentStream.AppendMode.APPEND, true, true)) {
                textRenderer.render(cs, pn.ctx, pn.item, java.util.List.of(text));
            }
        }
    }

    // ---- metadata & security ---------------------------------------------------

    private void applyDocumentInformation(JSONObject json) throws IOException {
        PDDocumentInformation info = document.getDocumentInformation();
        info.setProducer("Created by thinreports-java.");
        info.setCreationDate(Calendar.getInstance());

        JSONObject settingsJson = json.optJSONObject("settings");
        if (settingsJson == null) {
            return;
        }
        info.setTitle(settingsJson.optString("title", null));
        info.setCreator(settingsJson.optString("creator", null));
        info.setAuthor(settingsJson.optString("author", null));

        JSONObject security = settingsJson.optJSONObject("security_settings");
        if (security != null) {
            applySecurity(security);
        }
    }

    private void applySecurity(JSONObject security) throws IOException {
        String userPassword = security.optString("user_password", "");
        String ownerPassword = security.optString("owner_password", "");
        JSONObject permissions = security.optJSONObject("permissions");

        AccessPermission ap = new AccessPermission();
        if (permissions != null) {
            if (permissions.has("print_document")) {
                ap.setCanPrint(permissions.optBoolean("print_document"));
            }
            if (permissions.has("modify_contents")) {
                ap.setCanModify(permissions.optBoolean("modify_contents"));
            }
            if (permissions.has("copy_contents")) {
                ap.setCanExtractContent(permissions.optBoolean("copy_contents"));
            }
        }
        StandardProtectionPolicy policy =
                new StandardProtectionPolicy(ownerPassword, userPassword, ap);
        policy.setEncryptionKeyLength(128);
        policy.setPermissions(ap);
        document.protect(policy);
    }

    // ---- output ----------------------------------------------------------------

    public void save(String filename) throws IOException {
        if (!rendered) {
            renderAll();
            rendered = true;
        }
        document.save(new File(filename));
    }

    public void close() throws IOException {
        if (document != null) {
            document.close();
            document = null;
        }
    }

    private JSONObject readTlf(String file) throws IOException {
        String content = Files.readString(new File(file).toPath(), StandardCharsets.UTF_8);
        return new JSONObject(content);
    }
}
