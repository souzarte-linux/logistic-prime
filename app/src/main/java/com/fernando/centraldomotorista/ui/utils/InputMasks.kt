package com.fernando.centraldomotorista.ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
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
        val out = StringBuilder()
        for (i in raw.indices) {
            if (i == 5) out.append('-')
            out.append(raw[i])
        }
        val formatted = out.toString()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 5) return offset.coerceAtMost(formatted.length)
                return (offset + 1).coerceAtMost(formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 5) return offset.coerceAtMost(raw.length)
                return (offset - 1).coerceAtLeast(0).coerceAtMost(raw.length)
            }
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

class CnpjVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }.take(14)
        val out = StringBuilder()
        for (i in raw.indices) {
            if (i == 2 || i == 5) out.append('.')
            else if (i == 8) out.append('/')
            else if (i == 12) out.append('-')
            out.append(raw[i])
        }
        val formatted = out.toString()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, raw.length)
                val extra = when {
                    clamped <= 2 -> 0
                    clamped <= 5 -> 1
                    clamped <= 8 -> 2
                    clamped <= 12 -> 3
                    else -> 4
                }
                return (clamped + extra).coerceAtMost(formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                val reduction = when {
                    clamped <= 2 -> 0
                    clamped <= 6 -> 1
                    clamped <= 10 -> 2
                    clamped <= 15 -> 3
                    else -> 4
                }
                return (clamped - reduction).coerceIn(0, raw.length)
            }
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

class PhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }.take(11)
        val out = StringBuilder()
        val is11Digits = raw.length > 10
        for (i in raw.indices) {
            if (i == 0) out.append('(')
            if (i == 2) out.append(") ")
            if (is11Digits && i == 7) out.append('-')
            else if (!is11Digits && i == 6) out.append('-')
            out.append(raw[i])
        }
        val formatted = out.toString()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, raw.length)
                if (clamped == 0) return 0
                val extra = when {
                    clamped <= 2 -> 1
                    is11Digits && clamped <= 7 -> 3
                    !is11Digits && clamped <= 6 -> 3
                    else -> 4
                }
                return (clamped + extra).coerceAtMost(formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                val reduction = when {
                    clamped <= 1 -> clamped
                    clamped <= 4 -> 1
                    is11Digits && clamped <= 10 -> 3
                    !is11Digits && clamped <= 9 -> 3
                    else -> 4
                }
                return (clamped - reduction).coerceIn(0, raw.length)
            }
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
