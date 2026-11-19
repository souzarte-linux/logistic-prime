package com.fernando.centraldomotorista.data.local

import android.app.Application
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CostCenterDatabaseMigrationTest {

    private val testDb = "migration-test-db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CostCenterDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrationTestHelperCarregaEsquema1Exportado() {
        val db = helper.createDatabase(testDb, 1)
        assertNotNull(db)

        // Valida criação e integridade das tabelas do esquema 1
        val cursorOutbox = db.query("SELECT * FROM outbox")
        assertNotNull(cursorOutbox)
        cursorOutbox.close()

        val cursorCursors = db.query("SELECT * FROM sync_cursors")
        assertNotNull(cursorCursors)
        cursorCursors.close()

        db.close()
    }

    @Test
    fun migration1Para2PreservaLinhaPreExistenteEAdicionaColunaLastAttemptAt() {
        // 7.2 (i) MigrationTestHelper.runMigrationsAndValidate(db, 2, true, MIGRATION_1_2) com uma linha pré-existente no esquema 1 preservada
        var db = helper.createDatabase(testDb, 1)
        db.execSQL(
            """
            INSERT INTO outbox (id, table_name, row_id, operation, payload_json, attempts, last_error, created_at)
            VALUES ('item-v1', 'fixed_costs', 'row-1', 'INSERT', '{"value": 100}', 1, 'Error test', 123456789)
            """.trimIndent()
        )
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(testDb, 2, true, CostCenterDatabase.MIGRATION_1_2)

        val cursor = migratedDb.query("SELECT id, table_name, row_id, operation, payload_json, attempts, last_error, created_at, last_attempt_at FROM outbox WHERE id = 'item-v1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("item-v1", cursor.getString(0))
        assertEquals("fixed_costs", cursor.getString(1))
        assertEquals("row-1", cursor.getString(2))
        assertEquals("INSERT", cursor.getString(3))
        assertEquals("{\"value\": 100}", cursor.getString(4))
        assertEquals(1, cursor.getInt(5))
        assertEquals("Error test", cursor.getString(6))
        assertEquals(123456789L, cursor.getLong(7))
        assertTrue("last_attempt_at deve ser nulo para registro pré-existente", cursor.isNull(8))
        cursor.close()

        migratedDb.close()
    }
}
