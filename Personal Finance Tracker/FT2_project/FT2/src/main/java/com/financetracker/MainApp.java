package com.financetracker;

import com.financetracker.util.DataManager;
import com.financetracker.util.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // ── Print the data file location to console on startup ─────
        System.out.println("==============================================");
        System.out.println(" Finance Tracker — Data File Location:");
        System.out.println(" " + DataManager.getSaveFilePath());
        System.out.println(" Open this file in any text editor to view");
        System.out.println(" your saved transactions and budgets.");
        System.out.println("==============================================");

        // Load the main layout from FXML
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/financetracker/fxml/MainView.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1100, 700);

        // Load the shared stylesheet
        scene.getStylesheets().add(
            getClass().getResource("/com/financetracker/css/style.css").toExternalForm());

        // Register scene with ThemeManager so the dark/light toggle works
        ThemeManager.setScene(scene);
        ThemeManager.apply();

        stage.setTitle("Finance Tracker");
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
