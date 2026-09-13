package com.fernando.centraldomotorista.ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.VisualTransformation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InputMasksTest {

    private fun validateVisualTransformation(
        transformation: VisualTransformation,
        input: String
    ) {
        val original = AnnotatedString(input)
        val transformed = transformation.filter(original)
        val formatted = transformed.text.text
        val offsetMapping = transformed.offsetMapping

        // 1. Check originalToTransformed for all valid offsets [0..input.length]
        var prevTrans = -1
        for (offset in 0..input.length) {
            val transOffset = offsetMapping.originalToTransformed(offset)
            assertTrue(
                "originalToTransformed($offset) = $transOffset must be in [0, ${formatted.length}] for input '$input'",
                transOffset in 0..formatted.length
            )
            assertTrue(
                "originalToTransformed must be monotonic: $transOffset >= $prevTrans for input '$input'",
                transOffset >= prevTrans
            )
            prevTrans = transOffset
        }

        // 2. Check transformedToOriginal for all valid offsets [0..formatted.length]
        var prevOrig = -1
        for (offset in 0..formatted.length) {
            val origOffset = offsetMapping.transformedToOriginal(offset)
            assertTrue(
                "transformedToOriginal($offset) = $origOffset must be in [0, ${input.length}] for input '$input' (formatted: '$formatted')",
                origOffset in 0..input.length
            )
            assertTrue(
                "transformedToOriginal must be monotonic: $origOffset >= $prevOrig for input '$input'",
                origOffset >= prevOrig
            )
            prevOrig = origOffset
        }
    }

    @Test
    fun testPhoneVisualTransformation_noCrashOnAnyLength() {
        val transformation = PhoneVisualTransformation()
        val digits = "11987654321999"

        // Test every substring from 0 to full length
        for (len in 0..digits.length) {
            val input = digits.take(len)
            validateVisualTransformation(transformation, input)
        }
    }

    @Test
    fun testPhoneVisualTransformation_specificCases() {
        val transformation = PhoneVisualTransformation()

        // 2 digits (which was the exact crash case!)
        val result2 = transformation.filter(AnnotatedString("11"))
        assertEquals("(11", result2.text.text)
        validateVisualTransformation(transformation, "11")

        // 10 digits (landline)
        val result10 = transformation.filter(AnnotatedString("1187654321"))
        assertEquals("(11) 8765-4321", result10.text.text)
        validateVisualTransformation(transformation, "1187654321")

        // 11 digits (mobile)
        val result11 = transformation.filter(AnnotatedString("11987654321"))
        assertEquals("(11) 98765-4321", result11.text.text)
        validateVisualTransformation(transformation, "11987654321")
    }

    @Test
    fun testCpfVisualTransformation_noCrashOnAnyLength() {
        val transformation = CpfVisualTransformation()
        val digits = "1234567890123"

        for (len in 0..digits.length) {
            val input = digits.take(len)
            validateVisualTransformation(transformation, input)
        }
    }

    @Test
    fun testCepVisualTransformation_noCrashOnAnyLength() {
        val transformation = CepVisualTransformation()
        val digits = "0100100099"

        for (len in 0..digits.length) {
            val input = digits.take(len)
            validateVisualTransformation(transformation, input)
        }
    }

    @Test
    fun testCnpjVisualTransformation_noCrashOnAnyLength() {
        val transformation = CnpjVisualTransformation()
        val digits = "1234567800019999"

        for (len in 0..digits.length) {
            val input = digits.take(len)
            validateVisualTransformation(transformation, input)
        }
    }
}
