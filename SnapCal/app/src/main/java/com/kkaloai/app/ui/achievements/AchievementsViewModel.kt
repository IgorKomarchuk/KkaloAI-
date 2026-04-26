package com.kkaloai.app.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkaloai.app.data.local.AchievementDao
import com.kkaloai.app.util.AchievementCatalog
import com.kkaloai.app.util.AchievementDef
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AchievementRow(
    val def: AchievementDef,
    val unlocked: Boolean,
    val unlockedAt: Long?
)

data class AchievementsState(
    val rows: List<AchievementRow> = emptyList(),
    val unlockedCount: Int = 0,
    val totalCount: Int = AchievementCatalog.ALL.size
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    achievementDao: AchievementDao
) : ViewModel() {

    val state: StateFlow<AchievementsState> = achievementDao.getAllUnlocked()
        .map { unlockedList ->
            val byCode = unlockedList.associateBy { it.code }
            val rows = AchievementCatalog.ALL.map { def ->
                val u = byCode[def.code]
                AchievementRow(def, u != null, u?.unlockedAt)
            }
            AchievementsState(rows = rows, unlockedCount = unlockedList.size)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AchievementsState()
        )
}
