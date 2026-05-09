package com.financetracker.controller;

import com.financetracker.model.AppState;
import com.financetracker.util.ThemeManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainViewController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private StackPane  logoPane;
    @FXML private Button     currencyBtn;
    @FXML private Button     themeBtn;

    @FXML private Button dashBtn;
    @FXML private Button transBtn;
    @FXML private Button budgetBtn;
    @FXML private Button chartsBtn;
    @FXML private Button categoryBtn;

    private Button activeNavBtn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        buildLogoIcon();
        currencyBtn.setText("💱 " + AppState.get().getCurrency());
        themeBtn.setText(AppState.get().isDarkMode() ? "☀️ Light" : "🌙 Dark");
        showDashboard();
    }

    // ── Build the coin logo icon using JavaFX shapes ──────────────
    private void buildLogoIcon() {
        Circle outer  = new Circle(28);
        outer.setFill(Color.web("#7C3AED"));

        Circle inner  = new Circle(20);
        inner.setFill(Color.web("#6D28D9"));

        Label symbol = new Label("₿");
        symbol.setStyle("-fx-font-size: 22px; -fx-text-fill: #FDCB6E; -fx-font-weight: bold;");

        logoPane.getChildren().setAll(outer, inner, symbol);
        logoPane.setAlignment(Pos.CENTER);
    }

    // ── Navigation ────────────────────────────────────────────────
    @FXML
    public void showDashboard() {
        loadPanel("/com/financetracker/fxml/DashboardPanel.fxml");
        setActive(dashBtn);
    }

    @FXML
    public void showTransactions() {
        loadPanel("/com/financetracker/fxml/TransactionPanel.fxml");
        setActive(transBtn);
    }

    @FXML
    public void showBudgets() {
        loadPanel("/com/financetracker/fxml/BudgetPanel.fxml");
        setActive(budgetBtn);
    }

    @FXML
    public void showCharts() {
        loadPanel("/com/financetracker/fxml/ChartsPanel.fxml");
        setActive(chartsBtn);
    }

    @FXML
    public void showCategories() {
        loadPanel("/com/financetracker/fxml/CategoryPanel.fxml");
        setActive(categoryBtn);
    }

    // Load a panel FXML and set it as the center of the root BorderPane
    private void loadPanel(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent panel = loader.load();
            rootPane.setCenter(panel);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR,
                "Could not load panel:\n" + e.getMessage()).showAndWait();
        }
    }

    // ── Dark / Light mode toggle ──────────────────────────────────
    @FXML
    public void onThemeToggle() {
        ThemeManager.toggle();
        themeBtn.setText(AppState.get().isDarkMode() ? "☀️ Light" : "🌙 Dark");
    }

    // ── Currency selector ─────────────────────────────────────────
    @FXML
    public void onCurrencyClick() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(AppState.get().getCurrency());
        for (String[] pair : AppState.CURRENCIES) {
            dialog.getItems().add(pair[0] + "  (" + pair[1] + ")");
        }
        for (String item : dialog.getItems()) {
            if (item.startsWith(AppState.get().getCurrency())) {
                dialog.setSelectedItem(item);
                break;
            }
        }
        dialog.setTitle("Select Currency");
        dialog.setHeaderText("Choose your preferred currency");
        dialog.setContentText("Currency:");

        dialog.showAndWait().ifPresent(selected -> {
            String code = selected.split(" ")[0].trim();
            AppState.get().setCurrency(code);
            currencyBtn.setText("💱 " + code);
            showDashboard(); // refresh to reflect new currency
        });
    }

    // ── Highlight active nav button ───────────────────────────────
    private void setActive(Button btn) {
        if (activeNavBtn != null) {
            activeNavBtn.getStyleClass().remove("nav-btn-active");
        }
        activeNavBtn = btn;
        if (btn != null) {
            btn.getStyleClass().add("nav-btn-active");
        }
    }
}
