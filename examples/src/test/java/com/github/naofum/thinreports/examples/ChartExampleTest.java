package com.github.naofum.thinreports.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.naofum.thinreports.TRGenerator;

class ChartExampleTest {

    @TempDir
    File tempDir;

    private String resource(String name) {
        return new File("src/test/resources/" + name).getPath();
    }

    @Test
    void generatesChartsIntoPdf() throws Exception {
        DefaultCategoryDataset cat = new DefaultCategoryDataset();
        cat.setValue(120, "A", "3");
        cat.setValue(130, "A", "4");
        cat.setValue(250, "B", "3");
        cat.setValue(190, "B", "4");

        DefaultPieDataset<String> pie = new DefaultPieDataset<>();
        pie.setValue("A", 37);
        pie.setValue("B", 31);
        pie.setValue("C", 32);

        BufferedImage bar = ChartGenerators.bar(cat, 240, 140);
        BufferedImage line = ChartGenerators.line(cat, 240, 140);
        BufferedImage pieImg = ChartGenerators.pie(pie, 240, 140);
        assertTrue(bar.getWidth() > 0 && line.getWidth() > 0 && pieImg.getWidth() > 0,
                "chart images should be generated");

        Map<String, Object> map = new HashMap<>();
        map.put("bar_chart", bar);
        map.put("line_chart", line);
        map.put("pie_chart", pieImg);

        TRGenerator generator = new TRGenerator();
        generator.newDocument(resource("chart.tlf"), map);
        File out = new File(tempDir, "chart.pdf");
        generator.save(out.getPath());
        generator.close();

        assertTrue(out.exists() && out.length() > 0, "chart PDF should be created");
        try (PDDocument doc = Loader.loadPDF(out)) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }
}
