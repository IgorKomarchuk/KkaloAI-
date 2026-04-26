package com.kkaloai.app.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import com.kkaloai.app.R
import com.kkaloai.app.data.local.ActivityLevel
import com.kkaloai.app.data.local.HealthGoal
import com.kkaloai.app.data.local.Sex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val PAGE_COUNT = 10

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val lastPage = PAGE_COUNT - 1

    var showLangSheet by remember { mutableStateOf(false) }
    if (showLangSheet) {
        LanguagePickerSheet(onDismiss = { showLangSheet = false })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF121212), Color(0xFF1E1E1E))
                )
            )
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> GoalPage(viewModel)
                2 -> SexAgePage(viewModel)
                3 -> HeightPage(viewModel)
                4 -> WeightPage(viewModel)
                5 -> ActivityPage(viewModel)
                6 -> RatePage(viewModel)
                7 -> SpecialHealthPage(viewModel)
                8 -> RevealPage(viewModel)
                9 -> HealthConnectPage(viewModel)
            }
        }

        // Top progress bar
        LinearProgressIndicator(
            progress = { (pagerState.currentPage + 1) / PAGE_COUNT.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 36.dp, start = 24.dp, end = 56.dp)
                .align(Alignment.TopStart),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.White.copy(alpha = 0.12f)
        )

        if (pagerState.currentPage == 0) {
            IconButton(
                onClick = { showLangSheet = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 32.dp, end = 12.dp)
            ) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = stringResource(R.string.onb_language_cd),
                    tint = Color.White
                )
            }
        }

        // Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage > 0) {
                OutlinedButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.onb_back),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            Button(
                onClick = {
                    if (pagerState.currentPage < lastPage) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        viewModel.persistChoices()
                        onFinish()
                    }
                },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(
                        if (pagerState.currentPage < lastPage) R.string.onb_continue
                        else R.string.onb_get_started
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerSheet(onDismiss: () -> Unit) {
    val current = AppCompatDelegate.getApplicationLocales().get(0)?.language ?: "en"
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.onb_language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LanguageRow(stringResource(R.string.onb_lang_english), current == "en") {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")); onDismiss()
            }
            LanguageRow(stringResource(R.string.onb_lang_ukrainian), current == "uk") {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("uk")); onDismiss()
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LanguageRow(label: String, selected: Boolean, onPick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPick).padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        if (selected) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PageFrame(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(32.dp))
        content()
    }
}

@Composable
fun WelcomePage() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.onb_welcome_title),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onb_welcome_subtitle),
            color = Color.Gray,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GoalPage(vm: OnboardingViewModel) {
    val selected by vm.selectedGoal.collectAsState()
    PageFrame(
        title = stringResource(R.string.onb_goal_title),
        subtitle = stringResource(R.string.onb_goal_subtitle)
    ) {
        GoalOption(
            stringResource(R.string.onb_goal_lose),
            stringResource(R.string.onb_goal_lose_sub),
            selected == HealthGoal.LOSS
        ) { vm.selectGoal(HealthGoal.LOSS) }
        Spacer(Modifier.height(12.dp))
        GoalOption(
            stringResource(R.string.onb_goal_maintain),
            stringResource(R.string.onb_goal_maintain_sub),
            selected == HealthGoal.MAINTAIN
        ) { vm.selectGoal(HealthGoal.MAINTAIN) }
        Spacer(Modifier.height(12.dp))
        GoalOption(
            stringResource(R.string.onb_goal_bulk),
            stringResource(R.string.onb_goal_bulk_sub),
            selected == HealthGoal.BULK
        ) { vm.selectGoal(HealthGoal.BULK) }
    }
}

@Composable
private fun GoalOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.04f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(2.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun SexAgePage(vm: OnboardingViewModel) {
    val sex by vm.sex.collectAsState()
    val age by vm.age.collectAsState()
    PageFrame(
        title = stringResource(R.string.onb_sexage_title),
        subtitle = stringResource(R.string.onb_sexage_subtitle)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SexOption(stringResource(R.string.onb_sex_female), sex == Sex.FEMALE, Modifier.weight(1f)) { vm.setSex(Sex.FEMALE) }
            SexOption(stringResource(R.string.onb_sex_male), sex == Sex.MALE, Modifier.weight(1f)) { vm.setSex(Sex.MALE) }
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.onb_age_label, age),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        Slider(
            value = age.toFloat(),
            onValueChange = { vm.setAge(it.toInt()) },
            valueRange = 13f..80f
        )
    }
}

@Composable
private fun SexOption(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.04f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(2.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = Color.White, fontWeight = FontWeight.Bold) }
}

@Composable
fun HeightPage(vm: OnboardingViewModel) {
    val h by vm.heightCm.collectAsState()
    PageFrame(
        title = stringResource(R.string.onb_height_title),
        subtitle = stringResource(R.string.onb_height_value, h)
    ) {
        Slider(value = h.toFloat(), onValueChange = { vm.setHeight(it.toInt()) }, valueRange = 140f..210f)
    }
}

@Composable
fun WeightPage(vm: OnboardingViewModel) {
    val w by vm.weightKg.collectAsState()
    PageFrame(
        title = stringResource(R.string.onb_weight_title),
        subtitle = stringResource(R.string.onb_weight_value, w.roundToInt())
    ) {
        Slider(value = w, onValueChange = { vm.setWeight(it) }, valueRange = 40f..200f)
    }
}

