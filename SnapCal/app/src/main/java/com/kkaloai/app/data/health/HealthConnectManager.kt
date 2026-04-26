package com.kkaloai.app.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Volume
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    enum class Availability { AVAILABLE, PROVIDER_NOT_INSTALLED, PROVIDER_UPDATE_NEEDED, UNAVAILABLE }

    val availability: Availability
        get() = when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> Availability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.PROVIDER_UPDATE_NEEDED
            HealthConnectClient.SDK_UNAVAILABLE -> Availability.UNAVAILABLE
            else -> Availability.PROVIDER_NOT_INSTALLED
        }

    val isAvailable: Boolean
        get() = availability == Availability.AVAILABLE

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions = setOf(
        HealthPermission.getWritePermission(NutritionRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getWritePermission(HydrationRecord::class)
    )

    suspend fun hasAllPermissions(): Boolean {
        if (!isAvailable) return false
        return try {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            granted.containsAll(permissions)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun writeMeal(
        name: String,
        calories: Int,
        proteins: Float,
        carbs: Float,
        fats: Float,
        startTime: Instant,
        endTime: Instant = startTime.plusSeconds(300)
    ) {
        val record = NutritionRecord(
            startTime = startTime,
            startZoneOffset = ZonedDateTime.now().offset,
            endTime = endTime,
            endZoneOffset = ZonedDateTime.now().offset,
            energy = Energy.kilocalories(calories.toDouble()),
            protein = Mass.grams(proteins.toDouble()),
            totalCarbohydrate = Mass.grams(carbs.toDouble()),
            totalFat = Mass.grams(fats.toDouble()),
            name = name
        )
        healthConnectClient.insertRecords(listOf(record))
    }

    suspend fun writeWater(amountMl: Int, timestamp: Instant = Instant.now()) {
        val record = HydrationRecord(
            startTime = timestamp,
            startZoneOffset = ZonedDateTime.now().offset,
            endTime = timestamp.plusSeconds(60),
            endZoneOffset = ZonedDateTime.now().offset,
            volume = Volume.milliliters(amountMl.toDouble())
        )
        runCatching { healthConnectClient.insertRecords(listOf(record)) }
    }

    suspend fun writeWeight(weightKg: Double, timestamp: Instant = Instant.now()) {
        val record = WeightRecord(
            time = timestamp,
            zoneOffset = ZonedDateTime.now().offset,
            weight = Mass.kilograms(weightKg)
        )
        healthConnectClient.insertRecords(listOf(record))
    }

    suspend fun readDailyNutrition(start: Instant, end: Instant): List<NutritionRecord> {
        val request = androidx.health.connect.client.request.ReadRecordsRequest(
            recordType = NutritionRecord::class,
            timeRangeFilter = androidx.health.connect.client.time.TimeRangeFilter.between(start, end)
        )
        return healthConnectClient.readRecords(request).records
    }

    suspend fun readSteps(start: Instant, end: Instant): Long {
        val request = androidx.health.connect.client.request.ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = androidx.health.connect.client.time.TimeRangeFilter.between(start, end)
        )
        return healthConnectClient.readRecords(request).records.sumOf { it.count }
    }

    suspend fun readCaloriesBurned(start: Instant, end: Instant): Double {
        val request = androidx.health.connect.client.request.ReadRecordsRequest(
            recordType = TotalCaloriesBurnedRecord::class,
            timeRangeFilter = androidx.health.connect.client.time.TimeRangeFilter.between(start, end)
        )
        return healthConnectClient.readRecords(request).records.sumOf { it.energy.inKilocalories }
    }

    suspend fun readLatestWeight(): Double? {
        val request = androidx.health.connect.client.request.ReadRecordsRequest(
            recordType = WeightRecord::class,
            timeRangeFilter = androidx.health.connect.client.time.TimeRangeFilter.after(Instant.now().minus(Duration.ofDays(30))),
            ascendingOrder = false,
            pageSize = 1
        )
        return healthConnectClient.readRecords(request).records.firstOrNull()?.weight?.inKilograms
    }

    suspend fun readWeightSeries(start: Instant, end: Instant): List<com.kkaloai.app.ui.stats.WeightPoint> {
        val request = androidx.health.connect.client.request.ReadRecordsRequest(
            recordType = WeightRecord::class,
            timeRangeFilter = androidx.health.connect.client.time.TimeRangeFilter.between(start, end),
            ascendingOrder = true
        )
        val zone = java.time.ZoneId.systemDefault()
        return healthConnectClient.readRecords(request).records.map { rec ->
            com.kkaloai.app.ui.stats.WeightPoint(
                day = rec.time.atZone(zone).toLocalDate(),
                kg = rec.weight.inKilograms.toFloat()
            )
        }
    }
}
