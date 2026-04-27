package com.kkaloai.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kkaloai.app.R
import com.kkaloai.app.data.local.BiofeedbackEntry

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DailyCheckinCard(
    existing: BiofeedbackEntry?,
    isGlp1Mode: Boolean,
    onSave: (energy: Int, mood: String, symptoms: Set<String>) -> Unit
) {
    var energy by remember(existing) { mutableStateOf(existing?.energyLevel ?: 3) }
    var mood by remember(existing) { mutableStateOf(existing?.mood ?: "ok") }
    var symptoms by remember(existing) {
        mutableStateOf(
            if (existing?.symptoms.isNullOrBlank()) emptySet()
            else existing!!.symptoms.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        )
    }

    val symptomOptions = listOf(
        "nausea" to stringResource(R.string.checkin_sym_nausea),
        "fatigue" to stringResource(R.string.checkin_sym_fatigue),
        "bloating" to stringResource(R.string.checkin_sym_bloating),
        "early satiety" to stringResource(R.string.checkin_sym_satiety)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(if (existing == null) R.string.checkin_title_new else R.string.checkin_title_update),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.checkin_energy), style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1, 2, 3, 4, 5).forEach { level ->
                    FilterChip(
                        selected = energy == level,
                        onClick = { energy = level },
                        label = { Text("$level") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.checkin_mood), style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MoodChip(stringResource(R.string.checkin_mood_good), mood == "good") { mood = "good" }
                MoodChip(stringResource(R.string.checkin_mood_ok), mood == "ok") { mood = "ok" }
                MoodChip(stringResource(R.string.checkin_mood_bad), mood == "bad") { mood = "bad" }
            }

            if (isGlp1Mode) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.checkin_glp1_hint), style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    symptomOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = key in symptoms,
                            onClick = {
                                symptoms = if (key in symptoms) symptoms - key else symptoms + key
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onSave(energy, mood, symptoms) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(if (existing == null) R.string.checkin_save_new else R.string.checkin_save_update))
            }
        }
    }
}

@Composable
private fun MoodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}
