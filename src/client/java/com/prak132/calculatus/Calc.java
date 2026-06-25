package com.prak132.calculatus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class Calc implements ModInitializer, ClientModInitializer {

    public static final String MODID = "calculatus";

    @Override
    public void onInitialize() {
        // Common initialization if any
    }

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("calc")
                .executes(context -> {
                    sendMessage(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource.class.cast(context.getSource()), "Usage: /calc <expression> OR /calc history", true);
                    return 1;
                })
                .then(literal("clear")
                    .executes(context -> {
                        CalculationHistory.clearHistory();
                        sendMessage(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource.class.cast(context.getSource()), "Calculation history cleared.", false);
                        return 1;
                    }))
                .then(literal("history")
                    .executes(context -> {
                        List<String> history = CalculationHistory.getHistory();
                        if (history.isEmpty()) {
                            sendMessage(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource.class.cast(context.getSource()), "History is empty.", true);
                        } else {
                            sendMessage(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource.class.cast(context.getSource()), "Calculation History:", false);
                            for (String entry : history) {
                                sendMessage(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource.class.cast(context.getSource()), entry, false);
                            }
                        }
                        return 1;
                    }))
                .then(argument("expression", StringArgumentType.greedyString())
                    .executes(context -> {
                        String expression = StringArgumentType.getString(context, "expression");
                        processExpression(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource.class.cast(context.getSource()), expression);
                        return 1;
                    }))
            );
        });
    }

    private void processExpression(FabricClientCommandSource source, String expressionRaw) {
        String expression = expressionRaw.replaceAll("\\s+", "");
        try {
            int x = (int) Math.floor(source.getPosition().x);
            int y = (int) Math.floor(source.getPosition().y);
            int z = (int) Math.floor(source.getPosition().z);
            double result = evaluateExpression(expression, x, y, z);
            
            if (Double.isInfinite(result)) {
                sendMessage(source, "Error: Number overflow - result is too large to represent", true);
                return;
            }
            if (Double.isNaN(result)) {
                sendMessage(source, "Error: Invalid calculation result (NaN)", true);
                return;
            }
            
            DecimalFormat formatter = new DecimalFormat("#,###.##");
            String resultStr = formatter.format(result);
            CalculationHistory.addEntry(expression + " = " + resultStr);
            sendMessage(source, expression + " = " + resultStr, false);
        } catch (Exception e) {
            String suggestion = suggestFix(expression, e.getMessage());
            sendMessage(source, "Error: " + e.getMessage() + (suggestion.isEmpty() ? "" : " Did you mean: " + suggestion + "?"), true);
        }
    }

    private String suggestFix(String expression, String errorMessage) {
        if (errorMessage == null) errorMessage = "";
        if (errorMessage.contains("Division by zero")) {
            return expression.replaceAll("/0(\\D|$)", "/1$1");
        }
        else if (errorMessage.contains("Modulo by zero")) {
            return expression.replaceAll("%0(\\D|$)", "%1$1");
        }
        else if (errorMessage.contains("Unexpected character")) {
            return expression.replaceAll("[^0-9+\\-*/().^%xyzkmbt]", "");
        }
        else if (errorMessage.contains("Incomplete expression")) {
            char lastChar = expression.isEmpty() ? '\0' : expression.charAt(expression.length() - 1);
            if (Character.isDigit(lastChar) || lastChar == ')') {
                return expression + "+0";
            } else {
                return expression + "0";
            }
        }
        else if (errorMessage.contains("Mismatched parentheses")) {
            long openCount = expression.chars().filter(ch -> ch == '(').count();
            long closeCount = expression.chars().filter(ch -> ch == ')').count();
            if (openCount > closeCount) {
                StringBuilder sb = new StringBuilder(expression);
                for (int i = 0; i < (openCount - closeCount); i++) {
                    sb.append(')');
                }
                return sb.toString();
            }
        }
        return "";
    }

    private double evaluateExpression(String expression, int x, int y, int z) {
        if (!expression.matches("^[0-9+\\-*/().^%xyzkmbt]+$")) {
            throw new IllegalArgumentException("Invalid characters in expression");
        }

        Map<String, Double> variables = new HashMap<>();
        variables.put("x", (double) x);
        variables.put("y", (double) y);
        variables.put("z", (double) z);

        expression = expression.replace("^", "^")
                .replaceAll("(?i)k(?![a-z])", "*1000")
                .replaceAll("(?i)m(?![a-z])", "*1000000")
                .replaceAll("(?i)b(?![a-z])", "*1000000000")
                .replaceAll("(?i)t(?![a-z])", "*1000000000000");

        ExpressionParser parser = new ExpressionParser(expression, variables);
        return parser.parse();
    }

    private void sendMessage(FabricClientCommandSource source, String message, boolean isError) {
        try {
            MutableComponent baseComponent;
            if (isError) {
                baseComponent = Component.literal("§b[CCM] ").setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA));
                MutableComponent errorComponent = Component.literal(message)
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
                baseComponent.append(errorComponent);
            } else {
                String[] parts = message.split(" = ", 2);
                String expression = parts.length > 0 ? parts[0] : "Unknown Expression";
                String result = parts.length > 1 ? parts[1] : "Unknown Result";
                
                baseComponent = Component.literal("§b[CCM] " + expression);
                MutableComponent resultComponent = Component.literal(" = " + result)
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
                baseComponent.append(resultComponent);
            }
            source.sendFeedback(baseComponent);
        } catch (Exception e) {
            source.sendFeedback(Component.literal("§c[CCM] Failed to send message: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    static class ExpressionParser {
        private int pos = -1;
        private int ch;
        private final String expr;
        private final Map<String, Double> variables;

        public ExpressionParser(String expr, Map<String, Double> variables) {
            this.expr = expr;
            this.variables = variables != null ? variables : new HashMap<>();
        }

        private void nextChar() {
            ch = (++pos < expr.length()) ? expr.charAt(pos) : -1;
        }

        public double parse() {
            nextChar();
            double x = parseExpression();
            if (pos < expr.length()) {
                throw new RuntimeException("Invalid expression at position " + pos + ": '" + expr.substring(pos) + "'");
            }
            return x;
        }

        private double parseExpression() {
            double x = parseTerm();
            for (;;) {
                if (eat('+')) {
                    double nextTerm = parseTerm();
                    x += nextTerm;
                }
                else if (eat('-')) {
                    double nextTerm = parseTerm();
                    x -= nextTerm;
                }
                else return x;
            }
        }

        private double parseTerm() {
            double x = parseFactor();
            for (;;) {
                if (eat('*')) {
                    double nextFactor = parseFactor();
                    if (Math.abs(x) > 1e150 || Math.abs(nextFactor) > 1e150) {
                        throw new ArithmeticException("Potential overflow in multiplication");
                    }
                    x *= nextFactor;
                    if (Double.isInfinite(x)) {
                        throw new ArithmeticException("Multiplication resulted in overflow (infinity)");
                    }
                }
                else if (eat('/')) {
                    double nextFactor = parseFactor();
                    if (nextFactor == 0) {
                        throw new ArithmeticException("Division by zero");
                    }
                    x /= nextFactor;
                }
                else if (eat('%')) {
                    double nextFactor = parseFactor();
                    if (nextFactor == 0) {
                        throw new ArithmeticException("Modulo by zero");
                    }
                    x = ((x % nextFactor) + nextFactor) % nextFactor;
                }
                else return x;
            }
        }

        private double parseFactor() {
            if (eat('+')) return parseFactor();
            if (eat('-')) return -parseFactor();
            double x;
            int startPos = this.pos;
            if (eat('(')) {
                x = parseExpression();
                if (!eat(')')) {
                    throw new RuntimeException("Mismatched parentheses");
                }
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                x = Double.parseDouble(expr.substring(startPos, this.pos));
            } else if (ch >= 'a' && ch <= 'z') {
                while (ch >= 'a' && ch <= 'z') nextChar();
                String var = expr.substring(startPos, this.pos);
                if (!variables.containsKey(var)) {
                    throw new RuntimeException("Unknown variable: " + var);
                }
                x = variables.get(var);
            } else {
                throw new RuntimeException("Unexpected character: " + (char) ch);
            }
            while (eat('^')) {
                double exponent = parseFactor();
                if (Math.abs(x) > 1e10 && Math.abs(exponent) > 10) {
                    throw new ArithmeticException("Potential overflow in exponentiation: base=" + x + ", exponent=" + exponent);
                }
                x = Math.pow(x, exponent);
                if (Double.isInfinite(x)) {
                    throw new ArithmeticException("Exponentiation resulted in overflow (infinity)");
                }
                if (Double.isNaN(x)) {
                    throw new ArithmeticException("Exponentiation resulted in invalid number (NaN)");
                }
            }
            return x;
        }

        private boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }
    }
}
