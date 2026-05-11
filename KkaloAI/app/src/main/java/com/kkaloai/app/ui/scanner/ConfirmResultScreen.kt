package com.kkaloai.app.ui.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kkaloai.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kkaloai.app.data.model.FoodRecognitionResult
import com.kkaloai.app.data.model.GeminiFoodResponse
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmResultScreen(
    response: GeminiFoodResponse,
    onConfirmed: (GeminiFoodResponse) -> Unit,
    onCancel: () -> Unit,
    viewModel: ConfirmResultViewModel = hiltViewModel()
) {
    var scaleFactor by remember { mutableStateOf(1f) }

    val adjustedItems = response.items.map { item ->
        item.copy(
            calories = (item.calories * scaleFactor).roundToInt(),
            proteins = item.proteins * scaleFactor,
            carbs = item.carbs * scaleFactor,
            fats = item.fats * scaleFactor
        )
    }

    val totalCalories = adjustedItems.sumOf { it.calories }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.confirm_results), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { scaffoldPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.total_calories),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "$totalCalories kcal",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.adjust_portion) + ": ${(scaleFactor * 100).roundToInt()}%",
            style = MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = scaleFactor,
            onValueChange = { scaleFactor = it },
            valueRange = 0.5f..2.5f,
            steps = 20
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.conf_size_small), style = MaterialTheme.typography.labelSmall)
            Text(stringResource(R.string.conf_size_normal), style = MaterialTheme.typography.labelSmall)
            Text(stringResource(R.string.conf_size_xl), style = MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(adjustedItems) { item ->
                FoodResultItem(item)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.retake))
            }
            Button(
                onClick = {
                    val finalResponse = response.copy(items = adjustedItems, totalCalories = totalCalories)
                    viewModel.saveMeal(finalResponse) {
                        onConfirmed(finalResponse)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.confirm_save))
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Viral Mechanism: visual share card (PNG → TikTok/Reels/Shorts)
        val context = LocalContext.current
        Button(
            onClick = {
                val mainMeal = response.items.firstOrNull()?.name ?: "Meal"
                val totalP = adjustedItems.sumOf { it.proteins.toDouble() }.toInt()
                val totalC = adjustedItems.sumOf { it.carbs.toDouble() }.toInt()
                val totalF = adjustedItems.sumOf { it.fats.toDouble() }.toInt()
                com.kkaloai.app.util.SharingUtils.shareMealCard(
                    context = context,
                    mealName = mainMeal,
                    calories = totalCalories,
                    proteinG = totalP,
                    carbsG = totalC,
                    fatsG = totalF
                )
                viewModel.onShared()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.share_viral))
        }
    }
    } // end Scaffold content
}

@Composable
fun FoodResultItem(item: FoodRecognitionResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                item.cookingMethod?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Text(
                    text = "${item.calories} kcal • P: ${item.proteins.roundToInt()}g • C: ${item.carbs.roundToInt()}g • F: ${item.fats.roundToInt()}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
