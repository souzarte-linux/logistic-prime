package com.fernando.centraldomotorista.ui.screens.costcenter

import androidx.annotation.StringRes
import com.fernando.centraldomotorista.R
import com.fernando.centraldomotorista.data.model.AccountType
import com.fernando.centraldomotorista.data.model.LedgerEntryType
import com.fernando.centraldomotorista.data.model.VaultPurpose

@StringRes
fun AccountType.labelRes(): Int = when (this) {
    AccountType.CORRENTE -> R.string.account_type_corrente
    AccountType.POUPANCA -> R.string.account_type_poupanca
    AccountType.PAGAMENTO -> R.string.account_type_pagamento
    AccountType.OUTRA -> R.string.account_type_outra
}

@StringRes
fun VaultPurpose.labelRes(): Int = when (this) {
    VaultPurpose.MANUTENCAO -> R.string.vault_purpose_manutencao
    VaultPurpose.IMPOSTOS -> R.string.vault_purpose_impostos
    VaultPurpose.GARAGEM -> R.string.vault_purpose_garagem
    VaultPurpose.SEGURO -> R.string.vault_purpose_seguro
    VaultPurpose.CUSTOS_FIXOS -> R.string.vault_purpose_custos_fixos
    VaultPurpose.VIAGEM -> R.string.vault_purpose_viagem
    VaultPurpose.EMERGENCIA -> R.string.vault_purpose_emergencia
    VaultPurpose.OUTRO -> R.string.vault_purpose_outro
}

@StringRes
fun LedgerEntryType.labelRes(): Int = when (this) {
    LedgerEntryType.APORTE -> R.string.ledger_entry_aporte
    LedgerEntryType.RETIRADA -> R.string.ledger_entry_retirada
    LedgerEntryType.TRANSFERENCIA -> R.string.ledger_entry_transferencia
    LedgerEntryType.AJUSTE -> R.string.ledger_entry_ajuste
    LedgerEntryType.PAGAMENTO_DESPESA -> R.string.ledger_entry_pagamento_despesa
    LedgerEntryType.PAGAMENTO_CUSTO_FIXO -> R.string.ledger_entry_pagamento_custo_fixo
    LedgerEntryType.PAGAMENTO_FATURA -> R.string.ledger_entry_pagamento_fatura
    LedgerEntryType.ESTORNO -> R.string.ledger_entry_estorno
}
