package com.financetracker.controller;

import com.financetracker.model.AppState;
import com.financetracker.model.Transaction;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

public class BudgetController implements Initializable {

    @FXML private Label monthLabel;
    @FXML private VBox  listBox;

    private int currentMonth = LocalDate.now().getMonthValue();
    private int currentYear  = LocalDate.now().getYear();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        refresh();
    }

    @FXML
    public void onPrev() {
        if (--currentMonth < 1) { currentMonth = 12; currentYear--; }
        refresh();
    }

    @FXML
    public void onNext() {
        if (++currentMonth > 12) { currentMonth = 1; currentYear++; }
        refresh();
    }

    @FXML
    public void onAddBudget() {
        showAddDialog();
    }

    // ── Rebuild the budget card list ───────────────────────────────
    private void refresh() {
        monthLabel.setText(
            Month.of(currentMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            + " " + currentYear);
        listBox.getChildren().clear();

        // Read budgets from AppState (which was loaded from txt file)
        Map<String, Double> budgetLimits = AppState.get().getBudgetLimits();

        if (budgetLimits.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60));
            Label ico = new Label("🎯");
            ico.setStyle("-fx-font-size: 40px;");
            Label lbl = new Label("No budgets yet.\nClick '+ Add Budget' to set a limit.");
            lbl.getStyleClass().add("empty-label");
            lbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            empty.getChildren().addAll(ico, lbl);
            listBox.getChildren().add(empty);
            return;
        }

        // Calculate spending per category for the current month
        List<Transaction> monthTx = AppState.get().getByMonth(currentMonth, currentYear);
        Map<String, Double> spent = new HashMap<>();
        for (Transaction t : monthTx) {
            if (t.getType() == Transaction.Type.EXPENSE) {
                spent.merge(t.getCategory(), t.getAmount(), Double::sum);
            }
        }

        for (Map.Entry<String, Double> e : budgetLimits.entrySet()) {
            String cat      = e.getKey();
            double limit    = e.getValue();
            double spentAmt = spent.getOrDefault(cat, 0.0);
            listBox.getChildren().add(buildBudgetCard(cat, limit, spentAmt));
        }
    }

    // ── Choose a progress bar colour based on how much is used ────
    private String progressHex(double progress) {
        if (progress >= 1.0)  return "#DC2626"; // red  — over budget
        if (progress >= 0.85) return "#D97706"; // amber — near limit
        return "#059669";                        // green — safe
    }

    private String progressLabel(double progress) {
        if (progress >= 1.0)  return "⚠️ Over budget!";
        if (progress >= 0.85) return "⚡ Near limit";
        return String.format("%.0f%% used", progress * 100);
    }

    // ── Build one budget card ──────────────────────────────────────
    private VBox buildBudgetCard(String cat, double limit, double spentAmt) {
        VBox card = new VBox(10);
        card.getStyleClass().add("budget-card");
        card.setPadding(new Insets(18));

        String icon     = AppState.EXPENSE_CATS.stream()
            .filter(c -> c[0].equals(cat)).map(c -> c[1]).findFirst().orElse("📦");
        double progress = limit > 0 ? Math.min(spentAmt / limit, 1.0) : 0;
        String colorHex = progressHex(progress);

        // Header row
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 22px;");

        VBox info = new VBox(2);
        Label name   = new Label(cat);
        name.getStyleClass().add("budget-name");
        Label status = new Label(progressLabel(progress));
        status.setStyle("-fx-font-size: 11px; -fx-text-fill: " + colorHex + ";");
        info.getChildren().addAll(name, status);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox amounts = new VBox(2);
        amounts.setAlignment(Pos.CENTER_RIGHT);
        Label spentLbl = new Label(AppState.get().format(spentAmt));
        spentLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #DC2626;");
        Label limitLbl = new Label("of " + AppState.get().format(limit));
        limitLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");
        amounts.getChildren().addAll(spentLbl, limitLbl);

        header.getChildren().addAll(iconLbl, info, spacer, amounts);

        // Progress bar
        StackPane bar = new StackPane();
        bar.setMaxWidth(Double.MAX_VALUE);
        Rectangle bg   = new Rectangle(0, 10);
        Rectangle fill = new Rectangle(0, 10);
        bg.setFill(Color.web("#E5E7EB"));
        bg.setArcWidth(10); bg.setArcHeight(10);
        fill.setFill(Color.web(colorHex));
        fill.setArcWidth(10); fill.setArcHeight(10);
        bg.widthProperty().bind(bar.widthProperty());
        fill.widthProperty().bind(bar.widthProperty().multiply(progress));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getChildren().addAll(bg, fill);

        // Footer row
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        Label remaining = new Label("Remaining: " +
            AppState.get().format(Math.max(0, limit - spentAmt)));
        remaining.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
        Region fSpacer = new Region();
        HBox.setHgrow(fSpacer, Priority.ALWAYS);

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("icon-btn");
        editBtn.setOnAction(e -> showEditDialog(cat, limit));

        Button delBtn = new Button("Delete");
        delBtn.getStyleClass().add("icon-btn-danger");
        // Delete from AppState — it auto-saves to txt file
        delBtn.setOnAction(e -> { AppState.get().removeBudget(cat); refresh(); });

        footer.getChildren().addAll(remaining, fSpacer, editBtn, delBtn);
        card.getChildren().addAll(header, bar, footer);
        return card;
    }

    // ── Dialogs ────────────────────────────────────────────────────
    private void showAddDialog() {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Add Budget");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/com/financetracker/css/style.css").toExternalForm());

        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(12);
        grid.setPadding(new Insets(20));
        ColumnConstraints c0 = new ColumnConstraints(100);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        ComboBox<String> catCombo = new ComboBox<>();
        catCombo.setMaxWidth(Double.MAX_VALUE);
        catCombo.getStyleClass().add("form-field");
        for (String[] cat : AppState.EXPENSE_CATS) catCombo.getItems().add(cat[1] + " " + cat[0]);
        catCombo.setValue(catCombo.getItems().get(0));

        TextField limitFld = new TextField();
        limitFld.setPromptText("Monthly limit");
        limitFld.getStyleClass().add("form-field");

        grid.add(new Label("Category:"), 0, 0); grid.add(catCombo,  1, 0);
        grid.add(new Label("Limit (" + AppState.get().getCurrencySymbol() + "):"), 0, 1);
        grid.add(limitFld, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            try {
                String limitText = limitFld.getText().trim();
                if (limitText.isEmpty()) {
                    new Alert(Alert.AlertType.ERROR, "Please enter a budget limit.").showAndWait();
                    return null;
                }
                double limit = Double.parseDouble(limitText);
                if (limit <= 0) {
                    new Alert(Alert.AlertType.ERROR, "Limit must be greater than zero.").showAndWait();
                    return null;
                }
                String sel  = catCombo.getValue();
                // Category name is everything after the first space (skip the emoji)
                String cat  = sel.substring(sel.indexOf(" ") + 1).trim();
                return new String[]{ cat, String.valueOf(limit) };
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR, "Invalid amount – please enter a number.").showAndWait();
                return null;
            }
        });

        dialog.showAndWait().ifPresent(result -> {
            // Save to AppState — it auto-saves to txt file
            AppState.get().setBudget(result[0], Double.parseDouble(result[1]));
            refresh();
        });
    }

    private void showEditDialog(String cat, double currentLimit) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(currentLimit));
        dialog.setTitle("Edit Budget");
        dialog.setHeaderText("Update limit for: " + cat);
        dialog.setContentText("New limit (" + AppState.get().getCurrencySymbol() + "):");
        dialog.showAndWait().ifPresent(val -> {
            try {
                double newLimit = Double.parseDouble(val.trim());
                if (newLimit <= 0) {
                    new Alert(Alert.AlertType.ERROR, "Limit must be greater than zero.").showAndWait();
                    return;
                }
                // Save to AppState — it auto-saves to txt file
                AppState.get().setBudget(cat, newLimit);
                refresh();
            } catch (NumberFormatException ignored) {
                new Alert(Alert.AlertType.ERROR, "Invalid amount – please enter a number.").showAndWait();
            }
        });
    }
}
