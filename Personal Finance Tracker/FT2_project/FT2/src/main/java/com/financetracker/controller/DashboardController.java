package com.financetracker.controller;

import com.financetracker.model.AppState;
import com.financetracker.model.Transaction;
import com.financetracker.util.TxtExporter;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private Label monthLabel;
    @FXML private Label incomeVal;
    @FXML private Label expenseVal;
    @FXML private Label balanceVal;
    @FXML private VBox  txListBox;

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
    public void onExport() {
        Stage stage = (Stage) monthLabel.getScene().getWindow();
        List<Transaction> list = AppState.get().getByMonth(currentMonth, currentYear);
        TxtExporter.export(list, currentMonth, currentYear, stage);
    }

    // ── Refresh all data for the selected month ────────────────────
    private void refresh() {
        monthLabel.setText(
            Month.of(currentMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            + " " + currentYear);

        List<Transaction> list = AppState.get().getByMonth(currentMonth, currentYear);
        double income  = AppState.get().totalIncome(list);
        double expense = AppState.get().totalExpense(list);
        double balance = income - expense;

        incomeVal.setText(AppState.get().format(income));
        expenseVal.setText(AppState.get().format(expense));
        balanceVal.setText(AppState.get().format(balance));

        txListBox.getChildren().clear();

        if (list.isEmpty()) {
            Label empty = new Label("No transactions this month. Add one from Transactions!");
            empty.getStyleClass().add("empty-label");
            txListBox.getChildren().add(empty);
            return;
        }

        // Show newest first
        list.stream()
            .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
            .forEach(t -> txListBox.getChildren().add(buildTxRow(t)));
    }

    // ── Build one transaction row ──────────────────────────────────
    private HBox buildTxRow(Transaction t) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.getStyleClass().add("recent-tx-row");

        Label icon = new Label(t.getCategoryIcon());
        icon.setStyle("-fx-font-size: 20px; -fx-min-width: 30px;");

        VBox info = new VBox(2);
        Label titleLbl = new Label(t.getTitle());
        titleLbl.getStyleClass().add("tx-name");
        Label catDate = new Label(t.getCategory() + "  •  " + t.getDate());
        catDate.getStyleClass().add("tx-date");
        info.getChildren().addAll(titleLbl, catDate);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean isIncome = t.getType() == Transaction.Type.INCOME;
        Label amount = new Label((isIncome ? "+" : "-") + AppState.get().format(t.getAmount()));
        amount.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: "
                + (isIncome ? "#059669" : "#DC2626") + ";");

        if (t.getNote() != null && !t.getNote().isBlank()) {
            Tooltip.install(row, new Tooltip(t.getNote()));
        }

        row.getChildren().addAll(icon, info, spacer, amount);
        return row;
    }
}
