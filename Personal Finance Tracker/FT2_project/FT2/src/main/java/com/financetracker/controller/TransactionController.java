package com.financetracker.controller;

import com.financetracker.model.AppState;
import com.financetracker.model.Transaction;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class TransactionController implements Initializable {

    @FXML private Label                            monthLabel;
    @FXML private TableView<Transaction>           table;
    @FXML private TableColumn<Transaction, String> dateCol;
    @FXML private TableColumn<Transaction, String> titleCol;
    @FXML private TableColumn<Transaction, String> catCol;
    @FXML private TableColumn<Transaction, String> typeCol;
    @FXML private TableColumn<Transaction, String> amtCol;
    @FXML private TableColumn<Transaction, String> noteCol;
    @FXML private TableColumn<Transaction, Void>   actCol;

    private int currentMonth = LocalDate.now().getMonthValue();
    private int currentYear  = LocalDate.now().getYear();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Must be set here — FXML cannot convert the string to a Callback
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupColumns();
        refresh();
    }

    // ── Wire cell value factories and cell factories ───────────────
    @SuppressWarnings("unchecked")
    private void setupColumns() {

        dateCol.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getDate().toString()));

        titleCol.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getTitle()));

        catCol.setCellValueFactory(c ->
            new SimpleStringProperty(
                c.getValue().getCategoryIcon() + " " + c.getValue().getCategory()));

        // Type — colour coded
        typeCol.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getType().name()));
        typeCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    setStyle("-fx-text-fill: " +
                        (item.equals("INCOME") ? "#059669" : "#DC2626") +
                        "; -fx-font-weight: bold;");
                }
            }
        });

        // Amount — colour coded, right-aligned
        amtCol.setCellValueFactory(c -> {
            Transaction t = c.getValue();
            boolean inc = t.getType() == Transaction.Type.INCOME;
            return new SimpleStringProperty(
                (inc ? "+" : "-") + AppState.get().format(t.getAmount()));
        });
        amtCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    setStyle("-fx-text-fill: " +
                        (item.startsWith("+") ? "#059669" : "#DC2626") +
                        "; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                }
            }
        });

        noteCol.setCellValueFactory(c ->
            new SimpleStringProperty(
                c.getValue().getNote() != null ? c.getValue().getNote() : ""));

        // Actions — Edit and Delete buttons
        actCol.setCellFactory(tc -> new TableCell<>() {
            final Button edit = new Button("Edit");
            final Button del  = new Button("Delete");
            final HBox   box  = new HBox(6, edit, del);
            {
                edit.getStyleClass().add("icon-btn");
                del.getStyleClass().add("icon-btn-danger");
                box.setAlignment(Pos.CENTER);

                edit.setOnAction(e ->
                    openDialog(getTableView().getItems().get(getIndex()), null));

                del.setOnAction(e -> {
                    Transaction t = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Delete \"" + t.getTitle() + "\"?",
                        ButtonType.YES, ButtonType.NO);
                    confirm.setHeaderText(null);
                    confirm.showAndWait().ifPresent(btn -> {
                        if (btn == ButtonType.YES) {
                            AppState.get().getTransactions().remove(t);
                            AppState.get().saveData(); // persist the deletion
                            refresh();
                        }
                    });
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    @FXML public void onPrev() {
        if (--currentMonth < 1) { currentMonth = 12; currentYear--; }
        refresh();
    }

    @FXML public void onNext() {
        if (++currentMonth > 12) { currentMonth = 1; currentYear++; }
        refresh();
    }

    @FXML public void onAddIncome()  { openDialog(null, Transaction.Type.INCOME); }
    @FXML public void onAddExpense() { openDialog(null, Transaction.Type.EXPENSE); }

    // ── Reload table for the current month ────────────────────────
    private void refresh() {
        monthLabel.setText(
            Month.of(currentMonth)
                 .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            + " " + currentYear);

        List<Transaction> list = AppState.get()
            .getByMonth(currentMonth, currentYear)
            .stream()
            .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
            .toList();

        table.setItems(FXCollections.observableArrayList(list));

        if (list.isEmpty())
            table.setPlaceholder(new Label("No transactions for this month."));
    }

    // ── Add / Edit dialog ─────────────────────────────────────────
    private void openDialog(Transaction existing, Transaction.Type defaultType) {
        Dialog<Transaction> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Transaction" : "Edit Transaction");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/com/financetracker/css/style.css").toExternalForm());

        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(12);
        grid.setPadding(new Insets(20));
        ColumnConstraints c0 = new ColumnConstraints(90);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        // Type toggle
        ToggleGroup tg     = new ToggleGroup();
        RadioButton incBtn = new RadioButton("Income");
        RadioButton expBtn = new RadioButton("Expense");
        incBtn.setToggleGroup(tg);
        expBtn.setToggleGroup(tg);
        Transaction.Type initType = existing != null ? existing.getType()
                                  : (defaultType != null ? defaultType : Transaction.Type.EXPENSE);
        (initType == Transaction.Type.INCOME ? incBtn : expBtn).setSelected(true);
        HBox typeBox = new HBox(12, incBtn, expBtn);

        TextField  titleFld  = field(existing != null ? existing.getTitle() : "", "e.g. Lunch, Salary…");
        TextField  amountFld = field(existing != null ? String.valueOf(existing.getAmount()) : "", "0.00");
        DatePicker datePk    = new DatePicker(existing != null ? existing.getDate() : LocalDate.now());
        datePk.getStyleClass().add("form-field");

        ComboBox<String> catCombo = new ComboBox<>();
        catCombo.getStyleClass().add("form-field");
        catCombo.setMaxWidth(Double.MAX_VALUE);

        TextField noteFld = field(
            existing != null && existing.getNote() != null ? existing.getNote() : "",
            "Optional note…");

        // Populate categories based on selected type
        Runnable fillCats = () -> {
            var cats = incBtn.isSelected() ? AppState.INCOME_CATS : AppState.EXPENSE_CATS;
            catCombo.getItems().setAll(cats.stream().map(c -> c[1] + " " + c[0]).toList());
            if (existing != null && existing.getCategory() != null) {
                catCombo.getItems().stream()
                    .filter(s -> s.contains(existing.getCategory()))
                    .findFirst().ifPresent(catCombo::setValue);
            } else {
                catCombo.setValue(catCombo.getItems().get(0));
            }
        };
        incBtn.setOnAction(e -> fillCats.run());
        expBtn.setOnAction(e -> fillCats.run());
        fillCats.run();

        grid.add(new Label("Type:"),     0, 0); grid.add(typeBox,   1, 0);
        grid.add(new Label("Title:"),    0, 1); grid.add(titleFld,  1, 1);
        grid.add(new Label("Amount:"),   0, 2); grid.add(amountFld, 1, 2);
        grid.add(new Label("Date:"),     0, 3); grid.add(datePk,    1, 3);
        grid.add(new Label("Category:"), 0, 4); grid.add(catCombo,  1, 4);
        grid.add(new Label("Note:"),     0, 5); grid.add(noteFld,   1, 5);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            try {
                String titleText = titleFld.getText().trim();
                if (titleText.isEmpty()) { alert("Title is required."); return null; }
                double amt = Double.parseDouble(amountFld.getText().trim());
                if (amt <= 0)           { alert("Amount must be positive."); return null; }

                String sel  = catCombo.getValue();
                String icon = sel.substring(0, sel.indexOf(" ")).trim();
                String cat  = sel.substring(sel.indexOf(" ") + 1).trim();
                Transaction.Type type =
                    incBtn.isSelected() ? Transaction.Type.INCOME : Transaction.Type.EXPENSE;

                if (existing != null) {
                    existing.setTitle(titleText); existing.setAmount(amt);
                    existing.setType(type);       existing.setCategory(cat);
                    existing.setCategoryIcon(icon);
                    existing.setDate(datePk.getValue());
                    existing.setNote(noteFld.getText().trim());
                    return existing;
                } else {
                    return new Transaction(titleText, amt, type, cat, icon,
                                          datePk.getValue(), noteFld.getText().trim());
                }
            } catch (NumberFormatException ex) {
                alert("Invalid amount – enter a number."); return null;
            }
        });

        dialog.showAndWait().ifPresent(t -> {
            if (existing == null) AppState.get().getTransactions().add(t);
            AppState.get().saveData(); // persist add or edit
            refresh();
        });
    }

    private TextField field(String value, String prompt) {
        TextField tf = new TextField(value);
        tf.setPromptText(prompt);
        tf.getStyleClass().add("form-field");
        return tf;
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
