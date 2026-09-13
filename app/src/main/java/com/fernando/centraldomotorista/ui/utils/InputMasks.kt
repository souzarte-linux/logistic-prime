package com.fernando.centraldomotorista.ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CurrencyVisualTransformation(
    private val prefix: String = "R$ "
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formattedText = "$prefix$originalText"

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return offset + prefix.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                return if (offset <= prefix.length) 0 else (offset - prefix.length).coerceAtMost(originalText.length)
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}

class SuffixVisualTransformation(
    private val suffix: String
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formattedText = "$originalText$suffix"

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return offset.coerceAtMost(originalText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                return offset.coerceAtMost(originalText.length)
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}

class KmVisualTransformation(
    private val suffix: String = " KM"
) : VisualTransformation {
    private val symbols = DecimalFormatSymbols(Locale("pt", "BR")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    private val decimalFormat = DecimalFormat("#,##0.###", symbols)

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        // Format integer part with dots and maintain suffix
        val parts = raw.split(',', '.')
        val integerPart = parts[0].toLongOrNull()
        val formattedInt = if (integerPart != null) decimalFormat.format(integerPart) else parts[0]
        val formatted = if (parts.size > 1) {
            "$formattedInt,${parts[1]}$suffix"
        } else {
            "$formattedInt$suffix"
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return formatted.length.coerceAtMost(formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                return raw.length.coerceAtMost(raw.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

class CepVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }.take(8)
        val formatted = StringBuilder()
        val origToTrans = IntArray(raw.length + 1)
        val transToOrig = ArrayList<Int>()

        for (i in 0 until raw.length) {
            origToTrans[i] = formatted.length
            if (i == 5) {
                transToOrig.add(i)
                formatted.append('-')
            }
            transToOrig.add(i)
            formatted.append(raw[i])
        }
        origToTrans[raw.length] = formatted.length
        transToOrig.add(raw.length)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, raw.length)
                return origToTrans[clamped]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                return transToOrig[clamped]
            }
        }
        return TransformedText(AnnotatedString(formatted.toString()), offsetMapping)
    }
}

class CnpjVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }.take(14)
        val formatted = StringBuilder()
        val origToTrans = IntArray(raw.length + 1)
        val transToOrig = ArrayList<Int>()

        for (i in 0 until raw.length) {
            origToTrans[i] = formatted.length
            if (i == 2 || i == 5) {
                transToOrig.add(i)
                formatted.append('.')
            } else if (i == 8) {
                transToOrig.add(i)
                formatted.append('/')
            } else if (i == 12) {
                transToOrig.add(i)
                formatted.append('-')
            }
            transToOrig.add(i)
            formatted.append(raw[i])
        }
        origToTrans[raw.length] = formatted.length
        transToOrig.add(raw.length)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, raw.length)
                return origToTrans[clamped]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                return transToOrig[clamped]
            }
        }
        return TransformedText(AnnotatedString(formatted.toString()), offsetMapping)
    }
}

class PhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }.take(11)
        val formatted = StringBuilder()
        val is11Digits = raw.length > 10

        val origToTrans = IntArray(raw.length + 1)
        val transToOrig = ArrayList<Int>()

        for (i in 0 until raw.length) {
            origToTrans[i] = formatted.length
            if (i == 0) {
                transToOrig.add(0)
                formatted.append('(')
            }
            if (i == 2) {
                transToOrig.add(2)
                formatted.append(')')
                transToOrig.add(2)
                formatted.append(' ')
            }
            if (is11Digits && i == 7) {
                transToOrig.add(7)
                formatted.append('-')
            } else if (!is11Digits && i == 6) {
                transToOrig.add(6)
                formatted.append('-')
            }
            transToOrig.add(i)
            formatted.append(raw[i])
        }
        origToTrans[raw.length] = formatted.length
        transToOrig.add(raw.length)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, raw.length)
                return origToTrans[clamped]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                return transToOrig[clamped]
            }
        }
        return TransformedText(AnnotatedString(formatted.toString()), offsetMapping)
    }
}

class CpfVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }.take(11)
        val formatted = StringBuilder()
        val origToTrans = IntArray(raw.length + 1)
        val transToOrig = ArrayList<Int>()

        for (i in 0 until raw.length) {
            origToTrans[i] = formatted.length
            if (i == 3 || i == 6) {
                transToOrig.add(i)
                formatted.append('.')
            } else if (i == 9) {
                transToOrig.add(i)
                formatted.append('-')
            }
            transToOrig.add(i)
            formatted.append(raw[i])
        }
        origToTrans[raw.length] = formatted.length
        transToOrig.add(raw.length)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, raw.length)
                return origToTrans[clamped]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                return transToOrig[clamped]
            }
        }
        return TransformedText(AnnotatedString(formatted.toString()), offsetMapping)
    }
}

fun isValidCpf(cpf: String): Boolean {
    val digits = cpf.filter { it.isDigit() }
    if (digits.length != 11) return false
    if (digits.all { it == digits[0] }) return false

    val sum1 = (0..8).sumOf { i -> digits[i].digitToInt() * (10 - i) }
    val rem1 = (sum1 * 10) % 11
    val digit1 = if (rem1 == 10) 0 else rem1
    if (digit1 != digits[9].digitToInt()) return false

    val sum2 = (0..9).sumOf { i -> digits[i].digitToInt() * (11 - i) }
    val rem2 = (sum2 * 10) % 11
    val digit2 = if (rem2 == 10) 0 else rem2
    return digit2 == digits[10].digitToInt()
}

fun cleanCurrencyInput(input: String): String {
    return input.filter { it.isDigit() || it == ',' || it == '.' }.trim()
}

fun parseCurrency(text: String): BigDecimal {
    val clean = cleanCurrencyInput(text)
    if (clean.isBlank()) return BigDecimal.ZERO
    val normalized = if (clean.contains(',')) {
        clean.replace(".", "").replace(',', '.')
    } else {
        clean
    }
    return normalized.toBigDecimalOrNull() ?: BigDecimal.ZERO
}
