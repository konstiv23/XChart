/**
 * Copyright 2015-2017 Knowm Inc. (http://knowm.org) and contributors.
 * Copyright 2011-2015 Xeiam LLC (http://xeiam.com) and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.knowm.xchart.internal.chartpart;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.knowm.xchart.internal.series.AxesChartSeries;
import org.knowm.xchart.internal.series.Series;
import org.knowm.xchart.style.AxesChartStyler;
import org.knowm.xchart.style.CategoryStyler;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.knowm.xchart.style.Styler.YAxisPosition;

/**
 * Axis
 *
 * @author timmolter
 */
public class Axis<ST extends AxesChartStyler, S extends Series> implements ChartPart {

  public enum AxisDataType {

    Number, Date, String
  }

  private final Chart<AxesChartStyler, AxesChartSeries> chart;
  private final Rectangle2D.Double bounds;

  private final AxesChartStyler stylerAxesChart;

  /**
   * the axisDataType
   */
  private AxisDataType axisDataType;

  /**
   * the axis title
   */
  private final AxisTitle<AxesChartStyler, AxesChartSeries> axisTitle;

  /**
   * the axis tick
   */
  private final AxisTick<AxesChartStyler, AxesChartSeries> axisTick;

  /**
   * the axis tick calculator
   */
  private AxisTickCalculator_ axisTickCalculator;

  /**
   * the axis direction
   */
  private final Direction direction;

  private double min;

  private double max;

  private final int yIndex;

  /**
   * An axis direction
   */
  public enum Direction {

    /**
     * the constant to represent X axis
     */
    X,

    /**
     * the constant to represent Y axis
     */
    Y
  }

  /**
   * Constructor
   *
   * @param chart the Chart
   * @param direction the axis direction (X or Y)
   */
  public Axis(Chart<AxesChartStyler, AxesChartSeries> chart, Direction direction, int yIndex) {

    this.chart = chart;
    this.stylerAxesChart = chart.getStyler();

    this.direction = direction;
    this.yIndex = yIndex;
    bounds = new Rectangle2D.Double();
    axisTitle = new AxisTitle<AxesChartStyler, AxesChartSeries>(chart, direction, direction == Direction.Y ? this : null, yIndex);
    axisTick = new AxisTick<AxesChartStyler, AxesChartSeries>(chart, direction, direction == Direction.Y ? this : null);
  }

  /**
   * Reset the default min and max values in preparation for calculating the actual min and max
   */
  void resetMinMax() {

    min = Double.MAX_VALUE;
    max = -1 * Double.MAX_VALUE;
  }

  /**
   * @param min
   * @param max
   */
  void addMinMax(double min, double max) {

    // System.out.println(min);
    // System.out.println(max);
    // NaN indicates String axis data, so min and max play no role
    if (this.min == Double.NaN || min < this.min) {
      this.min = min;
    }
    if (this.max == Double.NaN || max > this.max) {
      this.max = max;
    }

    // System.out.println(this.min);
    // System.out.println(this.max);
  }

  public void preparePaint() {

    // determine Axis bounds
    if (direction == Direction.Y) { // Y-Axis - gets called first

      // calculate paint zone
      // ----
      // |
      // |
      // |
      // |
      // ----
      //double xOffset = chart.getAxisPair().getYAxisXOffset();
      double xOffset = 0; // this will be updated on AxisPair.paint() method 
      // double yOffset = chart.getChartTitle().getBounds().getHeight() < .1 ? axesChartStyler.getChartPadding() : chart.getChartTitle().getBounds().getHeight()
      // + axesChartStyler.getChartPadding();
      double yOffset = chart.getChartTitle().getBounds().getHeight() + stylerAxesChart.getChartPadding();

      /////////////////////////
      int i = 1; // just twice through is all it takes
      double width = 60; // arbitrary, final width depends on Axis tick labels
      double height;
      do {
        // System.out.println("width before: " + width);

        double approximateXAxisWidth =

            chart.getWidth()

                - width // y-axis approx. width

                - (stylerAxesChart.getLegendPosition() == LegendPosition.OutsideE ? chart.getLegend().getBounds().getWidth() : 0)

                - 2 * stylerAxesChart.getChartPadding()

                - (stylerAxesChart.isYAxisTicksVisible() ? (stylerAxesChart.getPlotMargin()) : 0)

                - (stylerAxesChart.getLegendPosition() == LegendPosition.OutsideE && stylerAxesChart.isLegendVisible() ? stylerAxesChart.getChartPadding() : 0);

        height =
            chart.getHeight()

                - yOffset

                - chart.getXAxis().getXAxisHeightHint(approximateXAxisWidth)

                - stylerAxesChart.getPlotMargin()

                - stylerAxesChart.getChartPadding()

                - (stylerAxesChart.getLegendPosition() == LegendPosition.OutsideS ? chart.getLegend().getBounds().getHeight() : 0);

        width = getYAxisWidthHint(height);
        // System.out.println("width after: " + width);

        // System.out.println("height: " + height);

      } while (i-- > 0);

      /////////////////////////

      //bounds = new Rectangle2D.Double(xOffset, yOffset, width, height);
      bounds.setRect(xOffset, yOffset, width, height);

    } else { // X-Axis

      // calculate paint zone
      // |____________________|

      Rectangle2D leftYAxisBounds = chart.getAxisPair().getLeftYAxisBounds();
      Rectangle2D rightYAxisBounds = chart.getAxisPair().getRightYAxisBounds();

      double maxYAxisY = Math.max(leftYAxisBounds.getY() + leftYAxisBounds.getHeight(), rightYAxisBounds.getY() + rightYAxisBounds.getHeight());
      double xOffset = leftYAxisBounds.getWidth() + stylerAxesChart.getChartPadding();
      double yOffset = maxYAxisY + stylerAxesChart.getPlotMargin() - (stylerAxesChart.getLegendPosition() == LegendPosition.OutsideS ? chart.getLegend().getBounds().getHeight() : 0);

      double legendWidth = 0;
      if (stylerAxesChart.getLegendPosition() == LegendPosition.OutsideE && stylerAxesChart.isLegendVisible()) {
        legendWidth = chart.getLegend().getBounds().getWidth() + stylerAxesChart.getChartPadding();
      }
      double width =

          chart.getWidth()

              - leftYAxisBounds.getWidth() // y-axis was already painted

              - rightYAxisBounds.getWidth() // y-axis was already painted

              - 2 * stylerAxesChart.getChartPadding()

              //- tickMargin is included in left & right y axis bounds

              - legendWidth;

      // double height = this.getXAxisHeightHint(width);
      // System.out.println("height: " + height);
      // the Y-Axis was already draw at this point so we know how much vertical room is left for the X-Axis
      double height = chart.getHeight() - maxYAxisY - stylerAxesChart.getChartPadding() - stylerAxesChart.getPlotMargin();
      // System.out.println("height2: " + height2);

      bounds.setRect(xOffset, yOffset, width, height);
    }
  }

