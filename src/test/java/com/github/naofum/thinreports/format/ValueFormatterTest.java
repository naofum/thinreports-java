package com.github.naofum.thinreports.format;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;
import java.util.Date;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class ValueFormatterTest {

    @Test
    void numberWithDelimiterAndPrecision() {
        JSONObject fmt = new JSONObject()
                .put("type", "number")
                .put("base", "")
                .put("number", new JSONObject().put("delimiter", ",").put("precision", 3));
        assertEquals("99,999.999", ValueFormatter.apply(99999.9994, fmt));
    }

    @Test
    void numberWithBaseTemplate() {
        JSONObject fmt = new JSONObject()
                .put("type", "number")
                .put("base", "\u00a5 {value}")
                .put("number", new JSONObject().put("delimiter", ",").put("precision", 0));
        assertEquals("\u00a5 1,980", ValueFormatter.apply(1980, fmt));
    }

    @Test
    void paddingLeft() {
        JSONObject fmt = new JSONObject()
                .put("type", "padding")
                .put("padding", new JSONObject().put("char", "0").put("length", 10).put("direction", "L"));
        assertEquals("0000000999", ValueFormatter.apply(999, fmt));
    }

    @Test
    void datetimeFormat() {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, Calendar.JULY, 7);
        Date date = cal.getTime();
        JSONObject fmt = new JSONObject()
                .put("type", "datetime")
                .put("datetime", new JSONObject().put("format", "%Y/%m/%d"));
        assertEquals("2015/07/07", ValueFormatter.apply(date, fmt));
    }

    @Test
    void rubyPatternTranslation() {
        assertEquals("yyyy/MM/dd HH:mm:ss",
                ValueFormatter.toJavaDatePattern("%Y/%m/%d %H:%M:%S"));
    }

    @Test
    void rubyPatternWeekdayAndMonthName() {
        assertEquals("EEEE, MMMM d, yyyy",
                ValueFormatter.toJavaDatePattern("%A, %B %e, %Y"));
    }

    @Test
    void rubyPatternTwelveHourAndMeridiem() {
        assertEquals("hh:mm a",
                ValueFormatter.toJavaDatePattern("%I:%M %p"));
    }

    @Test
    void rubyPatternLiteralLettersAreQuoted() {
        // "T" between date and time must be emitted verbatim, not as a field.
        assertEquals("yyyy-MM-dd'T'HH:mm:ss",
                ValueFormatter.toJavaDatePattern("%Y-%m-%dT%H:%M:%S"));
    }

    @Test
    void rubyPatternPercentEscape() {
        assertEquals("yyyy% ",
                ValueFormatter.toJavaDatePattern("%Y%% "));
    }

    @Test
    void datetimeWeekdayEnglishLocale() {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, Calendar.JULY, 7); // Tuesday
        Date date = cal.getTime();
        JSONObject fmt = new JSONObject()
                .put("type", "datetime")
                .put("datetime", new JSONObject().put("format", "%A %b %d, %Y"));
        assertEquals("Tuesday Jul 07, 2015", ValueFormatter.apply(date, fmt));
    }

    @Test
    void noFormatReturnsPlainString() {
        assertEquals("hello", ValueFormatter.apply("hello", null));
    }
}
