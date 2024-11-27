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

@Mod(modid = Calc.MODID, name = Calc.NAME, version = Calc.VERSION, clientSideOnly = true)
public class Calc {
    public static final String MODID = "calculatus";
    public static final String NAME = "Calculator Mod";
    public static final String VERSION = "1.0";

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
                sendMessage("Usage: /calc <expression>", false);
                return;
            }
            String expression = String.join(" ", args).replaceAll("\\s+", "");
            try {
                double result = evaluateExpression(expression);
                DecimalFormat formatter = new DecimalFormat("#,###.##");
                sendMessage("Result: " + formatter.format(result), false);
            } catch (Exception e) {
                sendMessage("Error: " + e.getMessage(), true);
            }
        }

        private double evaluateExpression(String expression) {
            if (!expression.matches("^[0-9+\\-*/().^%]+$")) {
                throw new IllegalArgumentException("Invalid characters in expression");
            }
            expression = expression.replace("^", "^");
            return parseExpression(expression);
        }

        private double parseExpression(String expression) {
            ExpressionParser parser = new ExpressionParser(expression);
            return parser.parse();
        }

        private void sendMessage(String message, boolean isError) {
            IChatComponent baseComponent;
            if (isError) {
                baseComponent = new ChatComponentText("§b[CALCMOD] ")
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
            } else if (message.startsWith("Result: ")) {
                String[] parts = message.split("Result: ", 2);
                baseComponent = new ChatComponentText("§b[CALCMOD] " + parts[0]);
                IChatComponent resultComponent = new ChatComponentText(parts[1])
                        .setChatStyle(new ChatStyle()
                                .setColor(EnumChatFormatting.YELLOW)
                                .setChatClickEvent(new net.minecraft.event.ClickEvent(
                                        net.minecraft.event.ClickEvent.Action.SUGGEST_COMMAND,
                                        parts[1]
                                ))
                                .setChatHoverEvent(new net.minecraft.event.HoverEvent(
                                        net.minecraft.event.HoverEvent.Action.SHOW_TEXT,
                                        new ChatComponentText("Click to copy")
                                ))
                        );
                baseComponent.appendSibling(resultComponent);
            } else {
                baseComponent = new ChatComponentText("§b[CALCMOD] " + message);
            }
            Minecraft.getMinecraft().thePlayer.addChatMessage(baseComponent);
        }

        static class ExpressionParser {
            private int pos = -1;
            private int ch;
            private final String expr;

            public ExpressionParser(String expr) {
                this.expr = expr;
            }

            private void nextChar() {
                ch = (++pos < expr.length()) ? expr.charAt(pos) : -1;
            }

            public double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < expr.length()) {
                    throw new RuntimeException("Invalid expression: unexpected characters at end");
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
                    int parenStart = pos - 1;
                    x = parseExpression();
                    if (!eat(')')) {
                        pos = parenStart;
                        throw new RuntimeException("Mismatched parentheses");
                    }
                }
                else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    if (startPos == pos) {
                        throw new RuntimeException("Invalid number format");
                    }
                    x = Double.parseDouble(expr.substring(startPos, this.pos));
                }
                else {
                    throw new RuntimeException("Unexpected character: " + (char)ch + ". Incomplete expression.");
                }
                while (eat('^')) {
                    double exponent = parseFactor();
                    x = Math.pow(x, exponent);
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
