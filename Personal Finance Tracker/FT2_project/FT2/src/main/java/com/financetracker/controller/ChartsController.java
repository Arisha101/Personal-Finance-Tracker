package com.financetracker.controller;

import com.financetracker.model.AppState;
import com.financetracker.model.Transaction;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.net.URL;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

public class ChartsController implements Initializable {

    @FXML private Canvas barCanvas;
    @FXML private HBox   barLegend;
    @FXML private Label  catTitle;
    @FXML private VBox   catRows;

    // Named colours used for the category breakdown chart
    private static final String[] CHART_COLORS = {
        "#7C3AED", "#059669", "#DC2626", "#D97706",
        "#0EA5E9", "#EC4899", "#14B8A6", "#F59E0B"
    };
    private static final String[] COLOR_NAMES = {
        "Violet", "Emerald", "Crimson", "Amber",
        "Sky Blue", "Pink", "Teal", "Gold"
    };

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Build legend and category breakdown immediately
        buildBarLegend();
        buildCategoryBreakdown();

        // Draw bar chart AFTER the layout pass so the canvas has its real width/height.
        // Platform.runLater delays execution until the JavaFX scene is fully laid out.
        Platform.runLater(this::drawBarChart);
    }

    // ── 6-Month Income vs Expense bar chart ───────────────────────
    private void drawBarChart() {
        GraphicsContext gc = barCanvas.getGraphicsContext2D();
        double w      = barCanvas.getWidth();
        double h      = barCanvas.getHeight();
        double chartH = h - 40; // leave 40px at the bottom for month labels

        // ── Collect income and expense totals for the last 6 months ──
        double[] incomes  = new double[6];
        double[] expenses = new double[6];
        String[] labels   = new String[6];
        LocalDate now = LocalDate.now();

        for (int i = 5; i >= 0; i--) {
            LocalDate d   = now.minusMonths(i);
            int idx       = 5 - i; // index 0 = oldest, 5 = current month
            List<Transaction> list = AppState.get().getByMonth(d.getMonthValue(), d.getYear());
            incomes[idx]  = AppState.get().totalIncome(list);
            expenses[idx] = AppState.get().totalExpense(list);
            labels[idx]   = d.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        }

        // Find the maximum value for scaling bars
        double maxVal = 0;
        for (int i = 0; i < 6; i++) maxVal = Math.max(maxVal, Math.max(incomes[i], expenses[i]));
        if (maxVal == 0) maxVal = 1000; // avoid division by zero when no data

        // ── Clear canvas before drawing ──
        gc.clearRect(0, 0, w, h);

        double leftPad = 50; // space on left for Y-axis labels
        double chartW  = w - leftPad - 10;
        double groupW  = chartW / 6.0;
        double barW    = groupW / 2.0 - 6; // each group has 2 bars + small gap

        // ── Draw horizontal grid lines ──
        gc.setStroke(Color.web("#E5E7EB"));
        gc.setLineWidth(1);
        for (int g = 0; g <= 4; g++) {
            double y = 10 + (chartH - 10) * g / 4.0;
            gc.strokeLine(leftPad, y, w - 10, y);
        }

        // ── Draw bars ──
        for (int i = 0; i < 6; i++) {
            double groupX = leftPad + i * groupW;

            // Income bar height (scaled to chart area)
            double incH = (incomes[i] / maxVal) * (chartH - 20);
            // Expense bar height
            double expH = (expenses[i] / maxVal) * (chartH - 20);

            // Income bar — Emerald Green (draw even if 0, so the label is visible)
            if (incomes[i] > 0) {
                gc.setFill(Color.web("#059669", 0.85));
                gc.fillRoundRect(groupX + 3, chartH - incH + 10, barW, incH, 4, 4);
            }

            // Expense bar — Crimson Red
            if (expenses[i] > 0) {
                gc.setFill(Color.web("#DC2626", 0.85));
                gc.fillRoundRect(groupX + barW + 8, chartH - expH + 10, barW, expH, 4, 4);
            }

            // Month label below the bars
            gc.setFill(Color.web("#6B7280"));
            gc.setFont(Font.font(10));
            gc.fillText(labels[i], groupX + 6, h - 6);
        }

        // ── Y-axis value labels on the left ──
        gc.setFill(Color.web("#9CA3AF"));
        gc.setFont(Font.font(9));
        for (int g = 0; g <= 4; g++) {
            double y   = 10 + (chartH - 10) * g / 4.0;
            double val = maxVal * (4 - g) / 4.0;
            gc.fillText(AppState.get().getCurrencySymbol() + (int) val, 0, y + 4);
        }
    }

    // ── Build the Income/Expense legend below the bar chart ───────
    private void buildBarLegend() {
        barLegend.getChildren().clear();
        barLegend.getChildren().addAll(
            legendItem("#059669", "Income (Green)"),
            legendItem("#DC2626", "Expense (Red)")
        );
    }

    private HBox legendItem(String hex, String label) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        Rectangle rect = new Rectangle(12, 12);
        rect.setFill(Color.web(hex));
        rect.setArcWidth(3); rect.setArcHeight(3);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #374151;");
        row.getChildren().addAll(rect, lbl);
        return row;
    }

    // ── Expense breakdown for the current month ───────────────────
    private void buildCategoryBreakdown() {
        int month = LocalDate.now().getMonthValue();
        int year  = LocalDate.now().getYear();
        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        catTitle.setText("Expense Breakdown — " + monthName);

        // Get all transactions for this month and group expenses by category
        List<Transaction> list = AppState.get().getByMonth(month, year);
        Map<String, Double> catSpend = new LinkedHashMap<>();
        for (Transaction t : list) {
            if (t.getType() == Transaction.Type.EXPENSE) {
                catSpend.merge(t.getCategory(), t.getAmount(), Double::sum);
            }
        }
        double total = catSpend.values().stream().mapToDouble(Double::doubleValue).sum();

        catRows.getChildren().clear(); // clear old rows before rebuilding

        if (catSpend.isEmpty()) {
            Label empty = new Label("No expenses this month.");
            empty.getStyleClass().add("empty-label");
            catRows.getChildren().add(empty);
            return;
        }

        // Sort categories by amount — highest first
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(catSpend.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        int colorIdx = 0;
        for (Map.Entry<String, Double> e : sorted) {
            String cat    = e.getKey();
            double amount = e.getValue();
            double pct    = total > 0 ? amount / total : 0;
            String hex    = CHART_COLORS[colorIdx % CHART_COLORS.length];
            String name   = COLOR_NAMES [colorIdx % COLOR_NAMES.length];

            // Look up the emoji icon for this category
            String icon = AppState.EXPENSE_CATS.stream()
                .filter(c -> c[0].equals(cat)).map(c -> c[1]).findFirst().orElse("📦");

            catRows.getChildren().add(buildCatRow(icon, cat, amount, pct, hex, name));
            colorIdx++;
        }

        catRows.getChildren().add(new Separator());

        Label totalLbl = new Label("Total: " + AppState.get().format(total));
        totalLbl.setStyle(
            "-fx-font-weight: bold; -fx-font-size: 13px; " +
            "-fx-text-fill: #374151; -fx-padding: 8 0 0 0;");
        catRows.getChildren().add(totalLbl);
    }

    private VBox buildCatRow(String icon, String cat,
                             double amount, double pct,
                             String hex, String colorName) {
        VBox outer = new VBox(4);
        HBox.setHgrow(outer, Priority.ALWAYS);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 16px;");

        Label catLbl = new Label(cat);
        catLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label pctLbl = new Label(String.format("%.0f%%", pct * 100));
        pctLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");

        Label amtLbl = new Label(AppState.get().format(amount));
        amtLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + hex + ";");

        header.getChildren().addAll(iconLbl, catLbl, spacer, pctLbl, amtLbl);

        // Mini progress bar showing this category's share of total expenses
        StackPane bar = new StackPane();
        bar.setMaxWidth(Double.MAX_VALUE);
        Rectangle bg   = new Rectangle(0, 6);
        Rectangle fill = new Rectangle(0, 6);
        bg.setFill(Color.web("#E5E7EB"));
        bg.setArcWidth(6); bg.setArcHeight(6);
        fill.setFill(Color.web(hex));
        fill.setArcWidth(6); fill.setArcHeight(6);
        bg.widthProperty().bind(bar.widthProperty());
        fill.widthProperty().bind(bar.widthProperty().multiply(pct));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getChildren().addAll(bg, fill);
        Tooltip.install(bar, new Tooltip(
            colorName + " — " + String.format("%.0f%%", pct * 100) + " of total expenses"));

        outer.getChildren().addAll(header, bar);
        return outer;
    }
}
