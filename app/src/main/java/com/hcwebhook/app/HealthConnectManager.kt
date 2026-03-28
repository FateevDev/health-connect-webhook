package com.hcwebhook.app

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass

enum class HealthDataType(val displayName: String, val recordClass: KClass<out Record>) {
    STEPS("Steps", StepsRecord::class),
    SLEEP("Sleep", SleepSessionRecord::class),
    HEART_RATE("Heart Rate", HeartRateRecord::class),
    HEART_RATE_VARIABILITY("Heart Rate Variability", HeartRateVariabilityRmssdRecord::class),
    DISTANCE("Distance", DistanceRecord::class),
    ACTIVE_CALORIES("Active Calories", ActiveCaloriesBurnedRecord::class),
    TOTAL_CALORIES("Total Calories", TotalCaloriesBurnedRecord::class),
    WEIGHT("Weight", WeightRecord::class),
    HEIGHT("Height", HeightRecord::class),
    BLOOD_PRESSURE("Blood Pressure", BloodPressureRecord::class),
    BLOOD_GLUCOSE("Blood Glucose", BloodGlucoseRecord::class),
    OXYGEN_SATURATION("Oxygen Saturation", OxygenSaturationRecord::class),
    BODY_TEMPERATURE("Body Temperature", BodyTemperatureRecord::class),
    RESPIRATORY_RATE("Respiratory Rate", RespiratoryRateRecord::class),
    RESTING_HEART_RATE("Resting Heart Rate", RestingHeartRateRecord::class),
    EXERCISE("Exercise Sessions", ExerciseSessionRecord::class),
    HYDRATION("Hydration", HydrationRecord::class),
    NUTRITION("Nutrition", NutritionRecord::class)
}

data class HealthData(
    val steps: List<StepsData>,
    val sleep: List<SleepData>,
    val heartRate: List<HeartRateData>,
    val heartRateVariability: List<HeartRateVariabilityData>,
    val distance: List<DistanceData>,
    val activeCalories: List<ActiveCaloriesData>,
    val totalCalories: List<TotalCaloriesData>,
    val weight: List<WeightData>,
    val height: List<HeightData>,
    val bloodPressure: List<BloodPressureData>,
    val bloodGlucose: List<BloodGlucoseData>,
    val oxygenSaturation: List<OxygenSaturationData>,
    val bodyTemperature: List<BodyTemperatureData>,
    val respiratoryRate: List<RespiratoryRateData>,
    val restingHeartRate: List<RestingHeartRateData>,
    val exercise: List<ExerciseData>,
    val hydration: List<HydrationData>,
    val nutrition: List<NutritionData>
)

data class StepsData(
    val count: Long,
    val startTime: Instant,
    val endTime: Instant
)

data class SleepData(
    val startTime: Instant,
    val sessionEndTime: Instant,
    val duration: Duration,
    val stages: List<SleepStage>
)

data class SleepStage(
    val stage: String,
    val startTime: Instant,
    val endTime: Instant,
    val duration: Duration
)

data class HeartRateData(
    val bpm: Long,
    val time: Instant
)

data class HeartRateVariabilityData(
    val rmssdMillis: Double,
    val time: Instant
)

data class DistanceData(
    val meters: Double,
    val startTime: Instant,
    val endTime: Instant
)

data class ActiveCaloriesData(
    val calories: Double,
    val startTime: Instant,
    val endTime: Instant
)

data class TotalCaloriesData(
    val calories: Double,
    val startTime: Instant,
    val endTime: Instant
)

data class WeightData(
    val kilograms: Double,
    val time: Instant
)

data class HeightData(
    val meters: Double,
    val time: Instant
)

data class BloodPressureData(
    val systolic: Double,
    val diastolic: Double,
    val bodyPosition: Int,
    val measurementLocation: Int,
    val time: Instant
)

data class BloodGlucoseData(
    val mmolPerLiter: Double,
    val specimenSource: Int,
    val mealType: Int,
    val relationToMeal: Int,
    val time: Instant
)

