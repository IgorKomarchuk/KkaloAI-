package com.kkaloai.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kkaloai.app.R
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var tabIdx by remember { mutableStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.stats_tab_kcal),
        stringResource(R.string.stats_tab_weight),
        stringResource(R.string.stats_tab_macros)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            RangePicker(state.rangeDays) { viewModel.loadRange(it) }
            Spacer(Modifier.height(12.dp))

            TabRow(selectedTabIndex = tabIdx) {
                tabs.forEachIndexed { i, t ->
                    Tab(selected = tabIdx == i, onClick = { tabIdx = i }, text = { Text(t) })
                }
            }

            Spacer(Modifier.height(16.dp))

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.hasNoData -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.stats_no_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> when (tabIdx) {
                    0 -> KcalChart(state)
                    1 -> WeightChart(state)
                    2 -> MacroDonut(state)
                }
            }
        }
    }
}

@Composable
private fun RangePicker(current: Int, onPick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(7, 30, 90).forEach { d ->
            FilterChip(
                selected = current == d,
                onClick = { onPick(d) },
                label = { Text("$d ${stringResource(R.string.stats_days)}") }
            )
        }
    }
}

@Composable
private fun KcalChart(state: StatsState) {
    Column {
        Text(
            text = stringResource(R.string.stats_avg_kcal, state.avgKcal),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Box(modifier = Modifier.padding(12.dp)) {
                val kcalChartDesc = stringResource(R.string.stats_kcal_chart_desc, state.avgKcal, state.rangeDays)
                Canvas(modifier = Modifier.fillMaxWidth().height(220.dp).semantics { contentDescription = kcalChartDesc }) {
                    if (state.dailyKcal.isEmpty()) return@Canvas
                    val maxKcal = max(state.dailyKcal.maxOf { it.kcal }, state.calorieGoal).toFloat().coerceAtLeast(1f)
                    val barCount = state.dailyKcal.size
                    val gap = 4f
                    val barW = (size.width - gap * (barCount - 1)) / barCount

                    state.dailyKcal.forEachIndexed { i, d ->
                        val h = (d.kcal / maxKcal) * size.height
                        val x = i * (barW + gap)
                        val y = size.height - h
                        val isOver = d.kcal > state.calorieGoal
                        drawRect(
                            color = if (isOver) Color(0xFFE74C3C) else Color(0xFF2ECC71),
                            topLeft = Offset(x, y),
                            size = Size(barW, h)
                        )
                    }

                    val goalY = size.height - (state.calorieGoal / maxKcal) * size.height
                    drawLine(
                        color = Color(0xFFD4AF37),
                        start = Offset(0f, goalY),
                        end = Offset(size.width, goalY),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.stats_goal_line, state.calorieGoal),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeightChart(state: StatsState) {
    Column {
        if (state.weightHistory.size < 2) {
            Text(
                text = stringResource(R.string.stats_weight_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }
        val first = state.weightHistory.first().kg
        val last = state.weightHistory.last().kg
        val delta = last - first
        val deltaText = if (delta >= 0) "+%.1f kg".format(delta) else "%.1f kg".format(delta)
        Text(
            text = stringResource(R.string.stats_weight_change, deltaText),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (delta < 0) Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Box(modifier = Modifier.padding(12.dp)) {
                val weightChartDesc = stringResource(R.string.stats_weight_chart_desc, deltaText)
                Canvas(modifier = Modifier.fillMaxWidth().height(220.dp).semantics { contentDescription = weightChartDesc }) {
                    val pts = state.weightHistory
                    val minW = pts.minOf { it.kg }
                    val maxW = pts.maxOf { it.kg }
                    val rng = (maxW - minW).coerceAtLeast(0.5f)
                    val path = Path()

                    pts.forEachIndexed { i, p ->
                        val x = (i / (pts.size - 1).toFloat()) * size.width
                        val y = size.height - ((p.kg - minW) / rng) * size.height
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color = Color(0xFF3498DB), style = Stroke(width = 5f, cap = StrokeCap.Round))

                    pts.forEachIndexed { i, p ->
                        val x = (i / (pts.size - 1).toFloat()) * size.width
                        val y = size.height - ((p.kg - minW) / rng) * size.height
                        drawCircle(Color(0xFF3498DB), radius = 6f, center = Offset(x, y))
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroDonut(state: StatsState) {
    val m = state.macroSplit
    val total = (m.proteinG + m.carbsG + m.fatsG).coerceAtLeast(1)
    val pPct = m.proteinG.toFloat() / total
    val cPct = m.carbsG.toFloat() / total
    val fPct = m.fatsG.toFloat() / total

    Column {
        Text(
            text = stringResource(R.string.stats_macros_total),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        val donutDesc = stringResource(R.string.stats_donut_desc, m.proteinG, m.carbsG, m.fatsG)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(modifier = Modifier.size(200.dp).semantics { contentDescription = donutDesc }) {
                    val sw = 36f
                    val arcGap = 2f
                    val arcSize = Size(size.width - sw, size.height - sw)
                    val topLeft = Offset(sw / 2, sw / 2)
                    var startAngle = -90f

                    listOf(
                        pPct to Color(0xFFE74C3C),
                        cPct to Color(0xFFF1C40F),
                        fPct to Color(0xFF3498DB)
                    ).forEach { (pct, color) ->
                        val fullSweep = pct * 360f
                        val drawSweep = (fullSweep - arcGap).coerceAtLeast(0f)
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = drawSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = sw, cap = StrokeCap.Butt)
                        )
                        startAngle += fullSweep
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot(Color(0xFFE74C3C), "P ${m.proteinG}g")
                    LegendDot(Color(0xFFF1C40F), "C ${m.carbsG}g")
                    LegendDot(Color(0xFF3498DB), "F ${m.fatsG}g")
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(50))
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
