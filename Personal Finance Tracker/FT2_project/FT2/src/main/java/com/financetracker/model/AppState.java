package com.financetracker.model;

import com.financetracker.util.DataManager;

import java.time.LocalDate;
import java.util.*;

/**
 * Central place for all app data and settings.
 *
 * On startup:
 *   - Transactions are loaded from FinanceTracker_data.txt
 *   - Budgets are also loaded from FinanceTracker_data.txt
 *
 * Whenever data changes, call saveData() to write everything back.
 */
public class AppState {

    // ── Singleton ──────────────────────────────────────────────────
    private static final AppState INSTANCE = new AppState();
    public static AppState get() { return INSTANCE; }

    // ── Theme & Currency ───────────────────────────────────────────
    private boolean darkMode = false;
    private String  currency = "USD";

    // ── Transaction list ───────────────────────────────────────────
    private final List<Transaction> transactions = new ArrayList<>();

    // ── Budget limits: category name → monthly limit amount ───────
    private final Map<String, Double> budgetLimits = new LinkedHashMap<>();

    // ── Available categories ───────────────────────────────────────
    public static final List<String[]> EXPENSE_CATS = Arrays.asList(
        new String[]{"Food & Dining",   "🍔"},
        new String[]{"Transport",       "🚌"},
        new String[]{"Shopping",        "🛍️"},
        new String[]{"Bills",           "💡"},
        new String[]{"Entertainment",   "🎬"},
        new String[]{"Health",          "💊"},
        new String[]{"Education",       "📚"},
        new String[]{"Other",           "📦"}
    );

    public static final List<String[]> INCOME_CATS = Arrays.asList(
        new String[]{"Salary",        "💼"},
        new String[]{"Freelance",     "💻"},
        new String[]{"Investment",    "📈"},
        new String[]{"Gift",          "🎁"},
        new String[]{"Other Income",  "💰"}
    );

    public static final String[][] CURRENCIES = {
        {"USD", "$"},
        {"BDT", "৳"},
        {"EUR", "€"},
        {"GBP", "£"},
        {"JPY", "¥"},
        {"INR", "₹"},
        {"CAD", "CA$"},
        {"AUD", "A$"}
    };

    private AppState() {
        // Load saved transactions from txt file
        List<Transaction> savedTransactions = DataManager.loadTransactions();
        if (savedTransactions.isEmpty()) {
            loadSampleData(); // show sample data on very first launch
        } else {
            transactions.addAll(savedTransactions);
        }

        // Load saved budgets from txt file
        budgetLimits.putAll(DataManager.loadBudgets());
    }

    // ── Sample data shown on very first launch ─────────────────────
    private void loadSampleData() {
        LocalDate today = LocalDate.now();
        transactions.add(new Transaction("Monthly Salary",    5000, Transaction.Type.INCOME,  "Salary",        "💼", today.withDayOfMonth(1),  ""));
        transactions.add(new Transaction("Freelance Project",  800, Transaction.Type.INCOME,  "Freelance",     "💻", today.withDayOfMonth(5),  "Web design"));
        transactions.add(new Transaction("Grocery Shopping",   120, Transaction.Type.EXPENSE, "Food & Dining", "🍔", today.withDayOfMonth(3),  "Weekly groceries"));
        transactions.add(new Transaction("Electric Bill",       85, Transaction.Type.EXPENSE, "Bills",         "💡", today.withDayOfMonth(7),  ""));
        transactions.add(new Transaction("Bus Pass",            40, Transaction.Type.EXPENSE, "Transport",     "🚌", today.withDayOfMonth(8),  "Monthly pass"));
        transactions.add(new Transaction("Netflix",             15, Transaction.Type.EXPENSE, "Entertainment", "🎬", today.withDayOfMonth(10), ""));
        transactions.add(new Transaction("Lunch",               25, Transaction.Type.EXPENSE, "Food & Dining", "🍔", today.withDayOfMonth(12), "With colleagues"));
        transactions.add(new Transaction("Doctor Visit",        60, Transaction.Type.EXPENSE, "Health",        "💊", today.withDayOfMonth(14), ""));
        transactions.add(new Transaction("Online Course",       49, Transaction.Type.EXPENSE, "Education",     "📚", today.withDayOfMonth(15), "Java course"));
        transactions.add(new Transaction("Gift Received",      200, Transaction.Type.INCOME,  "Gift",          "🎁", today.withDayOfMonth(16), "Birthday"));
    }

    // ── Save ALL data (transactions + budgets) to txt file ─────────
    public void saveData() {
        DataManager.save(transactions, budgetLimits);
    }

    // ── Transaction getters ────────────────────────────────────────
    public List<Transaction> getTransactions() { return transactions; }

    // ── Budget getters / setters ───────────────────────────────────
    public Map<String, Double> getBudgetLimits() { return budgetLimits; }

    public void setBudget(String category, double limit) {
        budgetLimits.put(category, limit);
        saveData(); // auto-save whenever a budget changes
    }

    public void removeBudget(String category) {
        budgetLimits.remove(category);
        saveData(); // auto-save whenever a budget is removed
    }

    // ── Theme / Currency ───────────────────────────────────────────
    public boolean isDarkMode()              { return darkMode; }
    public void    setDarkMode(boolean dark) { this.darkMode = dark; }

    public String  getCurrency()             { return currency; }
    public void    setCurrency(String c)     { this.currency = c; }

    public String getCurrencySymbol() {
        for (String[] pair : CURRENCIES) {
            if (pair[0].equals(currency)) return pair[1];
        }
        return currency;
    }

    public String format(double amount) {
        return getCurrencySymbol() + String.format("%,.2f", amount);
    }

    // ── Filtering helpers ──────────────────────────────────────────
    public List<Transaction> getByMonth(int month, int year) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction t : transactions) {
            if (t.getDate().getMonthValue() == month && t.getDate().getYear() == year) {
                result.add(t);
            }
        }
        return result;
    }

    public double totalIncome(List<Transaction> list) {
        return list.stream().filter(t -> t.getType() == Transaction.Type.INCOME)
                   .mapToDouble(Transaction::getAmount).sum();
    }

    public double totalExpense(List<Transaction> list) {
        return list.stream().filter(t -> t.getType() == Transaction.Type.EXPENSE)
                   .mapToDouble(Transaction::getAmount).sum();
    }
}
