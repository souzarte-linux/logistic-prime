package com.fernando.centraldomotorista.ui.screens.costcenter.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.ui.screens.costcenter.SyncChipStatus
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert

@Composable
fun SyncStatusChip(
    status: SyncChipStatus,
    onRetrySync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (label, color, icon) = when (status) {
        is SyncChipStatus.Synced -> Triple(
            "Sincronizado",
            GreenNeon,
            Icons.Default.CloudDone
        )
        is SyncChipStatus.Pending -> Triple(
            if (status.count == 1) "1 alteração pendente" else "${status.count} alterações pendentes",
            OrangeNeon,
            Icons.Default.Sync
        )
        is SyncChipStatus.Failed -> Triple(
            "Falha ao sincronizar — tentar de novo",
            RedAlert,
            Icons.Default.CloudOff
        )
    }

    val isClickable = status is SyncChipStatus.Failed

    Surface(
        modifier = modifier
            .testTag("sync_status_chip")
            .semantics { contentDescription = label }
            .then(if (isClickable) Modifier.clickable(onClick = onRetrySync) else Modifier),
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
