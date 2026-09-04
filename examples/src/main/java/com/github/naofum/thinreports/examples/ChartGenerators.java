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
package com.github.naofum.thinreports.examples;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.PieDataset;

/**
 * Generates chart images with JFreeChart, returning {@link BufferedImage} to be
 * fed into {@code image-block} elements. Chart support is intentionally kept in
 * the examples module rather than the core library.
 */
public final class ChartGenerators {

    private ChartGenerators() {
    }

    public static BufferedImage bar(CategoryDataset data, int width, int height) {
        JFreeChart chart = ChartFactory.createStackedBarChart(
                null, null, null, data, PlotOrientation.HORIZONTAL, false, false, false);
        return whiteBackground(chart).createBufferedImage(width, height);
    }

    public static BufferedImage line(CategoryDataset data, int width, int height) {
        JFreeChart chart = ChartFactory.createLineChart(
                null, null, null, data, PlotOrientation.VERTICAL, false, false, false);
        return whiteBackground(chart).createBufferedImage(width, height);
    }

    public static BufferedImage pie(PieDataset<String> data, int width, int height) {
        JFreeChart chart = ChartFactory.createPieChart("", data, false, false, false);
        return whiteBackground(chart).createBufferedImage(width, height);
    }

    private static JFreeChart whiteBackground(JFreeChart chart) {
        chart.setBorderVisible(false);
        chart.setBackgroundPaint(Color.WHITE);
        chart.getPlot().setBackgroundPaint(Color.WHITE);
        return chart;
    }
}
