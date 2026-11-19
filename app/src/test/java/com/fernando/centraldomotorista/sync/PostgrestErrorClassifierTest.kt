package com.fernando.centraldomotorista.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostgrestErrorClassifierTest {

    @Test
    fun postgrestErrorClassifierIdentificaDuplicidadeCorretamente() {
        // (b) "23505" -> true, "23503" -> false, null -> false
        assertTrue(PostgrestErrorClassifier.isIdempotentDuplicate("23505"))
        assertFalse(PostgrestErrorClassifier.isIdempotentDuplicate("23503"))
        assertFalse(PostgrestErrorClassifier.isIdempotentDuplicate(null))
        assertFalse(PostgrestErrorClassifier.isIdempotentDuplicate("42P01"))
        assertFalse(PostgrestErrorClassifier.isIdempotentDuplicate(""))
    }
}
