package com.fernando.centraldomotorista.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
}
