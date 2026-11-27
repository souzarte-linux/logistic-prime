package com.fernando.centraldomotorista.costcenter

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fernando.centraldomotorista.R
import com.fernando.centraldomotorista.data.model.AccountType
import com.fernando.centraldomotorista.data.model.LedgerEntryType
import com.fernando.centraldomotorista.data.model.VaultPurpose
import com.fernando.centraldomotorista.ui.screens.costcenter.labelRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CostCenterLabelsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `rotulos de AccountType vem de strings e batem com o label do enum`() {
        AccountType.entries.forEach { type ->
            assertEquals(type.label, context.getString(type.labelRes()))
        }
    }

    @Test
    fun `rotulos de VaultPurpose vem de strings e batem com o label do enum`() {
        VaultPurpose.entries.forEach { purpose ->
            assertEquals(purpose.label, context.getString(purpose.labelRes()))
        }
    }

    @Test
    fun `rotulos de LedgerEntryType vem de strings e batem com o label do enum`() {
        LedgerEntryType.entries.forEach { entryType ->
            assertEquals(entryType.label, context.getString(entryType.labelRes()))
        }
    }

    @Test
    fun `plural do chip de sync`() {
        assertEquals(
            "1 alteração pendente",
            context.resources.getQuantityString(R.plurals.sync_status_pending, 1, 1)
        )
        assertEquals(
            "3 alterações pendentes",
            context.resources.getQuantityString(R.plurals.sync_status_pending, 3, 3)
        )
    }

    @Test
    fun `plural do aviso de arquivar conta`() {
        val oneText = context.resources.getQuantityString(
            R.plurals.cost_center_archive_account_active_vaults_warning,
            1,
            1
        )
        assertTrue(oneText.contains("1 caixinha ativa"))

        val twoText = context.resources.getQuantityString(
            R.plurals.cost_center_archive_account_active_vaults_warning,
            2,
            2
        )
        assertTrue(twoText.contains("2 caixinhas ativas"))
    }
}
