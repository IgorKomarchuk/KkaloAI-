package com.kkaloai.app.di

import android.content.Context
import androidx.room.Room
import com.kkaloai.app.data.local.AchievementDao
import com.kkaloai.app.data.local.BiofeedbackDao
import com.kkaloai.app.data.local.KkaloAiDatabase
import com.kkaloai.app.data.local.MealDao
import com.kkaloai.app.data.local.WaterDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KkaloAiDatabase {
        return Room.databaseBuilder(
            context,
            KkaloAiDatabase::class.java,
            "kkaloai_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideMealDao(database: KkaloAiDatabase): MealDao = database.mealDao()

    @Provides
    fun provideWaterDao(database: KkaloAiDatabase): WaterDao = database.waterDao()

    @Provides
    fun provideBiofeedbackDao(database: KkaloAiDatabase): BiofeedbackDao = database.biofeedbackDao()

    @Provides
    fun provideAchievementDao(database: KkaloAiDatabase): AchievementDao = database.achievementDao()
}
