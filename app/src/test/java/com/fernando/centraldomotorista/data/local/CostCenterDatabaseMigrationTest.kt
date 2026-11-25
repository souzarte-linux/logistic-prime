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

    @Test
    fun migration2Para3PreservaDadosExistentesECriaAs12NovasTabelas() {
        val db2 = helper.createDatabase(testDb, 2)
        db2.execSQL(
            """
            INSERT INTO outbox (id, table_name, row_id, operation, payload_json, attempts, last_error, created_at, last_attempt_at)
            VALUES ('item-v2', 'vaults', 'v-1', 'UPSERT', '{"name":"Manutencao"}', 2, 'Timeout', 987654321, 987654999)
            """.trimIndent()
        )
        db2.execSQL(
            """
            INSERT INTO sync_cursors (table_name, last_pulled_at)
            VALUES ('bank_accounts', '2026-11-20T10:00:00Z')
            """.trimIndent()
        )
        db2.close()

        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            3,
            true,
            CostCenterDatabase.MIGRATION_2_3
        )

        // Verifica preservação dos registros da v2
        val outboxCursor = migratedDb.query(
            "SELECT id, table_name, row_id, attempts, last_attempt_at FROM outbox WHERE id = 'item-v2'"
        )
        assertTrue(outboxCursor.moveToFirst())
        assertEquals("item-v2", outboxCursor.getString(0))
        assertEquals("vaults", outboxCursor.getString(1))
        assertEquals("v-1", outboxCursor.getString(2))
        assertEquals(2, outboxCursor.getInt(3))
        assertEquals(987654999L, outboxCursor.getLong(4))
        outboxCursor.close()

        val cursorCursor = migratedDb.query(
            "SELECT last_pulled_at FROM sync_cursors WHERE table_name = 'bank_accounts'"
        )
        assertTrue(cursorCursor.moveToFirst())
        assertEquals("2026-11-20T10:00:00Z", cursorCursor.getString(0))
        cursorCursor.close()

        // Verifica existência e consultabilidade de todas as 12 novas tabelas
        val newTables = listOf(
            "bank_accounts",
            "vaults",
            "vault_ledger",
            "expense_settlements",
            "fixed_costs",
            "fixed_cost_versions",
            "fixed_cost_payments",
            "card_invoice_payments",
            "cost_center_settings",
            "reserve_allocations",
            "reserve_allocation_days",
            "card_account_links"
        )
        assertEquals(12, newTables.size)
        for (table in newTables) {
            val c = migratedDb.query("SELECT COUNT(*) FROM $table")
            assertTrue("Tabela $table deve existir após migração 2->3", c.moveToFirst())
            assertEquals(0, c.getInt(0))
            c.close()
        }

        // Verifica inserção e leitura em uma tabela nova após migração
        migratedDb.execSQL(
            """
            INSERT INTO bank_accounts (id, user_id, name, active, sort_order, created_at, updated_at, sync_state)
            VALUES ('acc-m3', 'usr-1', 'Conta Migrada', 1, 0, '2026-11-20T10:00:00Z', '2026-11-20T10:00:00Z', 'PENDENTE')
            """.trimIndent()
        )
        val accCursor = migratedDb.query("SELECT name FROM bank_accounts WHERE id = 'acc-m3'")
        assertTrue(accCursor.moveToFirst())
        assertEquals("Conta Migrada", accCursor.getString(0))
        accCursor.close()

        migratedDb.close()
    }
}
