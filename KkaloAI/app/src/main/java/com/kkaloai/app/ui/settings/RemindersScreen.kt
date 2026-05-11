package com.kkaloai.app.ui.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkaloai.app.R
import com.kkaloai.app.data.local.UserPreferences
import com.kkaloai.app.notif.ReminderScheduler
import com.kkaloai.app.notif.ReminderType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.content.Context
import javax.inject.Inject

data class RemindersUiState(
    val breakfast: Boolean = true,
    val lunch: Boolean = true,
    val dinner: Boolean = true,
    val water: Boolean = false,
    val streak: Boolean = true,
    val checkin: Boolean = false,
    val breakfastHour: Int = 9,
    val lunchHour: Int = 13,
    val dinnerHour: Int = 19
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val state: StateFlow<RemindersUiState> = kotlinx.coroutines.flow.combine(
        userPreferences.reminderBreakfast,
        userPreferences.reminderLunch,
        userPreferences.reminderDinner,
        userPreferences.reminderWater,
        userPreferences.reminderStreak,
        userPreferences.reminderCheckin,
        userPreferences.breakfastHour,
        userPreferences.lunchHour,
        userPreferences.dinnerHour
    ) { values ->
        RemindersUiState(
            breakfast = values[0] as Boolean,
            lunch = values[1] as Boolean,
            dinner = values[2] as Boolean,
            water = values[3] as Boolean,
            streak = values[4] as Boolean,
            checkin = values[5] as Boolean,
            breakfastHour = values[6] as Int,
            lunchHour = values[7] as Int,
            dinnerHour = values[8] as Int
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RemindersUiState())

    fun toggle(type: String, enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setReminder(type, enabled)
            val reminderType = when (type) {
                "breakfast" -> ReminderType.BREAKFAST
                "lunch" -> ReminderType.LUNCH
                "dinner" -> ReminderType.DINNER
                "water" -> ReminderType.WATER
                "streak" -> ReminderType.STREAK
                "checkin" -> ReminderType.CHECKIN
                else -> return@launch
            }
            if (enabled) {
                when (reminderType) {
                    ReminderType.WATER -> ReminderScheduler.scheduleRepeatingHourly(context, reminderType)
                    ReminderType.BREAKFAST -> ReminderScheduler.scheduleDaily(context, reminderType, state.value.breakfastHour)
                    ReminderType.LUNCH -> ReminderScheduler.scheduleDaily(context, reminderType, state.value.lunchHour)
                    ReminderType.DINNER -> ReminderScheduler.scheduleDaily(context, reminderType, state.value.dinnerHour)
                    else -> ReminderScheduler.scheduleDaily(context, reminderType, reminderType.defaultHour, reminderType.defaultMinute)
                }
            } else {
                ReminderScheduler.cancel(context, reminderType)
            }
        }
    }

    fun setMealHour(type: String, hour: Int) {
        viewModelScope.launch {
            userPreferences.setMealHour(type, hour)
            val reminderType = when (type) {
                "breakfast" -> ReminderType.BREAKFAST
                "lunch" -> ReminderType.LUNCH
                "dinner" -> ReminderType.DINNER
                else -> return@launch
            }
            // Reschedule if enabled
            val enabled = when (type) {
                "breakfast" -> state.value.breakfast
                "lunch" -> state.value.lunch
                "dinner" -> state.value.dinner
                else -> false
            }
            if (enabled) ReminderScheduler.scheduleDaily(context, reminderType, hour)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    viewModel: RemindersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rem_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.rem_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                ReminderRow(
                    title = stringResource(R.string.rem_breakfast),
                    subtitle = stringResource(R.string.rem_daily_at, state.breakfastHour),
                    checked = state.breakfast,
                    onChecked = { viewModel.toggle("breakfast", it) },
                    onTimeClick = {
                        pickHour(context, state.breakfastHour) { h -> viewModel.setMealHour("breakfast", h) }
                    }
                )
            }
            item {
                ReminderRow(
                    title = stringResource(R.string.rem_lunch),
                    subtitle = stringResource(R.string.rem_daily_at, state.lunchHour),
                    checked = state.lunch,
                    onChecked = { viewModel.toggle("lunch", it) },
                    onTimeClick = {
                        pickHour(context, state.lunchHour) { h -> viewModel.setMealHour("lunch", h) }
                    }
                )
            }
            item {
                ReminderRow(
                    title = stringResource(R.string.rem_dinner),
                    subtitle = stringResource(R.string.rem_daily_at, state.dinnerHour),
                    checked = state.dinner,
                    onChecked = { viewModel.toggle("dinner", it) },
                    onTimeClick = {
                        pickHour(context, state.dinnerHour) { h -> viewModel.setMealHour("dinner", h) }
                    }
                )
            }
            item {
                ReminderRow(
                    title = stringResource(R.string.rem_water_label),
                    subtitle = stringResource(R.string.rem_water_sub),
                    checked = state.water,
                    onChecked = { viewModel.toggle("water", it) }
                )
            }
            item {
                ReminderRow(
                    title = stringResource(R.string.rem_streak),
                    subtitle = stringResource(R.string.rem_streak_sub),
                    checked = state.streak,
                    onChecked = { viewModel.toggle("streak", it) }
                )
            }
            item {
                ReminderRow(
                    title = stringResource(R.string.rem_checkin),
                    subtitle = stringResource(R.string.rem_checkin_sub),
                    checked = state.checkin,
                    onChecked = { viewModel.toggle("checkin", it) }
                )
            }
        }
    }
}

@Composable
private fun ReminderRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    onTimeClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(if (onTimeClick != null) Modifier.clickable(onClick = onTimeClick) else Modifier)
            ) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}

private fun pickHour(context: Context, current: Int, onPick: (Int) -> Unit) {
    TimePickerDialog(
        context,
        { _, h, _ -> onPick(h) },
        current, 0, true
    ).show()
}
