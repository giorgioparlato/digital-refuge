package app.bedtime.data

import java.math.BigDecimal
import java.math.MathContext

/**
 * Sums typed into the search: + − × ÷ (or * / x), ^ for powers, % for percent, brackets and decimals.
 * Only something with at least one operation counts, so a plain number stays a search.
 */
object Calculator {
    /** The answer to [text], or null if it isn't a sum. */
    fun evaluate(text: String): Double? {
        val t = text.trim().replace(',', '.')
        if (t.isEmpty() || t.none { it in "+-−*/×÷x^%" } || t.any { it !in "0123456789.+-−*/×÷x^%() " }) return null
        if (t.trimStart('-', '−').none { it in "+-−*/×÷x^%" }) return null
        return runCatching { Parser(t.filterNot { it == ' ' }).parse() }.getOrNull()?.takeIf { it.isFinite() }
    }

    /** The answer as it reads: no ".0" on whole numbers, at most ten significant figures. */
    fun format(value: Double): String =
        BigDecimal(value).round(MathContext(10)).stripTrailingZeros().toPlainString().let { if (it == "-0") "0" else it }

    private class Parser(val s: String) {
        var at = 0

        fun parse(): Double = sum().also { require(at == s.length) }

        private fun peek() = s.getOrNull(at)

        private fun sum(): Double {
            var v = product()
            while (true) {
                v = when (peek()) {
                    '+' -> { at++; v + product() }
                    '-', '−' -> { at++; v - product() }
                    else -> return v
                }
            }
        }

        private fun product(): Double {
            var v = power()
            while (true) {
                v = when (peek()) {
                    '*', '×', 'x' -> { at++; v * power() }
                    '/', '÷' -> { at++; v / power() }
                    else -> return v
                }
            }
        }

        private fun power(): Double {
            val base = unary()
            return if (peek() == '^') { at++; Math.pow(base, power()) } else base
        }

        private fun unary(): Double = when (peek()) {
            '-', '−' -> { at++; -unary() }
            '+' -> { at++; unary() }
            else -> percent()
        }

        private fun percent(): Double {
            var v = atom()
            while (peek() == '%') { at++; v /= 100 }
            return v
        }

        private fun atom(): Double {
            if (peek() == '(') {
                at++
                val v = sum()
                require(peek() == ')')
                at++
                return v
            }
            val start = at
            while (peek()?.let { it.isDigit() || it == '.' } == true) at++
            require(at > start)
            return s.substring(start, at).toDouble()
        }
    }
}
