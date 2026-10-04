package com.arpit.myapplication.finance;

import java.util.Locale;

/** Keyword based category guesser for imported transactions. */
public final class Categorizer {
    public static final String SALARY = "Salary";
    public static final String OTHER = "Other";

    /** Categories that can have a budget. */
    public static final String[] BUDGET_CATEGORIES = {
            "Food & Dining", "Groceries", "Transport", "Shopping", "Bills & Recharge",
            "Entertainment", "Health", "Rent & Housing", "Education", "Travel",
            "Personal Transfer", "Investments", OTHER
    };

    /** Categories a transaction can be tagged with. */
    public static final String[] ALL_CATEGORIES = {
            "Food & Dining", "Groceries", "Transport", "Shopping", "Bills & Recharge",
            "Entertainment", "Health", "Rent & Housing", "Education", "Travel",
            "Personal Transfer", "Investments", SALARY, OTHER
    };

    private static final String[][] RULES = {
            {"Food & Dining", "swiggy", "zomato", "restaurant", "cafe", "pizza", "burger", "dominos", "kfc",
                    "mcdonald", "bakery", "hotel", "dhaba", "juice", "tea", "chai", "food", "kitchen", "canteen"},
            {"Groceries", "bigbasket", "blinkit", "zepto", "grocery", "supermarket", "mart", "dmart", "kirana",
                    "vegetable", "fruit", "milk", "dairy", "provision", "general store"},
            {"Transport", "uber", "ola", "rapido", "petrol", "petroleum", "fuel", "metro", "irctc", "railway",
                    "parking", "pay and park", "fastag", "toll", "bus", "auto"},
            {"Shopping", "amazon", "flipkart", "myntra", "ajio", "meesho", "mall", "store", "fashion", "shop"},
            {"Bills & Recharge", "airtel", "jio", "vodafone", "vi ", "recharge", "electricity", "broadband",
                    "bill", "gas", "water", "dth", "insurance", "bsnl"},
            {"Entertainment", "netflix", "hotstar", "spotify", "prime video", "bookmyshow", "movie", "cinema",
                    "youtube", "game", "pvr", "inox"},
            {"Health", "pharmacy", "medical", "hospital", "clinic", "apollo", "medplus", "doctor", "lab",
                    "diagnostic", "gym"},
            {"Rent & Housing", "rent", "landlord", "society", "maintenance", "housing"},
            {"Education", "school", "college", "tuition", "course", "udemy", "coursera", "exam", "fees",
                    "university", "books"},
            {"Travel", "makemytrip", "goibibo", "cleartrip", "airline", "flight", "indigo", "oyo", "booking.com",
                    "yatra"},
            {"Investments", "zerodha", "groww", "upstox", "mutual fund", "sip", "nps", "stock", "gold",
                    "kuvera", "coin"},
    };

    private Categorizer() {}

    public static String categorize(String counterparty, String note, boolean received) {
        String text = ((counterparty == null ? "" : counterparty) + " " + (note == null ? "" : note))
                .toLowerCase(Locale.ROOT);
        if (received) {
            if (text.contains("salary") || text.contains("payroll")) return SALARY;
            return OTHER;
        }
        for (String[] rule : RULES) {
            for (int i = 1; i < rule.length; i++) {
                if (text.contains(rule[i])) return rule[0];
            }
        }
        return OTHER;
    }
}
