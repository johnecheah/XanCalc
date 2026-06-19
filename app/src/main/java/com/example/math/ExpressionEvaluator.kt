package com.example.math

import kotlin.math.*

object ExpressionEvaluator {
    fun evaluate(expression: String, isRadians: Boolean = true): Double {
        val sanitized = preprocess(expression)
        if (sanitized.trim().isEmpty()) return 0.0
        return Parser(sanitized, isRadians).parse()
    }

    private fun preprocess(expr: String): String {
        return expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", "3.141592653589793")
            .replace("e", "2.718281828459045")
            .replace("√", "sqrt")
    }

    private class Parser(val str: String, val isRadians: Boolean) {
        var pos = -1
        var ch = 0

        fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw RuntimeException("Unexpected character: " + ch.toChar())
            return x
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+'.code)) x += parseTerm()
                else if (eat('-'.code)) x -= parseTerm()
                else break
            }
            return x
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) x *= parseFactor()
                else if (eat('/'.code)) {
                    val divisor = parseFactor()
                    if (divisor == 0.0) throw ArithmeticException("Division by zero")
                    x /= divisor
                } else break
            }
            return x
        }

        fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = this.pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                val numStr = str.substring(startPos, this.pos)
                x = numStr.toDouble()
            } else if (ch >= 'a'.code && ch <= 'z'.code) {
                while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                val func = str.substring(startPos, this.pos)
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else {
                    x = parseFactor()
                }
                
                x = when (func) {
                    "sin" -> if (isRadians) sin(x) else sin(Math.toRadians(x))
                    "cos" -> if (isRadians) cos(x) else cos(Math.toRadians(x))
                    "tan" -> if (isRadians) tan(x) else tan(Math.toRadians(x))
                    "asin" -> if (isRadians) asin(x) else Math.toDegrees(asin(x))
                    "acos" -> if (isRadians) acos(x) else Math.toDegrees(acos(x))
                    "atan" -> if (isRadians) atan(x) else Math.toDegrees(atan(x))
                    "ln" -> ln(x)
                    "log" -> log10(x)
                    "sqrt" -> {
                        if (x < 0.0) throw ArithmeticException("Square root of negative number")
                        sqrt(x)
                    }
                    "cbrt" -> Math.cbrt(x)
                    else -> throw RuntimeException("Unknown function: $func")
                }
            } else {
                throw RuntimeException("Unexpected character: " + ch.toChar())
            }

            while (true) {
                if (eat('^'.code)) {
                    val exponent = parseFactor()
                    x = x.pow(exponent)
                } else if (eat('%'.code)) {
                    x /= 100.0
                } else if (eat('!'.code)) {
                    x = factorial(x)
                } else {
                    break
                }
            }

            return x
        }

        private fun factorial(n: Double): Double {
            if (n < 0.0) throw ArithmeticException("Factorial of negative values")
            if (n != floor(n)) throw ArithmeticException("Factorial of decimal values")
            val intN = n.toInt()
            if (intN > 170) return Double.POSITIVE_INFINITY
            var result = 1.0
            for (i in 1..intN) {
                result *= i
            }
            return result
        }
    }
}
