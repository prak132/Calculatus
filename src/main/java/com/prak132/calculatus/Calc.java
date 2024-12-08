package com.prak132.calculatus;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod(modid = Calc.MODID, name = Calc.NAME, version = Calc.VERSION, clientSideOnly = true)
public class Calc {
    public static final String MODID = "calculatus";
    public static final String NAME = "Calculatus";
    public static final String VERSION = "1.0.3";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        System.out.println(NAME + " initialized!");
        ClientCommandHandler.instance.registerCommand(new CalcCommand());
    }

    static class CalcCommand extends net.minecraft.command.CommandBase {
        @Override
        public String getCommandName() {
            return "calc";
        }

        @Override
        public String getCommandUsage(net.minecraft.command.ICommandSender sender) {
            return "/calc <expression>";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public void processCommand(net.minecraft.command.ICommandSender sender, String[] args) {
            if (args.length == 0) {
                sendMessage("Usage: /calc <expression>, /calc history, or /calc clear", true);
                return;
            }

            if (args[0].equalsIgnoreCase("clear")) {
                CalculationHistory.clearHistory();
                sendMessage("Calculation history cleared.", true);
                return;
            }

            if (args[0].equalsIgnoreCase("history")) {
                List<String> history = CalculationHistory.getHistory();
                if (history.isEmpty()) {
                    sendMessage("No calculations in history.", true);
                } else {
                    for (int i = 0; i < history.size(); i++) {
                        sendMessage((i + 1) + ": " + history.get(i), false);
                    }
                }
                return;
            }

            String expression = String.join(" ", args).replaceAll("\\s+", "");
            try {
                int x = (int) Math.floor(Minecraft.getMinecraft().thePlayer.posX);
                int y = (int) Math.floor(Minecraft.getMinecraft().thePlayer.posY);
                int z = (int) Math.floor(Minecraft.getMinecraft().thePlayer.posZ);
                double result = evaluateExpression(expression, x, y, z);
                DecimalFormat formatter = new DecimalFormat("#,###.##");
                CalculationHistory.addEntry(expression + " = " + formatter.format(result));
                sendMessage(expression + " = " + formatter.format(result), false);
            } catch (Exception e) {
                String suggestion = suggestFix(expression, e.getMessage());
                sendMessage("Error: " + e.getMessage() + (suggestion.isEmpty() ? "" : " Did you mean: " + suggestion + "?"), true);
            }
        }


        private String suggestFix(String expression, String errorMessage) {
            if (errorMessage.contains("Division by zero")) {
                return expression.replaceAll("/0(\\D|$)", "/1$1");
            }
            else if (errorMessage.contains("Unexpected character")) {
                return expression.replaceAll("[^0-9+\\-*/().^%xyz]", "");
            }
            else if (errorMessage.contains("Incomplete expression")) {
                char lastChar = expression.isEmpty() ? '\0' : expression.charAt(expression.length() - 1);
                if (Character.isDigit(lastChar) || lastChar == ')') {
                    return expression + "+0";
                } else {
                    return expression + "0";
                }
            }
            return expression;
        }

        private double evaluateExpression(String expression, int x, int y, int z) {
            if (!expression.matches("^[0-9+\\-*/().^%xyzkmbt]+$")) {
                throw new IllegalArgumentException("Invalid characters in expression");
            }
            expression = expression.replace("^", "^")
                    .replaceAll("(?i)x", String.valueOf(x))
                    .replaceAll("(?i)y", String.valueOf(y))
                    .replaceAll("(?i)z", String.valueOf(z))
                    .replaceAll("(?i)k", "* 1000")
                    .replaceAll("(?i)m", "* 1000000")
                    .replaceAll("(?i)b", "* 1000000000")
                    .replaceAll("(?i)t", "* 1000000000000");
            return parseExpression(expression);
        }

        private double parseExpression(String expression) {
            ExpressionParser parser = new ExpressionParser(expression);
            return parser.parse();
        }

        private void sendMessage(String message, boolean isError) {
            try {
                IChatComponent baseComponent;
                if (isError) {
                    baseComponent = new ChatComponentText("§b[CCM] ")
                            .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.BLUE));
                    IChatComponent errorComponent = new ChatComponentText(message)
                            .setChatStyle(new ChatStyle()
                                    .setColor(EnumChatFormatting.RED)
                                    .setChatClickEvent(new net.minecraft.event.ClickEvent(
                                            net.minecraft.event.ClickEvent.Action.SUGGEST_COMMAND,
                                            message
                                    ))
                                    .setChatHoverEvent(new net.minecraft.event.HoverEvent(
                                            net.minecraft.event.HoverEvent.Action.SHOW_TEXT,
                                            new ChatComponentText("Click to copy error")
                                    ))
                            );
                    baseComponent.appendSibling(errorComponent);
                } else {
                    String[] parts = message.split(" = ", 2);
                    String expression = parts.length > 0 ? parts[0] : "Unknown Expression";
                    String result = parts.length > 1 ? parts[1] : "Unknown Result";
                    baseComponent = new ChatComponentText("§b[CCM] " + expression);
                    IChatComponent resultComponent = new ChatComponentText(" = " + result + " [COPY]")
                            .setChatStyle(new ChatStyle()
                                    .setColor(EnumChatFormatting.YELLOW)
                                    .setChatClickEvent(new net.minecraft.event.ClickEvent(
                                            net.minecraft.event.ClickEvent.Action.SUGGEST_COMMAND,
                                            result
                                    ))
                                    .setChatHoverEvent(new net.minecraft.event.HoverEvent(
                                            net.minecraft.event.HoverEvent.Action.SHOW_TEXT,
                                            new ChatComponentText("Click to copy result")
                                    ))
                            );
                    baseComponent.appendSibling(resultComponent);
                }
                Minecraft.getMinecraft().thePlayer.addChatMessage(baseComponent);
            } catch (Exception e) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText("§c[CCM] Failed to send message: " + e.getMessage())
                );
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

            public ExpressionParser(String expr) {
                this(expr, null);
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
                        x *= nextFactor;
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
                    x = Math.pow(x, parseFactor());
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
}

