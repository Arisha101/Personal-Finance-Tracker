package com.financetracker.model;

import java.time.LocalDate;

public class Transaction {

    public enum Type { INCOME, EXPENSE }

    private String title;
    private double amount;
    private Type type;
    private String category;
    private String categoryIcon;
    private LocalDate date;
    private String note;

    public Transaction(String title, double amount, Type type,
                       String category, String categoryIcon, LocalDate date, String note) {
        this.title        = title;
        this.amount       = amount;
        this.type         = type;
        this.category     = category;
        this.categoryIcon = categoryIcon;
        this.date         = date;
        this.note         = note;
    }

    // Simple getters and setters
    public String    getTitle()        { return title; }
    public void      setTitle(String t){ this.title = t; }

    public double    getAmount()           { return amount; }
    public void      setAmount(double a)   { this.amount = a; }

    public Type      getType()             { return type; }
    public void      setType(Type t)       { this.type = t; }

    public String    getCategory()         { return category; }
    public void      setCategory(String c) { this.category = c; }

    public String    getCategoryIcon()             { return categoryIcon; }
    public void      setCategoryIcon(String icon)  { this.categoryIcon = icon; }

    public LocalDate getDate()             { return date; }
    public void      setDate(LocalDate d)  { this.date = d; }

    public String    getNote()             { return note; }
    public void      setNote(String n)     { this.note = n; }
}