data class OxygenSaturationData(
    val percentage: Double,
    val time: Instant
)

data class BodyTemperatureData(
    val celsius: Double,
    val time: Instant
)

data class RespiratoryRateData(
    val rate: Double,
    val time: Instant
)

data class RestingHeartRateData(
    val bpm: Long,
    val time: Instant
)

data class ExerciseSegmentData(
    val type: String,
    val startTime: Instant,
    val endTime: Instant,
    val repetitions: Int
)

data class ExerciseLapData(
    val startTime: Instant,
    val endTime: Instant,
    val lengthMeters: Double?
)

data class ExerciseData(
    val type: String,
    val startTime: Instant,
    val endTime: Instant,
    val duration: Duration,
    val title: String?,
    val notes: String?,
    val segments: List<ExerciseSegmentData>,
    val laps: List<ExerciseLapData>
)

data class HydrationData(
    val liters: Double,
    val startTime: Instant,
    val endTime: Instant
)

data class NutritionData(
    val calories: Double?,
    val energyFromFatKilocalories: Double?,
    val proteinGrams: Double?,
    val carbsGrams: Double?,
    val fatGrams: Double?,
    val dietaryFiberGrams: Double?,
    val sugarGrams: Double?,
    val saturatedFatGrams: Double?,
    val transFatGrams: Double?,
    val unsaturatedFatGrams: Double?,
    val monounsaturatedFatGrams: Double?,
    val polyunsaturatedFatGrams: Double?,
    val cholesterolMilligrams: Double?,
    val sodiumMilligrams: Double?,
    val potassiumMilligrams: Double?,
    val calciumMilligrams: Double?,
    val ironMilligrams: Double?,
    val magnesiumMilligrams: Double?,
    val phosphorusMilligrams: Double?,
    val chlorideMilligrams: Double?,
    val chromiumMilligrams: Double?,
    val copperMilligrams: Double?,
    val iodineMilligrams: Double?,
    val manganeseMilligrams: Double?,
    val molybdenumMilligrams: Double?,
    val seleniumMilligrams: Double?,
    val zincMilligrams: Double?,
    val vitaminAMilligrams: Double?,
    val vitaminB6Milligrams: Double?,
    val vitaminB12Milligrams: Double?,
    val vitaminCMilligrams: Double?,
    val vitaminDMilligrams: Double?,
    val vitaminEMilligrams: Double?,
    val vitaminKMilligrams: Double?,
    val biotinMilligrams: Double?,
    val folateMilligrams: Double?,
    val folicAcidMilligrams: Double?,
    val niacinMilligrams: Double?,
    val pantothenicAcidMilligrams: Double?,
    val riboflavinMilligrams: Double?,
    val thiaminMilligrams: Double?,
    val caffeineMilligrams: Double?,
    val name: String?,
    val mealType: Int,
    val startTime: Instant,
    val endTime: Instant
)

class HealthConnectManager(private val context: Context) {

