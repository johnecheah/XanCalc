package com.example.math

import kotlin.math.*

object ExpressionEvaluator {
    fun evaluate(expression: String, isRadians: Boolean = true): Double {
        val sanitized = preprocess(expression)
        if (sanitized.trim().isEmpty()) return 0.0
        val trimmed = sanitized.trim()

        // Calculator convention: "100+50%" means "100 + 50% OF 100" (=150), not
        // "100 + 0.5" (=100.5). This only applies when the "%" is the very last character
        // of the whole expression, sitting after a top-level +/- (not inside parentheses,
        // and not a unary sign). Everywhere else "%" still just means "divide by 100"
        // (e.g. "100×50%" = 50, "50%" alone = 0.5) — that's handled by the parser as before.
        if (trimmed.endsWith("%")) {
            val opMatch = findTopLevelAdditiveOpIndex(trimmed.dropLast(1))
            if (opMatch != null) {
                val (opChar, opPos) = opMatch
                val leftStr = trimmed.substring(0, opPos)
                val rightStr = trimmed.substring(opPos + 1, trimmed.length - 1)
                if (leftStr.isNotBlank() && rightStr.isNotBlank()) {
                    val leftVal = Parser(leftStr, isRadians).parse()
                    val rightVal = Parser(rightStr, isRadians).parse()
                    val percentOfLeft = leftVal * (rightVal / 100.0)
                    return if (opChar == '+') leftVal + percentOfLeft else leftVal - percentOfLeft
                }
            }
        }

        return Parser(trimmed, isRadians).parse()
    }

    // Finds the rightmost top-level '+' or '-' in the string (ignoring anything inside
    // parentheses, and skipping unary +/- that follow another operator, '(' or the start
    // of the string). Returns the operator and its index, or null if there isn't one.
    private fun findTopLevelAdditiveOpIndex(s: String): Pair<Char, Int>? {
        var depth = 0
        for (i in s.length - 1 downTo 0) {
            when (val c = s[i]) {
                ')' -> depth++
                '(' -> depth--
                '+', '-' -> {
                    if (depth == 0) {
                        var j = i - 1
                        while (j >= 0 && s[j] == ' ') j--
                        val isUnary = j < 0 || s[j] in "+-*/^("
                        if (!isUnary) return Pair(c, i)
                    }
                }
            }
        }
        return null
    }

    // NOTE: "π" and "e" are no longer blindly string-replaced here. Doing that merged them
    // into whatever digit sat next to them (e.g. "7e" -> "72.718281828459045") instead of
    // multiplying. They're now parsed as proper tokens in Parser.parseFactor().
    private fun preprocess(expr: String): String {
        return expr
            .replace(",", "")
            .replace("×", "*")
            .replace("÷", "/")
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

        // True if the upcoming (non-space) character could begin a new factor. Used to support
        // implicit multiplication for juxtaposed tokens like "7e", "7π", "2(3)", "3sin(30)".
        private fun canStartFactor(): Boolean {
            while (ch == ' '.code) nextChar()
            return (ch >= '0'.code && ch <= '9'.code) || ch == '.'.code ||
                (ch >= 'a'.code && ch <= 'z'.code) || ch == 'π'.code || ch == '('.code
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) x *= parseFactor()
                else if (eat('/'.code)) {
                    val divisor = parseFactor()
                    if (divisor == 0.0) throw ArithmeticException("Division by zero")
                    x /= divisor
                } else if (canStartFactor()) {
                    // Implicit multiplication, e.g. "7e" -> 7 * e, "2(3)" -> 2 * 3
                    x *= parseFactor()
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
            } else if (ch == 'π'.code) {
                nextChar()
                x = PI
            } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                val numStr = str.substring(startPos, this.pos)
                x = numStr.toDouble()
            } else if (ch >= 'a'.code && ch <= 'z'.code) {
                while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                val func = str.substring(startPos, this.pos)
                if (func == "e") {
                    // Euler's constant - not a function, takes no argument.
                    x = E
                } else {
                    x = if (eat('('.code)) {
                        val arg = parseExpression()
                        eat(')'.code)
                        arg
                    } else {
                        parseFactor()
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
            // Use a tolerance instead of exact equality: results piped in from earlier float
            // arithmetic (e.g. 15/3) can land on 4.999999999999999 instead of exactly 5.0.
            val rounded = Math.round(n).toDouble()
            if (abs(n - rounded) > 1e-9) throw ArithmeticException("Factorial of decimal values")
            val intN = rounded.toInt()
            if (intN > 170) return Double.POSITIVE_INFINITY
            var result = 1.0
            for (i in 1..intN) {
                result *= i
            }
            return result
        }
    }
}