  @Override
  public void paint(Graphics2D g) {

    Object oldHint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // determine Axis bounds
    if (direction == Direction.Y) { // Y-Axis - gets called first

      /////////////////////////

      // fill in Axis with sub-components
      boolean onRight = stylerAxesChart.getYAxisGroupPosistion(yIndex) == YAxisPosition.Right;
      if (onRight) {
        axisTick.paint(g);
        axisTitle.paint(g);
      } else {
        axisTitle.paint(g);
        axisTick.paint(g);
      }

      // now we know the real bounds width after ticks and title are painted
      double width = (stylerAxesChart.isYAxisTitleVisible() ? axisTitle.getBounds().getWidth() : 0) + axisTick.getBounds().getWidth();

      bounds.width = width;
      // g.setColor(Color.yellow);
      // g.draw(bounds);

    } else { // X-Axis

      // calculate paint zone
      // |____________________|

      // g.setColor(Color.yellow);
      // g.draw(bounds);

      // now paint the X-Axis given the above paint zone
      this.axisTickCalculator = getAxisTickCalculator(bounds.getWidth());
      axisTitle.paint(g);
      axisTick.paint(g);
    }

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldHint);
  }

  /**
   * The vertical Y-Axis is drawn first, but to know the lower bounds of it, we need to know how high the X-Axis paint zone is going to be. Since the tick labels could be rotated, we need to actually
   * determine the tick labels first to get an idea of how tall the X-Axis tick labels will be.
   *
   * @return
   */
  private double getXAxisHeightHint(double workingSpace) {

    // Axis title
    double titleHeight = 0.0;
    if (chart.getXAxisTitle() != null && !chart.getXAxisTitle().trim().equalsIgnoreCase("") && stylerAxesChart.isXAxisTitleVisible()) {
      TextLayout textLayout = new TextLayout(chart.getXAxisTitle(), stylerAxesChart.getAxisTitleFont(), new FontRenderContext(null, true, false));
      Rectangle2D rectangle = textLayout.getBounds();
      titleHeight = rectangle.getHeight() + stylerAxesChart.getAxisTitlePadding();
    }

    this.axisTickCalculator = getAxisTickCalculator(workingSpace);

    // Axis tick labels
    double axisTickLabelsHeight = 0.0;
    if (stylerAxesChart.isXAxisTicksVisible()) {

      // get some real tick labels
      // System.out.println("XAxisHeightHint");
      // System.out.println("workingSpace: " + workingSpace);

      String sampleLabel = "";
      // find the longest String in all the labels
      for (int i = 0; i < axisTickCalculator.getTickLabels().size(); i++) {
        // System.out.println("label: " + axisTickCalculator.getTickLabels().get(i));
        if (axisTickCalculator.getTickLabels().get(i) != null && axisTickCalculator.getTickLabels().get(i).length() > sampleLabel.length()) {
          sampleLabel = axisTickCalculator.getTickLabels().get(i);
        }
      }
      // System.out.println("sampleLabel: " + sampleLabel);

      // get the height of the label including rotation
      TextLayout textLayout = new TextLayout(sampleLabel.length() == 0 ? " " : sampleLabel, stylerAxesChart.getAxisTickLabelsFont(), new FontRenderContext(null, true, false));
      AffineTransform rot = stylerAxesChart.getXAxisLabelRotation() == 0 ? null : AffineTransform.getRotateInstance(-1 * Math.toRadians(stylerAxesChart.getXAxisLabelRotation()));
      Shape shape = textLayout.getOutline(rot);
      Rectangle2D rectangle = shape.getBounds();

      axisTickLabelsHeight = rectangle.getHeight() + stylerAxesChart.getAxisTickPadding() + stylerAxesChart.getAxisTickMarkLength();
    }
    return titleHeight + axisTickLabelsHeight;
  }

  private double getYAxisWidthHint(double workingSpace) {

    // Axis title
    double titleHeight = 0.0;
    String yAxisTitle = chart.getYAxisGroupTitle(yIndex);
    if (yAxisTitle != null && !yAxisTitle.trim().equalsIgnoreCase("") && stylerAxesChart.isYAxisTitleVisible()) {
      TextLayout textLayout = new TextLayout(yAxisTitle, stylerAxesChart.getAxisTitleFont(), new FontRenderContext(null, true, false));
      Rectangle2D rectangle = textLayout.getBounds();
      titleHeight = rectangle.getHeight() + stylerAxesChart.getAxisTitlePadding();
    }

    this.axisTickCalculator = getAxisTickCalculator(workingSpace);

    // Axis tick labels
    double axisTickLabelsHeight = 0.0;
    if (stylerAxesChart.isYAxisTicksVisible()) {

      // get some real tick labels
      // System.out.println("XAxisHeightHint");
      // System.out.println("workingSpace: " + workingSpace);

      String sampleLabel = "";
      // find the longest String in all the labels
      for (int i = 0; i < axisTickCalculator.getTickLabels().size(); i++) {
        if (axisTickCalculator.getTickLabels().get(i) != null && axisTickCalculator.getTickLabels().get(i).length() > sampleLabel.length()) {
          sampleLabel = axisTickCalculator.getTickLabels().get(i);
        }
      }

      // get the height of the label including rotation
      TextLayout textLayout = new TextLayout(sampleLabel.length() == 0 ? " " : sampleLabel, stylerAxesChart.getAxisTickLabelsFont(), new FontRenderContext(null, true, false));
      Rectangle2D rectangle = textLayout.getBounds();

      axisTickLabelsHeight = rectangle.getWidth() + stylerAxesChart.getAxisTickPadding() + stylerAxesChart.getAxisTickMarkLength();
    }
    return titleHeight + axisTickLabelsHeight;
  }

  private AxisTickCalculator_ getAxisTickCalculator(double workingSpace) {

    // X-Axis
    if (getDirection() == Direction.X) {

      if (stylerAxesChart instanceof CategoryStyler) {

        List<?> categories = (List<?>) chart.getSeriesMap().values().iterator().next().getXData();
        AxisDataType axisType = chart.getAxisPair().getXAxis().getAxisDataType();

        return new AxisTickCalculator_Category(getDirection(), workingSpace, categories, axisType, stylerAxesChart);
      } else if (getAxisDataType() == AxisDataType.Date) {

        return new AxisTickCalculator_Date(getDirection(), workingSpace, min, max, stylerAxesChart);
      } else if (stylerAxesChart.isXAxisLogarithmic()) {

        return new AxisTickCalculator_Logarithmic(getDirection(), workingSpace, min, max, stylerAxesChart);
      } else {
        return new AxisTickCalculator_Number(getDirection(), workingSpace, min, max, stylerAxesChart);
      }
    }

    // Y-Axis
    else {

      if (stylerAxesChart.isYAxisLogarithmic() && getAxisDataType() != AxisDataType.Date) {

        return new AxisTickCalculator_Logarithmic(getDirection(), workingSpace, min, max, stylerAxesChart);
      } else {
        return new AxisTickCalculator_Number(getDirection(), workingSpace, min, max, stylerAxesChart);
      }
    }
  }

  // Getters /////////////////////////////////////////////////

  AxisDataType getAxisDataType() {

    return axisDataType;
  }

  public void setAxisDataType(AxisDataType axisDataType) {

    if (axisDataType != null && this.axisDataType != null && this.axisDataType != axisDataType) {
      throw new IllegalArgumentException("Different Axes (e.g. Date, Number, String) cannot be mixed on the same chart!!");
    }
    this.axisDataType = axisDataType;
  }

  double getMin() {

    return min;
  }

  void setMin(double min) {

    this.min = min;
  }

  double getMax() {

    return max;
  }

  void setMax(double max) {

    this.max = max;
  }

  AxisTick<AxesChartStyler, AxesChartSeries> getAxisTick() {

    return axisTick;
  }

  private Direction getDirection() {

    return direction;
  }

  AxisTitle<AxesChartStyler, AxesChartSeries> getAxisTitle() {

    return axisTitle;
  }

  public AxisTickCalculator_ getAxisTickCalculator() {

    return this.axisTickCalculator;
  }

  @Override
  public Rectangle2D getBounds() {

    return bounds;
  }

  public int getYIndex() {

    return yIndex;
  }
}