    private val healthConnectClient by lazy {
        try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            throw IllegalStateException("Health Connect is not available on this device: ${e.message}", e)
        }
    }

    suspend fun readHealthData(
        enabledTypes: Set<HealthDataType>,
        lastSyncTimestamps: Map<HealthDataType, Instant?>,
        timeRangeDays: Int? = null,
        lookbackHours: Long = DEFAULT_LOOKBACK_HOURS
    ): Result<HealthData> {
        return try {
            val endTime = Instant.now()
            val startTime = if (timeRangeDays != null) {
                endTime.minus(timeRangeDays.toLong(), ChronoUnit.DAYS)
            } else {
                endTime.minus(lookbackHours, ChronoUnit.HOURS)
            }

            val stepsData = if (HealthDataType.STEPS in enabledTypes)
                readStepsData(startTime, endTime, lastSyncTimestamps[HealthDataType.STEPS]) else emptyList()
            val sleepData = if (HealthDataType.SLEEP in enabledTypes)
                readSleepData(startTime, endTime, lastSyncTimestamps[HealthDataType.SLEEP]) else emptyList()
            val heartRateData = if (HealthDataType.HEART_RATE in enabledTypes)
                readHeartRateData(startTime, endTime, lastSyncTimestamps[HealthDataType.HEART_RATE]) else emptyList()
            val heartRateVariabilityData = if (HealthDataType.HEART_RATE_VARIABILITY in enabledTypes)
                readHeartRateVariabilityData(startTime, endTime, lastSyncTimestamps[HealthDataType.HEART_RATE_VARIABILITY]) else emptyList()
            val distanceData = if (HealthDataType.DISTANCE in enabledTypes)
                readDistanceData(startTime, endTime, lastSyncTimestamps[HealthDataType.DISTANCE]) else emptyList()
            val activeCaloriesData = if (HealthDataType.ACTIVE_CALORIES in enabledTypes)
                readActiveCaloriesData(startTime, endTime, lastSyncTimestamps[HealthDataType.ACTIVE_CALORIES]) else emptyList()
            val totalCaloriesData = if (HealthDataType.TOTAL_CALORIES in enabledTypes)
                readTotalCaloriesData(startTime, endTime, lastSyncTimestamps[HealthDataType.TOTAL_CALORIES]) else emptyList()
            val weightData = if (HealthDataType.WEIGHT in enabledTypes)
                readWeightData(startTime, endTime, lastSyncTimestamps[HealthDataType.WEIGHT]) else emptyList()
            val heightData = if (HealthDataType.HEIGHT in enabledTypes)
                readHeightData(startTime, endTime, lastSyncTimestamps[HealthDataType.HEIGHT]) else emptyList()
            val bloodPressureData = if (HealthDataType.BLOOD_PRESSURE in enabledTypes)
                readBloodPressureData(startTime, endTime, lastSyncTimestamps[HealthDataType.BLOOD_PRESSURE]) else emptyList()
            val bloodGlucoseData = if (HealthDataType.BLOOD_GLUCOSE in enabledTypes)
                readBloodGlucoseData(startTime, endTime, lastSyncTimestamps[HealthDataType.BLOOD_GLUCOSE]) else emptyList()
            val oxygenSaturationData = if (HealthDataType.OXYGEN_SATURATION in enabledTypes)
                readOxygenSaturationData(startTime, endTime, lastSyncTimestamps[HealthDataType.OXYGEN_SATURATION]) else emptyList()
            val bodyTemperatureData = if (HealthDataType.BODY_TEMPERATURE in enabledTypes)
                readBodyTemperatureData(startTime, endTime, lastSyncTimestamps[HealthDataType.BODY_TEMPERATURE]) else emptyList()
            val respiratoryRateData = if (HealthDataType.RESPIRATORY_RATE in enabledTypes)
                readRespiratoryRateData(startTime, endTime, lastSyncTimestamps[HealthDataType.RESPIRATORY_RATE]) else emptyList()
            val restingHeartRateData = if (HealthDataType.RESTING_HEART_RATE in enabledTypes)
                readRestingHeartRateData(startTime, endTime, lastSyncTimestamps[HealthDataType.RESTING_HEART_RATE]) else emptyList()
            val exerciseData = if (HealthDataType.EXERCISE in enabledTypes)
                readExerciseData(startTime, endTime, lastSyncTimestamps[HealthDataType.EXERCISE]) else emptyList()
            val hydrationData = if (HealthDataType.HYDRATION in enabledTypes)
                readHydrationData(startTime, endTime, lastSyncTimestamps[HealthDataType.HYDRATION]) else emptyList()
            val nutritionData = if (HealthDataType.NUTRITION in enabledTypes)
                readNutritionData(startTime, endTime, lastSyncTimestamps[HealthDataType.NUTRITION]) else emptyList()

            Result.success(
                HealthData(
                    steps = stepsData,
                    sleep = sleepData,
                    heartRate = heartRateData,
                    heartRateVariability = heartRateVariabilityData,
                    distance = distanceData,
                    activeCalories = activeCaloriesData,
                    totalCalories = totalCaloriesData,
                    weight = weightData,
                    height = heightData,
                    bloodPressure = bloodPressureData,
                    bloodGlucose = bloodGlucoseData,
                    oxygenSaturation = oxygenSaturationData,
                    bodyTemperature = bodyTemperatureData,
                    respiratoryRate = respiratoryRateData,
                    restingHeartRate = restingHeartRateData,
                    exercise = exerciseData,
                    hydration = hydrationData,
                    nutrition = nutritionData
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend inline fun <reified T : Record> readAllRecords(
        timeRangeFilter: TimeRangeFilter
    ): List<T> {
        val allRecords = mutableListOf<T>()
        var pageToken: String? = null

        do {
            val request = ReadRecordsRequest(
                recordType = T::class,
                timeRangeFilter = timeRangeFilter,
                pageToken = pageToken
            )
            val response = healthConnectClient.readRecords(request)
            allRecords.addAll(response.records)
            pageToken = response.pageToken
        } while (pageToken != null)

        return allRecords
    }

    private suspend fun readStepsData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<StepsData> {
        val zone = java.time.ZoneId.systemDefault()
        val result = mutableListOf<StepsData>()

        val startLocalDate = startTime.atZone(zone).toLocalDate()
        val endLocalDate = endTime.atZone(zone).toLocalDate()

        var currentDate = startLocalDate
        while (!currentDate.isAfter(endLocalDate)) {
            val dayStart = currentDate.atStartOfDay(zone).toInstant()
            val dayEnd = currentDate.plusDays(1).atStartOfDay(zone).toInstant()

            val queryStart = if (dayStart.isBefore(startTime)) startTime else dayStart
            val queryEnd = if (dayEnd.isAfter(endTime)) endTime else dayEnd

            if (lastSync != null && queryEnd.isBefore(lastSync)) {
                currentDate = currentDate.plusDays(1)
                continue
            }

            val request = AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(queryStart, queryEnd)
            )
            val response = healthConnectClient.aggregate(request)
            val daySteps = response[StepsRecord.COUNT_TOTAL] ?: 0L

            result.add(
                StepsData(
                    count = daySteps,
                    startTime = dayStart,
                    endTime = queryEnd
                )
            )

            currentDate = currentDate.plusDays(1)
        }

        return result
    }

    private suspend fun readSleepData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<SleepData> {
        val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        return readAllRecords<SleepSessionRecord>(timeRangeFilter)
            .filter { record ->
                lastSync == null || record.endTime >= lastSync
            }
            .map { record ->
                val stages = record.stages?.map { stage ->
                    SleepStage(
                        stage = stage.stage.toString(),
                        startTime = stage.startTime,
                        endTime = stage.endTime,
                        duration = Duration.between(stage.startTime, stage.endTime)
                    )
                } ?: emptyList()

                SleepData(
                    startTime = record.startTime,
                    sessionEndTime = record.endTime,
                    duration = Duration.between(record.startTime, record.endTime),
                    stages = stages
                )
            }
    }

    private suspend fun readHeartRateData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<HeartRateData> {
        val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        return readAllRecords<HeartRateRecord>(timeRangeFilter)
            .flatMap { record ->
                record.samples
                    .filter { lastSync == null || it.time >= lastSync }
                    .map { HeartRateData(it.beatsPerMinute, it.time) }
            }
    }

    private suspend fun readHeartRateVariabilityData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<HeartRateVariabilityData> {
        val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        return readAllRecords<HeartRateVariabilityRmssdRecord>(timeRangeFilter)
            .filter { lastSync == null || it.time >= lastSync }
            .map { HeartRateVariabilityData(it.heartRateVariabilityMillis, it.time) }
    }

    private suspend fun readDistanceData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<DistanceData> {
        val zone = java.time.ZoneId.systemDefault()
        val result = mutableListOf<DistanceData>()

        val startLocalDate = startTime.atZone(zone).toLocalDate()
        val endLocalDate = endTime.atZone(zone).toLocalDate()

        var currentDate = startLocalDate
        while (!currentDate.isAfter(endLocalDate)) {
            val dayStart = currentDate.atStartOfDay(zone).toInstant()
            val dayEnd = currentDate.plusDays(1).atStartOfDay(zone).toInstant()

            val queryStart = if (dayStart.isBefore(startTime)) startTime else dayStart
            val queryEnd = if (dayEnd.isAfter(endTime)) endTime else dayEnd

            if (lastSync != null && queryEnd.isBefore(lastSync)) {
                currentDate = currentDate.plusDays(1)
                continue
            }

            val request = AggregateRequest(
                metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(queryStart, queryEnd)
            )
            val response = healthConnectClient.aggregate(request)
            val dayDistance = response[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0

            result.add(
                DistanceData(
                    meters = dayDistance,
                    startTime = dayStart,
                    endTime = queryEnd
                )
            )

            currentDate = currentDate.plusDays(1)
        }

        return result
    }

    private suspend fun readActiveCaloriesData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<ActiveCaloriesData> {
        val zone = java.time.ZoneId.systemDefault()
        val result = mutableListOf<ActiveCaloriesData>()

        val startLocalDate = startTime.atZone(zone).toLocalDate()
        val endLocalDate = endTime.atZone(zone).toLocalDate()

        var currentDate = startLocalDate
        while (!currentDate.isAfter(endLocalDate)) {
            val dayStart = currentDate.atStartOfDay(zone).toInstant()
            val dayEnd = currentDate.plusDays(1).atStartOfDay(zone).toInstant()

            val queryStart = if (dayStart.isBefore(startTime)) startTime else dayStart
            val queryEnd = if (dayEnd.isAfter(endTime)) endTime else dayEnd

            if (lastSync != null && queryEnd.isBefore(lastSync)) {
                currentDate = currentDate.plusDays(1)
                continue
            }

            val request = AggregateRequest(
                metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(queryStart, queryEnd)
            )
            val response = healthConnectClient.aggregate(request)
            val dayCalories = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0

            result.add(
                ActiveCaloriesData(
                    calories = dayCalories,
                    startTime = dayStart,
                    endTime = queryEnd
                )
            )

            currentDate = currentDate.plusDays(1)
        }

        return result
    }

    private suspend fun readTotalCaloriesData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<TotalCaloriesData> {
        val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        return readAllRecords<TotalCaloriesBurnedRecord>(timeRangeFilter)
            .filter { lastSync == null || it.endTime >= lastSync }
            .map { TotalCaloriesData(it.energy.inKilocalories, it.startTime, it.endTime) }
    }

    private suspend fun readWeightData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<WeightData> {
        val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        return readAllRecords<WeightRecord>(timeRangeFilter)
            .filter { lastSync == null || it.time >= lastSync }
            .map { WeightData(it.weight.inKilograms, it.time) }
    }

    private suspend fun readHeightData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<HeightData> {
        val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        return readAllRecords<HeightRecord>(timeRangeFilter)
            .filter { lastSync == null || it.time >= lastSync }
            .map { HeightData(it.height.inMeters, it.time) }
    }

    private suspend fun readBloodPressureData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<BloodPressureData> {
        val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        return readAllRecords<BloodPressureRecord>(timeRangeFilter)
            .filter { lastSync == null || it.time >= lastSync }
            .map {
                BloodPressureData(
                    systolic = it.systolic.inMillimetersOfMercury,
                    diastolic = it.diastolic.inMillimetersOfMercury,
                    bodyPosition = it.bodyPosition,
                    measurementLocation = it.measurementLocation,
                    time = it.time
                )
            }
    }

    private suspend fun readBloodGlucoseData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<BloodGlucoseData> {
        val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        return readAllRecords<BloodGlucoseRecord>(timeRangeFilter)
            .filter { lastSync == null || it.time >= lastSync }
            .map {
                BloodGlucoseData(
                    mmolPerLiter = it.level.inMillimolesPerLiter,
                    specimenSource = it.specimenSource,
                    mealType = it.mealType,
                    relationToMeal = it.relationToMeal,
                    time = it.time
                )
            }
    }

    private suspend fun readOxygenSaturationData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<OxygenSaturationData> {
        val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        return readAllRecords<OxygenSaturationRecord>(timeRangeFilter)
            .filter { lastSync == null || it.time >= lastSync }
            .map { OxygenSaturationData(it.percentage.value, it.time) }
    }

    private suspend fun readBodyTemperatureData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<BodyTemperatureData> {
        val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        return readAllRecords<BodyTemperatureRecord>(timeRangeFilter)
            .filter { lastSync == null || it.time >= lastSync }
            .map { BodyTemperatureData(it.temperature.inCelsius, it.time) }
    }

    private suspend fun readRespiratoryRateData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<RespiratoryRateData> {
        val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        return readAllRecords<RespiratoryRateRecord>(timeRangeFilter)
            .filter { lastSync == null || it.time >= lastSync }
            .map { RespiratoryRateData(it.rate, it.time) }
    }

    private suspend fun readRestingHeartRateData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<RestingHeartRateData> {
        val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        return readAllRecords<RestingHeartRateRecord>(timeRangeFilter)
            .filter { lastSync == null || it.time >= lastSync }
            .map { RestingHeartRateData(it.beatsPerMinute, it.time) }
    }

    private suspend fun readExerciseData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<ExerciseData> {
        val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        return readAllRecords<ExerciseSessionRecord>(timeRangeFilter)
            .filter { lastSync == null || it.endTime >= lastSync }
            .map {
                ExerciseData(
                    type = it.exerciseType.toString(),
                    startTime = it.startTime,
                    endTime = it.endTime,
                    duration = Duration.between(it.startTime, it.endTime),
                    title = it.title,
                    notes = it.notes,
                    segments = it.segments.map { segment ->
                        ExerciseSegmentData(
                            type = segment.segmentType.toString(),
                            startTime = segment.startTime,
                            endTime = segment.endTime,
                            repetitions = segment.repetitions
                        )
                    },
                    laps = it.laps.map { lap ->
                        ExerciseLapData(
                            startTime = lap.startTime,
                            endTime = lap.endTime,
                            lengthMeters = lap.length?.inMeters
                        )
                    }
                )
            }
    }

    private suspend fun readHydrationData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<HydrationData> {
        val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        return readAllRecords<HydrationRecord>(timeRangeFilter)
            .filter { lastSync == null || it.endTime >= lastSync }
            .map { HydrationData(it.volume.inLiters, it.startTime, it.endTime) }
    }

    private suspend fun readNutritionData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<NutritionData> {
        val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        return readAllRecords<NutritionRecord>(timeRangeFilter)
            .filter { lastSync == null || it.endTime >= lastSync }
            .map {
                NutritionData(
                    calories = it.energy?.inKilocalories,
                    energyFromFatKilocalories = it.energyFromFat?.inKilocalories,
                    proteinGrams = it.protein?.inGrams,
                    carbsGrams = it.totalCarbohydrate?.inGrams,
                    fatGrams = it.totalFat?.inGrams,
                    dietaryFiberGrams = it.dietaryFiber?.inGrams,
                    sugarGrams = it.sugar?.inGrams,
                    saturatedFatGrams = it.saturatedFat?.inGrams,
                    transFatGrams = it.transFat?.inGrams,
                    unsaturatedFatGrams = it.unsaturatedFat?.inGrams,
                    monounsaturatedFatGrams = it.monounsaturatedFat?.inGrams,
                    polyunsaturatedFatGrams = it.polyunsaturatedFat?.inGrams,
                    cholesterolMilligrams = it.cholesterol?.inMilligrams,
                    sodiumMilligrams = it.sodium?.inMilligrams,
                    potassiumMilligrams = it.potassium?.inMilligrams,
                    calciumMilligrams = it.calcium?.inMilligrams,
                    ironMilligrams = it.iron?.inMilligrams,
                    magnesiumMilligrams = it.magnesium?.inMilligrams,
                    phosphorusMilligrams = it.phosphorus?.inMilligrams,
                    chlorideMilligrams = it.chloride?.inMilligrams,
                    chromiumMilligrams = it.chromium?.inMilligrams,
                    copperMilligrams = it.copper?.inMilligrams,
                    iodineMilligrams = it.iodine?.inMilligrams,
                    manganeseMilligrams = it.manganese?.inMilligrams,
                    molybdenumMilligrams = it.molybdenum?.inMilligrams,
                    seleniumMilligrams = it.selenium?.inMilligrams,
                    zincMilligrams = it.zinc?.inMilligrams,
                    vitaminAMilligrams = it.vitaminA?.inMilligrams,
                    vitaminB6Milligrams = it.vitaminB6?.inMilligrams,
                    vitaminB12Milligrams = it.vitaminB12?.inMilligrams,
                    vitaminCMilligrams = it.vitaminC?.inMilligrams,
                    vitaminDMilligrams = it.vitaminD?.inMilligrams,
                    vitaminEMilligrams = it.vitaminE?.inMilligrams,
                    vitaminKMilligrams = it.vitaminK?.inMilligrams,
                    biotinMilligrams = it.biotin?.inMilligrams,
                    folateMilligrams = it.folate?.inMilligrams,
                    folicAcidMilligrams = it.folicAcid?.inMilligrams,
                    niacinMilligrams = it.niacin?.inMilligrams,
                    pantothenicAcidMilligrams = it.pantothenicAcid?.inMilligrams,
                    riboflavinMilligrams = it.riboflavin?.inMilligrams,
                    thiaminMilligrams = it.thiamin?.inMilligrams,
                    caffeineMilligrams = it.caffeine?.inMilligrams,
                    name = it.name,
                    mealType = it.mealType,
                    startTime = it.startTime,
                    endTime = it.endTime
                )
            }
    }

    fun isHealthConnectAvailable(): Boolean {
        return try {
            HealthConnectClient.getOrCreate(context)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun hasPermissions(requiredPermissions: Set<String> = ALL_PERMISSIONS): Boolean {
        if (!isHealthConnectAvailable()) return false
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return requiredPermissions.all { it in granted }
    }

    suspend fun getGrantedPermissions(): Set<String> {
        if (!isHealthConnectAvailable()) return emptySet()
        return healthConnectClient.permissionController.getGrantedPermissions()
    }

    suspend fun requestPermissions(permissions: Set<String>): android.content.Intent {
        if (!isHealthConnectAvailable()) {
            throw IllegalStateException("Health Connect is not available on this device")
        }
        val contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        return contract.createIntent(context, permissions.toTypedArray())
    }

    companion object {
        private const val DEFAULT_LOOKBACK_HOURS = 48L

        fun getPermissionsForTypes(types: Set<HealthDataType>): Set<String> {
            val permissions = types.map { HealthPermission.getReadPermission(it.recordClass) }.toMutableSet()
            permissions.add("android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND")
            return permissions
        }

        val ALL_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(HeightRecord::class),
            HealthPermission.getReadPermission(BloodPressureRecord::class),
            HealthPermission.getReadPermission(BloodGlucoseRecord::class),
            HealthPermission.getReadPermission(OxygenSaturationRecord::class),
            HealthPermission.getReadPermission(BodyTemperatureRecord::class),
            HealthPermission.getReadPermission(RespiratoryRateRecord::class),
            HealthPermission.getReadPermission(RestingHeartRateRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(HydrationRecord::class),
            HealthPermission.getReadPermission(NutritionRecord::class),
            "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
        )
    }
}
