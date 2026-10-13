package com.utilitynest.neoncalc

import java.util.Locale
import kotlin.math.abs

/**
 * A dependency-free recursive-descent expression evaluator written in plain
 * Kotlin. Supports + - * / ( ) decimals and unary +/-.
 *
 * The '%' key is treated as a postfix percent operator, so "50%" evaluates
 * to 0.5 (matching how simple calculator apps behave).
 */
class ExpressionEvaluator {

    class EvalException(message: String) : Exception(message)

    private var src: String = ""
    private var pos: Int = 0

    fun evaluate(expression: String): Double {
        src = expression
            .replace("\u00d7", "*") // ×
            .replace("\u00f7", "/") // ÷
            .replace("\u2212", "-") // −
            .replace(",", "")
            .replace(" ", "")
        pos = 0
        if (src.isEmpty()) throw EvalException("Empty")
        val value = parseExpression()
        if (pos < src.length) throw EvalException("Unexpected '" + src[pos] + "'")
        if (value.isNaN() || value.isInfinite()) throw EvalException("Invalid result")
        return value
    }

    private fun parseExpression(): Double {
        var value = parseTerm()
        while (pos < src.length && (src[pos] == '+' || src[pos] == '-')) {
            val op = src[pos]
            pos++
            val rhs = parseTerm()
            value = if (op == '+') value + rhs else value - rhs
        }
        return value
    }

    private fun parseTerm(): Double {
        var value = parseUnary()
        while (pos < src.length) {
            val c = src[pos]
            if (c == '*' || c == '/') {
                pos++
                val rhs = parseUnary()
                if (c == '/') {
                    if (rhs == 0.0) throw EvalException("Cannot divide by 0")
                    value /= rhs
                } else {
                    value *= rhs
                }
            } else if (c == '(' || c.isDigit() || c == '.') {
                // Implicit multiplication: 5(2), (3)(4) and (2)3 all mean "times".
                value *= parseUnary()
            } else {
                break
            }
        }
        return value
    }

    private fun parseUnary(): Double {
        if (pos < src.length && (src[pos] == '+' || src[pos] == '-')) {
            val op = src[pos]
            pos++
            val value = parseUnary()
            return if (op == '-') -value else value
        }
        return parsePercent()
    }

    private fun parsePercent(): Double {
        var value = parsePrimary()
        while (pos < src.length && src[pos] == '%') {
            pos++
            value *= 0.01
        }
        return value
    }

    private fun parsePrimary(): Double {
        if (pos >= src.length) throw EvalException("Incomplete expression")
        val c = src[pos]
        if (c == '(') {
            pos++
            val value = parseExpression()
            if (pos >= src.length || src[pos] != ')') throw EvalException("Missing ')'")
            pos++
            return value
        }
        if (c.isDigit() || c == '.') {
            val start = pos
            var dots = 0
            while (pos < src.length && (src[pos].isDigit() || src[pos] == '.')) {
                if (src[pos] == '.') dots++
                pos++
            }
            if (dots > 1) throw EvalException("Invalid number")
            return src.substring(start, pos).toDoubleOrNull()
                ?: throw EvalException("Invalid number")
        }
        throw EvalException("Unexpected '" + c + "'")
    }

    companion object {
        fun format(value: Double): String {
            if (value.isNaN() || value.isInfinite()) return "Error"
            if (abs(value) < 1e15 && value == value.toLong().toDouble()) {
                return value.toLong().toString()
            }
            var text = String.format(Locale.US, "%.10f", value)
            text = text.trimEnd('0').trimEnd('.')
            if (text.isEmpty() || text == "-") text = "0"
            return text
        }
    }
}