@Composable
fun ActivityPage(vm: OnboardingViewModel) {
    val a by vm.activity.collectAsState()
    val items = listOf(
        Triple(stringResource(R.string.onb_activity_sedentary), stringResource(R.string.onb_activity_sedentary_sub), ActivityLevel.SEDENTARY),
        Triple(stringResource(R.string.onb_activity_light), stringResource(R.string.onb_activity_light_sub), ActivityLevel.LIGHT),
        Triple(stringResource(R.string.onb_activity_moderate), stringResource(R.string.onb_activity_moderate_sub), ActivityLevel.MODERATE),
        Triple(stringResource(R.string.onb_activity_active), stringResource(R.string.onb_activity_active_sub), ActivityLevel.ACTIVE),
        Triple(stringResource(R.string.onb_activity_very_active), stringResource(R.string.onb_activity_very_active_sub), ActivityLevel.VERY_ACTIVE)
    )
    PageFrame(
        title = stringResource(R.string.onb_activity_title),
        subtitle = stringResource(R.string.onb_activity_subtitle)
    ) {
        items.forEach { (title, sub, level) ->
            GoalOption(title, sub, a == level) { vm.setActivity(level) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun RatePage(vm: OnboardingViewModel) {
    val r by vm.weeklyRate.collectAsState()
    val goal by vm.selectedGoal.collectAsState()
    val subtitle = when (goal) {
        HealthGoal.LOSS -> stringResource(R.string.onb_pace_lose, r)
        HealthGoal.BULK -> stringResource(R.string.onb_pace_gain, r)
        HealthGoal.MAINTAIN -> stringResource(R.string.onb_pace_maintain_skip)
    }
    PageFrame(
        title = stringResource(R.string.onb_pace_title),
        subtitle = subtitle
    ) {
        if (goal != HealthGoal.MAINTAIN) {
            Slider(
                value = r,
                onValueChange = { vm.setWeeklyRate(it) },
                valueRange = 0.25f..1f,
                steps = 2
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.onb_pace_slow), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.onb_pace_balanced), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.onb_pace_aggressive), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚖️", fontSize = 56.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.onb_pace_maintain_explainer),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun SpecialHealthPage(vm: OnboardingViewModel) {
    val glp1 by vm.glp1.collectAsState()
    val hormonal by vm.hormonal.collectAsState()
    PageFrame(
        title = stringResource(R.string.onb_special_title),
        subtitle = stringResource(R.string.onb_special_subtitle)
    ) {
        HealthToggle(
            stringResource(R.string.onb_glp1_title),
            stringResource(R.string.onb_glp1_sub),
            glp1
        ) { vm.toggleGlp1(it) }
        Spacer(Modifier.height(12.dp))
        HealthToggle(
            stringResource(R.string.onb_hormonal_title),
            stringResource(R.string.onb_hormonal_sub),
            hormonal
        ) { vm.toggleHormonal(it) }
    }
}

@Composable
private fun HealthToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.labelMedium)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun RevealPage(vm: OnboardingViewModel) {
    // Recompute on every input change — collect all upstream StateFlows so Compose recomposes.
    val sex by vm.sex.collectAsState()
    val age by vm.age.collectAsState()
    val heightCm by vm.heightCm.collectAsState()
    val weightKg by vm.weightKg.collectAsState()
    val activity by vm.activity.collectAsState()
    val goal by vm.selectedGoal.collectAsState()
    val rate by vm.weeklyRate.collectAsState()
    val calories = remember(sex, age, heightCm, weightKg, activity, goal, rate) { vm.computedCalories }
    val animated by animateFloatAsState(targetValue = calories.toFloat(), label = "calorie_reveal")

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "🎯",
            fontSize = 64.sp
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onb_reveal_plan),
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "${animated.roundToInt()}",
            fontSize = 96.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.onb_reveal_kcal_day),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.onb_reveal_explainer),
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onb_reveal_motivation),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun HealthConnectPage(viewModel: OnboardingViewModel) {
    val granted by viewModel.healthPermissionsGranted.collectAsState()
    val availability = viewModel.healthConnectAvailability
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { _ -> viewModel.checkHealthPermissions() }

    PageFrame(
        title = stringResource(R.string.onb_hc_title),
        subtitle = stringResource(R.string.onb_hc_subtitle)
    ) {
        when (availability) {
            com.kkaloai.app.data.health.HealthConnectManager.Availability.AVAILABLE -> {
                if (granted) {
                    Text(
                        text = stringResource(R.string.onb_hc_granted),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Button(
                        onClick = {
                            runCatching { permissionLauncher.launch(viewModel.healthPermissions) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = stringResource(R.string.onb_hc_enable),
                            color = Color.White
                        )
                    }
                }
            }
            com.kkaloai.app.data.health.HealthConnectManager.Availability.PROVIDER_NOT_INSTALLED -> {
                InstallHealthConnectAction(
                    prompt = stringResource(R.string.onb_hc_install_prompt),
                    cta = stringResource(R.string.onb_hc_install_cta),
                    context = context
                )
            }
            com.kkaloai.app.data.health.HealthConnectManager.Availability.PROVIDER_UPDATE_NEEDED -> {
                InstallHealthConnectAction(
                    prompt = stringResource(R.string.onb_hc_update_prompt),
                    cta = stringResource(R.string.onb_hc_update_cta),
                    context = context
                )
            }
            com.kkaloai.app.data.health.HealthConnectManager.Availability.UNAVAILABLE -> {
                Text(
                    text = stringResource(R.string.onb_hc_unavailable),
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun InstallHealthConnectAction(
    prompt: String,
    cta: String,
    context: android.content.Context
) {
    Text(
        text = prompt,
        color = Color.Gray,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setPackage("com.android.vending")
                data = android.net.Uri.parse("market://details?id=com.google.android.apps.healthdata")
            }
            runCatching {
                context.startActivity(intent)
            }.onFailure {
                val web = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                )
                runCatching { context.startActivity(web) }
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
    ) {
        Text(text = cta, color = Color.White)
    }
}
