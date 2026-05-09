package com.financetracker.util;

import com.financetracker.model.AppState;
import com.financetracker.model.Transaction;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.Month;
import java.util.List;
import java.util.Locale;

/**
 * Saves a list of transactions to a plain .txt file chosen by the user.
 * Uses simple file handling — no database, no CSV.
 */
public class TxtExporter {

    public static void export(List<Transaction> transactions, int month, int year, Window owner) {

        // Open a "Save As" dialog so the user can pick where to save the file
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save as TXT");

        String monthName = Month.of(month).getDisplayName(
                java.time.format.TextStyle.FULL, Locale.ENGLISH);
        chooser.setInitialFileName("FinanceTracker_" + monthName + "_" + year + ".txt");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        File file = chooser.showSaveDialog(owner);
        if (file == null) return; // user clicked Cancel

        // Write the file line by line
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {

            // ── Header ────────────────────────────────────────────────
            writer.write("Finance Tracker – " + monthName + " " + year);
            writer.newLine();
            writer.write("Currency: " + AppState.get().getCurrency());
            writer.newLine();
            writer.write("=".repeat(60));
            writer.newLine();
            writer.newLine();

            // ── Column headers ────────────────────────────────────────
            writer.write(String.format("%-12s %-20s %-10s %-18s %10s  %s",
                    "Date", "Title", "Type", "Category", "Amount", "Note"));
            writer.newLine();
            writer.write("-".repeat(90));
            writer.newLine();

            // ── One line per transaction ──────────────────────────────
            for (Transaction t : transactions) {
                String note = (t.getNote() != null && !t.getNote().isBlank())
                        ? t.getNote() : "-";

                writer.write(String.format("%-12s %-20s %-10s %-18s %10.2f  %s",
                        t.getDate().toString(),
                        truncate(t.getTitle(), 20),
                        t.getType().name(),
                        truncate(t.getCategory(), 18),
                        t.getAmount(),
                        note));
                writer.newLine();
            }

            // ── Summary totals ────────────────────────────────────────
            writer.newLine();
            writer.write("=".repeat(60));
            writer.newLine();

            double totalIncome  = AppState.get().totalIncome(transactions);
            double totalExpense = AppState.get().totalExpense(transactions);
            double balance      = totalIncome - totalExpense;

            writer.write(String.format("Total Income :  %s", AppState.get().format(totalIncome)));
            writer.newLine();
            writer.write(String.format("Total Expense:  %s", AppState.get().format(totalExpense)));
            writer.newLine();
            writer.write(String.format("Balance      :  %s", AppState.get().format(balance)));
            writer.newLine();

            showInfo("Export successful!\nSaved to: " + file.getAbsolutePath());

        } catch (Exception e) {
            showInfo("Export failed: " + e.getMessage());
        }
    }

    // Trim long text so columns stay neat
    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 1) + "…";
    }

    private static void showInfo(String msg) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION, msg);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
