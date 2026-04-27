package com.kkaloai.app.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.kkaloai.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kkaloai.app.data.local.MealEntry
import com.kkaloai.app.util.MassFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onSnapClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBarcodeClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onWeightClick: () -> Unit = {},
    onWeeklyReportClick: () -> Unit = {},
    onPlannerClick: () -> Unit = {},
    onStatsClick: () -> Unit = {},
    onAchievementsClick: () -> Unit = {},
    onMealPlanClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val ctx = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val unlockedTpl = stringResource(R.string.ach_unlocked_toast)

    LaunchedEffect(Unit) {
        viewModel.achievementUnlocks.collect { def ->
            val title = ctx.getString(def.titleRes)
            snackbarHostState.showSnackbar("${def.emoji} ${unlockedTpl.format(title)}")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("KkaloAI", fontWeight = FontWeight.Bold)
                        if (uiState.currentStreak > 0) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Surface(
                                color = Color(0xFFFF5722),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.clickable {
                                    com.kkaloai.app.util.SharingUtils.shareStreakCard(ctx, uiState.currentStreak)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Whatshot, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${uiState.currentStreak}", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onPlannerClick) {
                        Icon(Icons.Default.ChatBubble, contentDescription = stringResource(R.string.dash_planner_cd), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onStatsClick) {
                        Icon(Icons.Default.BarChart, contentDescription = stringResource(R.string.dash_stats_cd))
                    }
                    IconButton(onClick = onAchievementsClick) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = stringResource(R.string.dash_achievements_cd))
                    }
                    IconButton(onClick = onMealPlanClick) {
                        Icon(Icons.Default.MenuBook, contentDescription = stringResource(R.string.dash_mealplan_cd))
                    }
                    IconButton(onClick = onFavoritesClick) {
                        Icon(Icons.Default.Star, contentDescription = stringResource(R.string.dash_favorites_cd))
                    }
                    IconButton(onClick = { viewModel.refreshBiofeedback() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.dash_refresh_cd))
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.dash_settings_cd))
                    }
                }
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallFloatingActionButton(
                    onClick = onBarcodeClick,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.dash_scan_barcode_cd))
                }
                FloatingActionButton(
                    onClick = onSnapClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = stringResource(R.string.dash_snap_food_cd))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                if (uiState.isGlp1Mode) {
                    Glp1CompanionCard()
                } else {
                    WeeklyReportBanner(onClick = onWeeklyReportClick)
                }
            }
            
            item { CalorieProgressCard(consumed = uiState.totalCalories, goal = uiState.calorieGoal) }

            item {
                val pct = if (uiState.calorieGoal > 0)
                    ((uiState.totalCalories * 100) / uiState.calorieGoal) else 0
                DailyMissionCard(progressPct = pct)
            }

            item {
                DailyCheckinCard(
                    existing = uiState.todayBiofeedback,
                    isGlp1Mode = uiState.isGlp1Mode,
                    onSave = { energy, mood, symptoms ->
                        viewModel.saveDailyCheckin(energy, mood, symptoms)
                    }
                )
            }

            item { BiofeedbackRow(uiState, onWeightClick = onWeightClick) }

            item { MacroSummaryRow(uiState) }

            item {
                WaterTrackerCard(
                    consumedMl = uiState.waterMl,
                    goalMl = uiState.waterGoalMl,
                    onAdd = { viewModel.addWater(it) }
                )
            }

            if (uiState.plannedMeals.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.dash_planned_today), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                items(uiState.plannedMeals) { meal ->
                    PlannedMealItem(
                        meal = meal,
                        useImperial = uiState.useImperial,
                        onEaten = { viewModel.markPlannedEaten(meal.id) }
                    )
                }
            }

            item { Text(stringResource(R.string.todays_meals), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }

            if (uiState.meals.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_meals_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(uiState.meals) { meal ->
                    MealHistoryItem(
                        meal = meal,
                        useImperial = uiState.useImperial,
                        onToggleFavorite = { viewModel.toggleFavorite(meal.id, !meal.isFavorite) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BiofeedbackRow(state: DashboardState, onWeightClick: () -> Unit = {}) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 3
    ) {
        BioCard(stringResource(R.string.steps), "${state.steps}", Icons.AutoMirrored.Filled.DirectionsWalk, Modifier.weight(1f))
        BioCard(stringResource(R.string.burned), "${state.burnedCalories} kcal", Icons.Default.LocalFireDepartment, Modifier.weight(1f))
        val weightText = state.currentWeightKg?.let { "%.1f kg".format(it) } ?: "—"
        BioCard(
            stringResource(R.string.weight),
            weightText,
            Icons.Default.MonitorWeight,
            Modifier.weight(1f).clickable(onClick = onWeightClick)
        )
    }
}

@Composable
fun BioCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun Glp1CompanionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MedicalServices, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.glp1_companion), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.protein_target), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun PlannedMealItem(
    meal: MealEntry,
    useImperial: Boolean,
    onEaten: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(meal.name, fontWeight = FontWeight.Bold)
                val p = MassFormatter.formatGrams(meal.proteins, useImperial)
                val c = MassFormatter.formatGrams(meal.carbs, useImperial)
                val f = MassFormatter.formatGrams(meal.fats, useImperial)
                Text(
                    "${meal.calories} kcal • P: $p • C: $c • F: $f",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onEaten) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.dash_ate_it))
            }
        }
    }
}

