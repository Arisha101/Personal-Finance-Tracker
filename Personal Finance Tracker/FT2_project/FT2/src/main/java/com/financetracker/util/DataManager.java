package com.financetracker.util;

import com.financetracker.model.Transaction;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Saves and loads all app data (transactions + budgets) to/from a plain .txt file.
 *
 * FILE LOCATION:
 *   The file is saved in the SAME FOLDER where you run the app from.
 *   When running from IntelliJ → it appears right inside your project folder,
 *   next to pom.xml, so you can see it directly in the Project panel.
 *
 *   Full path printed in console on every save:
 *   e.g.  C:\Users\YourName\IdeaProjects\FT2\FinanceTracker_data.txt
 *
 * FILE FORMAT:
 *   [TRANSACTIONS]
 *   DATE|TITLE|TYPE|CATEGORY|ICON|AMOUNT|NOTE
 *
 *   [BUDGETS]
 *   CATEGORY|LIMIT
 */
public class DataManager {

    // Saved in the current working directory — when run from IntelliJ this is
    // the project root (same folder as pom.xml), so it appears in the Project panel.
    private static final Path SAVE_FILE = Paths.get("FinanceTracker_data.txt");

    // Section markers inside the txt file
    private static final String SECTION_TRANSACTIONS = "[TRANSACTIONS]";
    private static final String SECTION_BUDGETS      = "[BUDGETS]";

    // ── Save EVERYTHING (transactions + budgets) to the txt file ──
    public static void save(List<Transaction> transactions, Map<String, Double> budgets) {
        try (BufferedWriter writer = Files.newBufferedWriter(SAVE_FILE)) {

            // ── File header ──────────────────────────────────────────
            writer.write("# ============================================================");
            writer.newLine();
            writer.write("# Finance Tracker — Data File");
            writer.newLine();
            writer.write("# File saved at: " + SAVE_FILE.toAbsolutePath());
            writer.newLine();
            writer.write("# ============================================================");
            writer.newLine();
            writer.newLine();

            // ── Transactions section ─────────────────────────────────
            writer.write(SECTION_TRANSACTIONS);
            writer.newLine();
            writer.write("# Format: DATE|TITLE|TYPE|CATEGORY|ICON|AMOUNT|NOTE");
            writer.newLine();

            for (Transaction t : transactions) {
                writer.write(String.format("%s|%s|%s|%s|%s|%.2f|%s",
                    t.getDate(),
                    t.getTitle(),
                    t.getType().name(),
                    t.getCategory(),
                    t.getCategoryIcon(),
                    t.getAmount(),
                    t.getNote() != null ? t.getNote() : ""
                ));
                writer.newLine();
            }

            writer.newLine();

            // ── Budgets section ──────────────────────────────────────
            writer.write(SECTION_BUDGETS);
            writer.newLine();
            writer.write("# Format: CATEGORY|MONTHLY_LIMIT");
            writer.newLine();

            for (Map.Entry<String, Double> entry : budgets.entrySet()) {
                writer.write(entry.getKey() + "|" + String.format("%.2f", entry.getValue()));
                writer.newLine();
            }

            // Print the file path to console so you always know where to look
            System.out.println("[DataManager] Data saved to: " + SAVE_FILE.toAbsolutePath());

        } catch (IOException e) {
            System.err.println("[DataManager] ERROR saving data: " + e.getMessage());
        }
    }

    // ── Convenience: save only transactions (keeps existing budgets) ──
    public static void saveTransactions(List<Transaction> transactions) {
        Map<String, Double> budgets = loadBudgets();
        save(transactions, budgets);
    }

    // ── Load all transactions from the txt file ───────────────────
    public static List<Transaction> loadTransactions() {
        List<Transaction> result = new ArrayList<>();

        if (!Files.exists(SAVE_FILE)) {
            System.out.println("[DataManager] No data file found — starting fresh. File will be created at: "
                + SAVE_FILE.toAbsolutePath());
            return result;
        }

        System.out.println("[DataManager] Loading data from: " + SAVE_FILE.toAbsolutePath());

        try (BufferedReader reader = Files.newBufferedReader(SAVE_FILE)) {
            boolean inTransactions = false;
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.equals(SECTION_TRANSACTIONS)) { inTransactions = true;  continue; }
                if (line.equals(SECTION_BUDGETS))      { inTransactions = false; continue; }

                // Skip comments, blank lines, and non-transaction sections
                if (!inTransactions || line.startsWith("#") || line.isBlank()) continue;

                // Parse: DATE|TITLE|TYPE|CATEGORY|ICON|AMOUNT|NOTE
                String[] parts = line.split("\\|", 7);
                if (parts.length < 6) continue;

                LocalDate        date     = LocalDate.parse(parts[0]);
                String           title    = parts[1];
                Transaction.Type type     = Transaction.Type.valueOf(parts[2]);
                String           category = parts[3];
                String           icon     = parts[4];
                double           amount   = Double.parseDouble(parts[5]);
                String           note     = parts.length > 6 ? parts[6] : "";

                result.add(new Transaction(title, amount, type, category, icon, date, note));
            }

        } catch (IOException e) {
            System.err.println("[DataManager] ERROR loading transactions: " + e.getMessage());
        }

        System.out.println("[DataManager] Loaded " + result.size() + " transaction(s).");
        return result;
    }

    // ── Load all budget limits from the txt file ──────────────────
    public static Map<String, Double> loadBudgets() {
        Map<String, Double> budgets = new LinkedHashMap<>();

        if (!Files.exists(SAVE_FILE)) return budgets;

        try (BufferedReader reader = Files.newBufferedReader(SAVE_FILE)) {
            boolean inBudgets = false;
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.equals(SECTION_BUDGETS))      { inBudgets = true;  continue; }
                if (line.equals(SECTION_TRANSACTIONS)) { inBudgets = false; continue; }

                if (!inBudgets || line.startsWith("#") || line.isBlank()) continue;

                // Parse: CATEGORY|LIMIT
                String[] parts = line.split("\\|", 2);
                if (parts.length < 2) continue;

                String category = parts[0].trim();
                double limit    = Double.parseDouble(parts[1].trim());
                budgets.put(category, limit);
            }

        } catch (IOException e) {
            System.err.println("[DataManager] ERROR loading budgets: " + e.getMessage());
        }

        System.out.println("[DataManager] Loaded " + budgets.size() + " budget(s).");
        return budgets;
    }

    /** Returns the full path where the data file is saved. */
    public static String getSaveFilePath() {
        return SAVE_FILE.toAbsolutePath().toString();
    }
}
