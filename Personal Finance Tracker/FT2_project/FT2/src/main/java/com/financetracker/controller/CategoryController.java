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
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class CategoryController implements Initializable {

    @FXML private VBox expenseList;
    @FXML private VBox incomeList;

    // Mutable copies so the user can add custom categories this session
    private final List<String[]> expenseCats = new ArrayList<>(AppState.EXPENSE_CATS);
    private final List<String[]> incomeCats  = new ArrayList<>(AppState.INCOME_CATS);

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        refresh();
    }

    @FXML
    public void onAddExpense() { showAddDialog(Transaction.Type.EXPENSE); }

    @FXML
    public void onAddIncome()  { showAddDialog(Transaction.Type.INCOME); }

    // ── Rebuild both category lists ────────────────────────────────
    private void refresh() {
        expenseList.getChildren().clear();
        incomeList.getChildren().clear();
        for (String[] cat : expenseCats) expenseList.getChildren().add(buildRow(cat, Transaction.Type.EXPENSE));
        for (String[] cat : incomeCats)  incomeList.getChildren().add(buildRow(cat, Transaction.Type.INCOME));
    }

    // ── Build one category row ─────────────────────────────────────
    private HBox buildRow(String[] cat, Transaction.Type type) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("category-row");
        row.setPadding(new Insets(10, 14, 10, 14));

        // Colour dot — red for expense, green for income
        Color dotColor = type == Transaction.Type.EXPENSE
            ? Color.web("#DC2626") : Color.web("#059669");
        Circle dot = new Circle(7, dotColor);

        Label icon = new Label(cat[1]);
        icon.setStyle("-fx-font-size: 18px;");

        Label name = new Label(cat[0]);
        name.getStyleClass().add("category-name");

        Label colorName = new Label(type == Transaction.Type.EXPENSE ? "Crimson" : "Emerald");
        colorName.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF; -fx-padding: 0 0 0 4;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button del = new Button("Delete");
        del.getStyleClass().add("icon-btn-danger");
        del.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + cat[0] + "\"?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    if (type == Transaction.Type.EXPENSE) expenseCats.remove(cat);
                    else incomeCats.remove(cat);
                    refresh();
                }
            });
        });

        row.getChildren().addAll(dot, icon, name, colorName, spacer, del);
        return row;
    }

    // ── Add category dialog ────────────────────────────────────────
    private void showAddDialog(Transaction.Type type) {
        String typeName = type == Transaction.Type.EXPENSE ? "Expense" : "Income";
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Add " + typeName + " Category");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/com/financetracker/css/style.css").toExternalForm());

        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(12);
        grid.setPadding(new Insets(20));
        ColumnConstraints c0 = new ColumnConstraints(80);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        TextField nameFld = new TextField();
        nameFld.setPromptText("Category name");
        nameFld.getStyleClass().add("form-field");

        TextField iconFld = new TextField();
        iconFld.setPromptText("Emoji e.g. 🎮");
        iconFld.getStyleClass().add("form-field");

        grid.add(new Label("Name:"), 0, 0); grid.add(nameFld, 1, 0);
        grid.add(new Label("Icon:"), 0, 1); grid.add(iconFld, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            String n = nameFld.getText().trim();
            String i = iconFld.getText().trim();
            if (n.isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "Name required.").showAndWait();
                return null;
            }
            return new String[]{ n, i.isEmpty() ? "📦" : i };
        });

        dialog.showAndWait().ifPresent(cat -> {
            if (type == Transaction.Type.EXPENSE) expenseCats.add(cat);
            else incomeCats.add(cat);
            refresh();
        });
    }
}
