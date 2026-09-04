package com.github.naofum.thinreports.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.List;

import org.junit.jupiter.api.Test;

class MarkupParserTest {

    @Test
    void plainTextIsASingleRun() {
        List<TextRun> runs = MarkupParser.parse("hello", null);
        assertEquals(1, runs.size());
        assertEquals("hello", runs.get(0).getText());
        assertFalse(runs.get(0).isBold());
    }

    @Test
    void boldToggle() {
        List<TextRun> runs = MarkupParser.parse("a*b*c", null);
        assertEquals(3, runs.size());
        assertFalse(runs.get(0).isBold());
        assertTrue(runs.get(1).isBold());
        assertEquals("b", runs.get(1).getText());
        assertFalse(runs.get(2).isBold());
    }

    @Test
    void italicToggle() {
        List<TextRun> runs = MarkupParser.parse("_x_y", null);
        assertEquals(2, runs.size());
        assertTrue(runs.get(0).isItalic());
        assertEquals("x", runs.get(0).getText());
        assertFalse(runs.get(1).isItalic());
    }

    @Test
    void underlineDoubleUnderscore() {
        List<TextRun> runs = MarkupParser.parse("__u__t", null);
        assertEquals(2, runs.size());
        assertTrue(runs.get(0).isUnderline());
        assertEquals("u", runs.get(0).getText());
        assertFalse(runs.get(1).isUnderline());
    }

    @Test
    void lineThroughForm() {
        List<TextRun> runs = MarkupParser.parse("__{0.25:}gone__", null);
        assertEquals(1, runs.size());
        assertTrue(runs.get(0).isLinethrough());
        assertEquals("gone", runs.get(0).getText());
    }

    @Test
    void colorChange() {
        List<TextRun> runs = MarkupParser.parse("{color:#ff0000}red", null);
        assertEquals(1, runs.size());
        assertEquals(Color.RED, runs.get(0).getColor());
        assertEquals("red", runs.get(0).getText());
    }

    @Test
    void defaultColorInheritedBeforeMarker() {
        List<TextRun> runs = MarkupParser.parse("*b*", null);
        assertNull(runs.get(0).getColor());
    }
}
