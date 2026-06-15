package com.prak132.calculatus;

import java.util.ArrayList;
import java.util.List;

public class CalculationHistory {
    private static final int MAX_HISTORY = 10;
    private static final List<String> history = new ArrayList<>();

    public static void addEntry(String entry) {
        if (history.size() >= MAX_HISTORY) {
            history.remove(0);
        }
        history.add(entry);
    }

    public static void clearHistory() {
        history.clear();
    }

    public static List<String> getHistory() {
        return new ArrayList<>(history);
    }
}