@Composable
fun CalorieProgressCard(consumed: Int, goal: Int) {
    val progress = if (goal > 0) consumed.toFloat() / goal else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress.coerceAtMost(1f))
    val remaining = (goal - consumed).coerceAtLeast(0)

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth(0.45f).aspectRatio(1f),
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$remaining", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(R.string.remaining), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text(stringResource(R.string.consumed), style = MaterialTheme.typography.labelSmall); Text("$consumed kcal", fontWeight = FontWeight.Bold) }
                Column(horizontalAlignment = Alignment.End) { Text(stringResource(R.string.daily_goal), style = MaterialTheme.typography.labelSmall); Text("$goal kcal", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MacroSummaryRow(state: DashboardState) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 3
    ) {
        MacroCard(stringResource(R.string.dash_macro_protein), state.totalProtein, state.proteinGoalG.toFloat(), Color(0xFFE74C3C), state.useImperial, Modifier.weight(1f))
        MacroCard(stringResource(R.string.dash_macro_carbs), state.totalCarbs, state.carbsGoalG.toFloat(), Color(0xFFF1C40F), state.useImperial, Modifier.weight(1f))
        MacroCard(stringResource(R.string.dash_macro_fats), state.totalFats, state.fatsGoalG.toFloat(), Color(0xFF3498DB), state.useImperial, Modifier.weight(1f))
    }
}

@Composable
fun MacroCard(name: String, current: Float, goal: Float, color: Color, useImperial: Boolean, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(name, style = MaterialTheme.typography.labelSmall)
            Text(MassFormatter.formatGrams(current, useImperial), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (current / goal).coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth(),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MealHistoryItem(
    meal: MealEntry,
    useImperial: Boolean = false,
    onToggleFavorite: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onToggleFavorite
            ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(meal.name, fontWeight = FontWeight.Bold)
                    if (meal.isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Star,
                            contentDescription = stringResource(R.string.dash_favorite_cd),
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                val p = MassFormatter.formatGrams(meal.proteins, useImperial)
                val c = MassFormatter.formatGrams(meal.carbs, useImperial)
                val f = MassFormatter.formatGrams(meal.fats, useImperial)
                Text(
                    "${meal.calories} kcal • P: $p • C: $c • F: $f",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (meal.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = stringResource(R.string.dash_toggle_fav_cd),
                    tint = if (meal.isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReportBanner(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.weekly_analysis),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    stringResource(R.string.weekly_analysis_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
