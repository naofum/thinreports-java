package com.github.naofum.thinreports.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import com.github.naofum.thinreports.model.Report;

class PageContextTest {

    private static PageContext context(String paperType, String orientation) {
        JSONObject report = new JSONObject()
                .put("paper-type", paperType)
                .put("orientation", orientation);
        JSONObject root = new JSONObject().put("report", report);
        return new PageContext(new Report(root));
    }

    private static float mmToPt(float mm) {
        return mm * 72f / 25.4f;
    }

    @Test
    void b4PortraitDimensions() {
        PageContext ctx = context("B4", "portrait");
        assertEquals(mmToPt(250f), ctx.getPageWidth(), 0.5f);
        assertEquals(mmToPt(353f), ctx.getPageHeight(), 0.5f);
    }

    @Test
    void b5PortraitDimensions() {
        PageContext ctx = context("B5", "portrait");
        assertEquals(mmToPt(176f), ctx.getPageWidth(), 0.5f);
        assertEquals(mmToPt(250f), ctx.getPageHeight(), 0.5f);
    }

    @Test
    void landscapeSwapsWidthAndHeight() {
        PageContext portrait = context("B4", "portrait");
        PageContext landscape = context("B4", "landscape");
        assertEquals(portrait.getPageWidth(), landscape.getPageHeight(), 0.01f);
        assertEquals(portrait.getPageHeight(), landscape.getPageWidth(), 0.01f);
        assertTrue(landscape.getPageWidth() > landscape.getPageHeight());
    }

    @Test
    void paperTypeIsCaseInsensitive() {
        PageContext upper = context("B5", "portrait");
        PageContext lower = context("b5", "portrait");
        assertEquals(upper.getPageWidth(), lower.getPageWidth(), 0.01f);
        assertEquals(upper.getPageHeight(), lower.getPageHeight(), 0.01f);
    }

    @Test
    void unknownPaperTypeFallsBackToA4() {
        PageContext ctx = context("UNKNOWN", "portrait");
        // A4 is 210 x 297 mm.
        assertEquals(mmToPt(210f), ctx.getPageWidth(), 0.5f);
        assertEquals(mmToPt(297f), ctx.getPageHeight(), 0.5f);
    }

    @Test
    void toPdfYInvertsFromTop() {
        PageContext ctx = context("A4", "portrait");
        float h = ctx.getPageHeight();
        assertEquals(h - 100f, ctx.toPdfY(100f), 0.01f);
        assertEquals(h - 100f - 20f, ctx.toPdfYForBox(100f, 20f), 0.01f);
    }
}
