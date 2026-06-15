package com.prak132.calculatus;

import org.junit.Test;
import static org.junit.Assert.*;

public class BasicTests {

    @Test
    public void testBasicArithmetic() {
        Calc.CalcCommand.ExpressionParser parser1 = new Calc.CalcCommand.ExpressionParser("2+3");
        assertEquals(5.0, parser1.parse(), 0.001);
        Calc.CalcCommand.ExpressionParser parser2 = new Calc.CalcCommand.ExpressionParser("10-4");
        assertEquals(6.0, parser2.parse(), 0.001);
        Calc.CalcCommand.ExpressionParser parser3 = new Calc.CalcCommand.ExpressionParser("3*4");
        assertEquals(12.0, parser3.parse(), 0.001);
        Calc.CalcCommand.ExpressionParser parser4 = new Calc.CalcCommand.ExpressionParser("12/3");
        assertEquals(4.0, parser4.parse(), 0.001);
    }

    @Test
    public void testOrderOfOperations() {
        Calc.CalcCommand.ExpressionParser parser1 = new Calc.CalcCommand.ExpressionParser("2+3*4");
        assertEquals(14.0, parser1.parse(), 0.001);
        Calc.CalcCommand.ExpressionParser parser2 = new Calc.CalcCommand.ExpressionParser("(2+3)*4");
        assertEquals(20.0, parser2.parse(), 0.001);
    }

    @Test
    public void testExtraNumbers() {
        String expression = "2m + 1";
        expression = expression.replaceAll("(?i)k", "* 1000")
                .replaceAll("(?i)m", "* 1000000")
                .replaceAll("(?i)b", "* 1000000000")
                .replaceAll("(?i)t", "* 1000000000000");
        Calc.CalcCommand.ExpressionParser parser = new Calc.CalcCommand.ExpressionParser(expression);
        assertEquals(2000001.0, parser.parse(), 0.001);
        String expression2 = "2t + 1";
        expression2 = expression2.replaceAll("(?i)k", "* 1000")
                .replaceAll("(?i)m", "* 1000000")
                .replaceAll("(?i)b", "* 1000000000")
                .replaceAll("(?i)t", "* 1000000000000");
        Calc.CalcCommand.ExpressionParser parser2 = new Calc.CalcCommand.ExpressionParser(expression2);
        assertEquals(2000000000001.0, parser2.parse(), 0.001);
    }


    @Test
    public void testExponentiation() {
        Calc.CalcCommand.ExpressionParser parser1 = new Calc.CalcCommand.ExpressionParser("2^3");
        assertEquals(8.0, parser1.parse(), 0.001);
        Calc.CalcCommand.ExpressionParser parser2 = new Calc.CalcCommand.ExpressionParser("2^3^2");
        assertEquals(512.0, parser2.parse(), 0.001);
        Calc.CalcCommand.ExpressionParser parser3 = new Calc.CalcCommand.ExpressionParser("(2^3)^2");
        assertEquals(64.0, parser3.parse(), 0.001);
    }

    @Test
    public void testNegativeNumbers() {
        Calc.CalcCommand.ExpressionParser parser1 = new Calc.CalcCommand.ExpressionParser("-5+3");
        assertEquals(-2.0, parser1.parse(), 0.001);
        Calc.CalcCommand.ExpressionParser parser2 = new Calc.CalcCommand.ExpressionParser("5*-3");
        assertEquals(-15.0, parser2.parse(), 0.001);
    }

    @Test
    public void testModuloExpressions() {
        Calc.CalcCommand.ExpressionParser parser1 = new Calc.CalcCommand.ExpressionParser("50%10");
        assertEquals(0.0, parser1.parse(), 0.001);
        Calc.CalcCommand.ExpressionParser parser2 = new Calc.CalcCommand.ExpressionParser("9%2");
        assertEquals(1, parser2.parse(), 0.001);
        Calc.CalcCommand.ExpressionParser parser3 = new Calc.CalcCommand.ExpressionParser("2173781328%43");
        assertEquals(6, parser3.parse(), 0.001);
        Calc.CalcCommand.ExpressionParser parser4 = new Calc.CalcCommand.ExpressionParser("-129%4");
        assertEquals(3, parser4.parse(), 0.001);
        Calc.CalcCommand.ExpressionParser parser5 = new Calc.CalcCommand.ExpressionParser("14%5-19");
        assertEquals(-15, parser5.parse(), 0.001);
    }

    @Test
    public void testComplexExpressions() {
        Calc.CalcCommand.ExpressionParser parser1 = new Calc.CalcCommand.ExpressionParser("(2+3*2)^2");
        assertEquals(64.0, parser1.parse(), 0.001);
        Calc.CalcCommand.ExpressionParser parser2 = new Calc.CalcCommand.ExpressionParser("10/2+3*4");
        assertEquals(17.0, parser2.parse(), 0.001);
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidCharacters() {
        Calc.CalcCommand.ExpressionParser parser = new Calc.CalcCommand.ExpressionParser("2+a");
        parser.parse();
    }

    @Test(expected = RuntimeException.class)
    public void testMismatchedParentheses() {
        Calc.CalcCommand.ExpressionParser parser = new Calc.CalcCommand.ExpressionParser("(2+3");
        parser.parse();
    }

    @Test(expected = ArithmeticException.class)
    public void testDivisionByZero() {
        Calc.CalcCommand.ExpressionParser parser = new Calc.CalcCommand.ExpressionParser("5/0");
        parser.parse();
    }

    @Test(expected = RuntimeException.class)
    public void testIncompleteExpression1() {
        Calc.CalcCommand.ExpressionParser parser = new Calc.CalcCommand.ExpressionParser("10+");
        parser.parse();
    }

    @Test(expected = RuntimeException.class)
    public void testIncompleteExpression2() {
        Calc.CalcCommand.ExpressionParser parser = new Calc.CalcCommand.ExpressionParser("10+28/");
        parser.parse();
    }

    @Test(expected = RuntimeException.class)
    public void testTrailingCharacters() {
        Calc.CalcCommand.ExpressionParser parser = new Calc.CalcCommand.ExpressionParser("2+3x");
        parser.parse();
    }

    @Test
    public void testDecimalNumbers() {
        Calc.CalcCommand.ExpressionParser parser1 = new Calc.CalcCommand.ExpressionParser("3.5+2.5");
        assertEquals(6.0, parser1.parse(), 0.001);
        Calc.CalcCommand.ExpressionParser parser2 = new Calc.CalcCommand.ExpressionParser("2.5*4");
        assertEquals(10.0, parser2.parse(), 0.001);
    }

    @Test
    public void testParenthesesAndComplexOperations() {
        Calc.CalcCommand.ExpressionParser parser1 = new Calc.CalcCommand.ExpressionParser("(2+3)*4-5");
        assertEquals(15.0, parser1.parse(), 0.001);
        Calc.CalcCommand.ExpressionParser parser2 = new Calc.CalcCommand.ExpressionParser("2^(3+2)");
        assertEquals(32.0, parser2.parse(), 0.001);
    }
}