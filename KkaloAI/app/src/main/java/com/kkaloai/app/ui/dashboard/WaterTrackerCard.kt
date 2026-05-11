package com.kkaloai.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kkaloai.app.R

@Composable
fun WaterTrackerCard(
    consumedMl: Int,
    goalMl: Int,
    onAdd: (Int) -> Unit
) {
    val progress = if (goalMl > 0) (consumedMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = Color(0xFF29B6F6)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.water_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.water_amount, consumedMl, goalMl),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = Color(0xFF29B6F6),
                trackColor = Color(0xFF29B6F6).copy(alpha = 0.15f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WaterQuickAddButton(stringResource(R.string.water_add_250), Modifier.weight(1f)) { onAdd(250) }
                WaterQuickAddButton(stringResource(R.string.water_add_500), Modifier.weight(1f)) { onAdd(500) }
                WaterQuickAddButton(stringResource(R.string.water_add_750), Modifier.weight(1f)) { onAdd(750) }
            }
        }
    }
}

@Composable
private fun WaterQuickAddButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(label)
    }
}
