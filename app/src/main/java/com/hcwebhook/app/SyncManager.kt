package com.hcwebhook.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.time.Instant

class SyncManager(private val context: Context) {

    private val preferencesManager = PreferencesManager(context)
    private val healthConnectManager = HealthConnectManager(context)

    suspend fun performSync(
        timeRangeDays: Int? = null,
        resend: Boolean = false
    ): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            val webhookConfigs = preferencesManager.getWebhookConfigs()

            if (webhookConfigs.isEmpty()) {
                return@withContext Result.failure(Exception("No webhook URLs configured"))
            }

            val enabledTypes = preferencesManager.getEnabledDataTypes()
            if (enabledTypes.isEmpty()) {
                return@withContext Result.failure(Exception("No data types enabled"))
            }

            val lookbackHours = preferencesManager.getLookbackHours()
            val lastSyncTimestamps = if (timeRangeDays == null && !resend) {
                enabledTypes.associateWith { type ->
                    preferencesManager.getLastSyncTimestamp(type)?.let { Instant.ofEpochMilli(it) }
                }
            } else {
                emptyMap()
            }

            val healthDataResult = healthConnectManager.readHealthData(
                enabledTypes = enabledTypes,
                lastSyncTimestamps = lastSyncTimestamps,
                timeRangeDays = timeRangeDays,
                lookbackHours = lookbackHours
            )
            if (healthDataResult.isFailure) {
                return@withContext Result.failure(healthDataResult.exceptionOrNull() ?: Exception("Failed to read health data"))
            }

            val healthData = healthDataResult.getOrThrow()

            if (isHealthDataEmpty(healthData)) {
                preferencesManager.setLastSyncTime(Instant.now().toEpochMilli())
                preferencesManager.setLastSyncSummary("No new data")
                return@withContext Result.success(SyncResult.NoData)
            }

            val totalRecords = healthData.steps.size + healthData.sleep.size + healthData.heartRate.size +
                healthData.heartRateVariability.size +
                healthData.distance.size + healthData.activeCalories.size + healthData.totalCalories.size +
                healthData.weight.size + healthData.height.size + healthData.bloodPressure.size +
                healthData.bloodGlucose.size + healthData.oxygenSaturation.size + healthData.bodyTemperature.size +
                healthData.respiratoryRate.size + healthData.restingHeartRate.size + healthData.exercise.size +
                healthData.hydration.size + healthData.nutrition.size

            val webhookManager = WebhookManager(
                webhookConfigs = webhookConfigs,
                context = context,
                dataType = "all",
                recordCount = totalRecords
            )

            val jsonPayload = buildJsonPayload(healthData)

            val postResult = webhookManager.postData(jsonPayload)
            if (postResult.isFailure) {
                return@withContext Result.failure(postResult.exceptionOrNull() ?: Exception("Failed to post to webhooks"))
            }

            val syncCounts = mutableMapOf<HealthDataType, Int>()
            if (!resend) {
                updateSyncTimestamps(healthData, syncCounts)
            } else {
                populateSyncCounts(healthData, syncCounts)
            }

            val summary = buildSyncSummary(healthData)
            preferencesManager.setLastSyncTime(Instant.now().toEpochMilli())
            preferencesManager.setLastSyncSummary(summary)

            Result.success(SyncResult.Success(syncCounts))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isHealthDataEmpty(data: HealthData): Boolean {
        return data.steps.isEmpty() && data.sleep.isEmpty() && data.heartRate.isEmpty() &&
            data.heartRateVariability.isEmpty() &&
            data.distance.isEmpty() && data.activeCalories.isEmpty() && data.totalCalories.isEmpty() &&
            data.weight.isEmpty() && data.height.isEmpty() && data.bloodPressure.isEmpty() &&
            data.bloodGlucose.isEmpty() && data.oxygenSaturation.isEmpty() && data.bodyTemperature.isEmpty() &&
            data.respiratoryRate.isEmpty() && data.restingHeartRate.isEmpty() && data.exercise.isEmpty() &&
            data.hydration.isEmpty() && data.nutrition.isEmpty()
    }

    private fun updateSyncTimestamps(data: HealthData, syncCounts: MutableMap<HealthDataType, Int>) {
        if (data.steps.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.STEPS, data.steps.maxOf { it.endTime }.toEpochMilli())
            syncCounts[HealthDataType.STEPS] = data.steps.size
        }
        if (data.sleep.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.SLEEP, data.sleep.maxOf { it.sessionEndTime }.toEpochMilli())
            syncCounts[HealthDataType.SLEEP] = data.sleep.size
        }
        if (data.heartRate.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.HEART_RATE, data.heartRate.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.HEART_RATE] = data.heartRate.size
        }
        if (data.heartRateVariability.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.HEART_RATE_VARIABILITY, data.heartRateVariability.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.HEART_RATE_VARIABILITY] = data.heartRateVariability.size
        }
        if (data.distance.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.DISTANCE, data.distance.maxOf { it.endTime }.toEpochMilli())
            syncCounts[HealthDataType.DISTANCE] = data.distance.size
        }
        if (data.activeCalories.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.ACTIVE_CALORIES, data.activeCalories.maxOf { it.endTime }.toEpochMilli())
            syncCounts[HealthDataType.ACTIVE_CALORIES] = data.activeCalories.size
        }
        if (data.totalCalories.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.TOTAL_CALORIES, data.totalCalories.maxOf { it.endTime }.toEpochMilli())
            syncCounts[HealthDataType.TOTAL_CALORIES] = data.totalCalories.size
        }
        if (data.weight.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.WEIGHT, data.weight.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.WEIGHT] = data.weight.size
        }
        if (data.height.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.HEIGHT, data.height.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.HEIGHT] = data.height.size
        }
        if (data.bloodPressure.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.BLOOD_PRESSURE, data.bloodPressure.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.BLOOD_PRESSURE] = data.bloodPressure.size
        }
        if (data.bloodGlucose.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.BLOOD_GLUCOSE, data.bloodGlucose.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.BLOOD_GLUCOSE] = data.bloodGlucose.size
        }
        if (data.oxygenSaturation.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.OXYGEN_SATURATION, data.oxygenSaturation.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.OXYGEN_SATURATION] = data.oxygenSaturation.size
        }
        if (data.bodyTemperature.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.BODY_TEMPERATURE, data.bodyTemperature.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.BODY_TEMPERATURE] = data.bodyTemperature.size
        }
        if (data.respiratoryRate.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.RESPIRATORY_RATE, data.respiratoryRate.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.RESPIRATORY_RATE] = data.respiratoryRate.size
        }
        if (data.restingHeartRate.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.RESTING_HEART_RATE, data.restingHeartRate.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.RESTING_HEART_RATE] = data.restingHeartRate.size
        }
        if (data.exercise.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.EXERCISE, data.exercise.maxOf { it.endTime }.toEpochMilli())
            syncCounts[HealthDataType.EXERCISE] = data.exercise.size
        }
        if (data.hydration.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.HYDRATION, data.hydration.maxOf { it.endTime }.toEpochMilli())
            syncCounts[HealthDataType.HYDRATION] = data.hydration.size
        }
        if (data.nutrition.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.NUTRITION, data.nutrition.maxOf { it.endTime }.toEpochMilli())
            syncCounts[HealthDataType.NUTRITION] = data.nutrition.size
        }
    }

    private fun populateSyncCounts(data: HealthData, syncCounts: MutableMap<HealthDataType, Int>) {
        if (data.steps.isNotEmpty()) syncCounts[HealthDataType.STEPS] = data.steps.size
        if (data.sleep.isNotEmpty()) syncCounts[HealthDataType.SLEEP] = data.sleep.size
        if (data.heartRate.isNotEmpty()) syncCounts[HealthDataType.HEART_RATE] = data.heartRate.size
        if (data.heartRateVariability.isNotEmpty()) syncCounts[HealthDataType.HEART_RATE_VARIABILITY] = data.heartRateVariability.size
        if (data.distance.isNotEmpty()) syncCounts[HealthDataType.DISTANCE] = data.distance.size
        if (data.activeCalories.isNotEmpty()) syncCounts[HealthDataType.ACTIVE_CALORIES] = data.activeCalories.size
        if (data.totalCalories.isNotEmpty()) syncCounts[HealthDataType.TOTAL_CALORIES] = data.totalCalories.size
        if (data.weight.isNotEmpty()) syncCounts[HealthDataType.WEIGHT] = data.weight.size
        if (data.height.isNotEmpty()) syncCounts[HealthDataType.HEIGHT] = data.height.size
        if (data.bloodPressure.isNotEmpty()) syncCounts[HealthDataType.BLOOD_PRESSURE] = data.bloodPressure.size
        if (data.bloodGlucose.isNotEmpty()) syncCounts[HealthDataType.BLOOD_GLUCOSE] = data.bloodGlucose.size
        if (data.oxygenSaturation.isNotEmpty()) syncCounts[HealthDataType.OXYGEN_SATURATION] = data.oxygenSaturation.size
        if (data.bodyTemperature.isNotEmpty()) syncCounts[HealthDataType.BODY_TEMPERATURE] = data.bodyTemperature.size
        if (data.respiratoryRate.isNotEmpty()) syncCounts[HealthDataType.RESPIRATORY_RATE] = data.respiratoryRate.size
        if (data.restingHeartRate.isNotEmpty()) syncCounts[HealthDataType.RESTING_HEART_RATE] = data.restingHeartRate.size
        if (data.exercise.isNotEmpty()) syncCounts[HealthDataType.EXERCISE] = data.exercise.size
        if (data.hydration.isNotEmpty()) syncCounts[HealthDataType.HYDRATION] = data.hydration.size
        if (data.nutrition.isNotEmpty()) syncCounts[HealthDataType.NUTRITION] = data.nutrition.size
    }

    private fun buildSyncSummary(data: HealthData): String {
        val parts = mutableListOf<String>()

        if (data.steps.isNotEmpty()) {
            val total = data.steps.sumOf { it.count }
            parts.add("%,d steps".format(total))
        }
        if (data.distance.isNotEmpty()) {
            val totalKm = data.distance.sumOf { it.meters } / 1000.0
            parts.add("%.1f km".format(totalKm))
        }
        if (data.activeCalories.isNotEmpty()) {
            val total = data.activeCalories.sumOf { it.calories }.toInt()
            parts.add("$total cal")
        }
        if (data.sleep.isNotEmpty()) {
            parts.add("${data.sleep.size} sleep")
        }
        if (data.exercise.isNotEmpty()) {
            parts.add("${data.exercise.size} exercise")
        }
        if (data.weight.isNotEmpty()) {
            parts.add("${data.weight.size} weight")
        }
        if (data.heartRate.isNotEmpty()) {
            parts.add("${data.heartRate.size} HR")
        }
        if (data.heartRateVariability.isNotEmpty()) {
            parts.add("${data.heartRateVariability.size} HRV")
        }

        return if (parts.isEmpty()) "No new data" else parts.joinToString(" · ")
    }

    private fun buildJsonPayload(healthData: HealthData): String {
        fun JsonObjectBuilder.putNullable(key: String, value: Double?) {
            put(key, value?.let { JsonPrimitive(it) } ?: JsonNull)
        }

        fun JsonObjectBuilder.putNullable(key: String, value: String?) {
            put(key, value?.let { JsonPrimitive(it) } ?: JsonNull)
        }

        val json = buildJsonObject {
            put("timestamp", Instant.now().toString())
            put("app_version", "1.0")

            if (healthData.steps.isNotEmpty()) {
                putJsonArray("steps") {
                    healthData.steps.forEach { step ->
                        add(
                            buildJsonObject {
                                put("count", step.count)
                                put("start_time", step.startTime.toString())
                                put("end_time", step.endTime.toString())
                            }
                        )
                    }
                }
            }

            if (healthData.sleep.isNotEmpty()) {
                putJsonArray("sleep") {
                    healthData.sleep.forEach { sleep ->
                        add(
                            buildJsonObject {
                                put("start_time", sleep.startTime.toString())
                                put("session_end_time", sleep.sessionEndTime.toString())
                                put("duration_seconds", sleep.duration.seconds)
                                putJsonArray("stages") {
                                    sleep.stages.forEach { stage ->
                                        add(
                                            buildJsonObject {
                                                put("stage", stage.stage)
                                                put("start_time", stage.startTime.toString())
                                                put("end_time", stage.endTime.toString())
                                                put("duration_seconds", stage.duration.seconds)
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (healthData.heartRate.isNotEmpty()) {
                putJsonArray("heart_rate") {
                    healthData.heartRate.forEach { record ->
                        add(
                            buildJsonObject {
                                put("bpm", record.bpm)
                                put("time", record.time.toString())
                            }
                        )
                    }
                }
            }

            if (healthData.heartRateVariability.isNotEmpty()) {
                putJsonArray("heart_rate_variability") {
                    healthData.heartRateVariability.forEach { record ->
                        add(
                            buildJsonObject {
                                put("rmssd_millis", record.rmssdMillis)
                                put("time", record.time.toString())
                            }
                        )
                    }
                }
            }

            if (healthData.distance.isNotEmpty()) {
                putJsonArray("distance") {
                    healthData.distance.forEach { record ->
                        add(
                            buildJsonObject {
                                put("meters", record.meters)
                                put("start_time", record.startTime.toString())
                                put("end_time", record.endTime.toString())
                            }
                        )
                    }
                }
            }

            if (healthData.activeCalories.isNotEmpty()) {
                putJsonArray("active_calories") {
                    healthData.activeCalories.forEach { record ->
                        add(
                            buildJsonObject {
                                put("calories", record.calories)
                                put("start_time", record.startTime.toString())
                                put("end_time", record.endTime.toString())
                            }
                        )
                    }
                }
            }

            if (healthData.totalCalories.isNotEmpty()) {
                putJsonArray("total_calories") {
                    healthData.totalCalories.forEach { record ->
                        add(
                            buildJsonObject {
                                put("calories", record.calories)
                                put("start_time", record.startTime.toString())
                                put("end_time", record.endTime.toString())
                            }
                        )
                    }
                }
            }

            if (healthData.weight.isNotEmpty()) {
                putJsonArray("weight") {
                    healthData.weight.forEach { record ->
                        add(
                            buildJsonObject {
                                put("kilograms", record.kilograms)
                                put("time", record.time.toString())
                            }
                        )
                    }
                }
            }

            if (healthData.height.isNotEmpty()) {
                putJsonArray("height") {
                    healthData.height.forEach { record ->
                        add(
                            buildJsonObject {
                                put("meters", record.meters)
                                put("time", record.time.toString())
                            }
                        )
                    }
                }
            }

            if (healthData.bloodPressure.isNotEmpty()) {
                putJsonArray("blood_pressure") {
                    healthData.bloodPressure.forEach { record ->
                        add(
                            buildJsonObject {
                                put("systolic", record.systolic)
                                put("diastolic", record.diastolic)
                                put("body_position", record.bodyPosition)
                                put("measurement_location", record.measurementLocation)
                                put("time", record.time.toString())
                            }
                        )
                    }
                }
            }

            if (healthData.bloodGlucose.isNotEmpty()) {
                putJsonArray("blood_glucose") {
                    healthData.bloodGlucose.forEach { record ->
                        add(
                            buildJsonObject {
                                put("mmol_per_liter", record.mmolPerLiter)
                                put("specimen_source", record.specimenSource)
                                put("meal_type", record.mealType)
                                put("relation_to_meal", record.relationToMeal)
                                put("time", record.time.toString())
                            }
                        )
                    }
                }
            }

            if (healthData.oxygenSaturation.isNotEmpty()) {
                putJsonArray("oxygen_saturation") {
                    healthData.oxygenSaturation.forEach { record ->
                        add(
                            buildJsonObject {
                                put("percentage", record.percentage)
                                put("time", record.time.toString())
                            }
                        )
                    }
                }
            }

            if (healthData.bodyTemperature.isNotEmpty()) {
                putJsonArray("body_temperature") {
                    healthData.bodyTemperature.forEach { record ->
                        add(
                            buildJsonObject {
                                put("celsius", record.celsius)
                                put("time", record.time.toString())
                            }
                        )
                    }
                }
            }

            if (healthData.respiratoryRate.isNotEmpty()) {
                putJsonArray("respiratory_rate") {
                    healthData.respiratoryRate.forEach { record ->
                        add(
                            buildJsonObject {
                                put("rate", record.rate)
                                put("time", record.time.toString())
                            }
                        )
                    }
                }
            }

            if (healthData.restingHeartRate.isNotEmpty()) {
                putJsonArray("resting_heart_rate") {
                    healthData.restingHeartRate.forEach { record ->
                        add(
                            buildJsonObject {
                                put("bpm", record.bpm)
                                put("time", record.time.toString())
                            }
                        )
                    }
                }
            }

            if (healthData.exercise.isNotEmpty()) {
                putJsonArray("exercise") {
                    healthData.exercise.forEach { record ->
                        add(
                            buildJsonObject {
                                put("type", record.type)
                                putNullable("title", record.title)
                                putNullable("notes", record.notes)
                                put("start_time", record.startTime.toString())
                                put("end_time", record.endTime.toString())
                                put("duration_seconds", record.duration.seconds)
                                putJsonArray("segments") {
                                    record.segments.forEach { segment ->
                                        add(
                                            buildJsonObject {
                                                put("type", segment.type)
                                                put("start_time", segment.startTime.toString())
                                                put("end_time", segment.endTime.toString())
                                                put("repetitions", segment.repetitions)
                                            }
                                        )
                                    }
                                }
                                putJsonArray("laps") {
                                    record.laps.forEach { lap ->
                                        add(
                                            buildJsonObject {
                                                put("start_time", lap.startTime.toString())
                                                put("end_time", lap.endTime.toString())
                                                putNullable("length_meters", lap.lengthMeters)
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (healthData.hydration.isNotEmpty()) {
                putJsonArray("hydration") {
                    healthData.hydration.forEach { record ->
                        add(
                            buildJsonObject {
                                put("liters", record.liters)
                                put("start_time", record.startTime.toString())
                                put("end_time", record.endTime.toString())
                            }
                        )
                    }
                }
            }

            if (healthData.nutrition.isNotEmpty()) {
                putJsonArray("nutrition") {
                    healthData.nutrition.forEach { record ->
                        add(
                            buildJsonObject {
                                putNullable("calories", record.calories)
                                putNullable("energy_from_fat_kilocalories", record.energyFromFatKilocalories)
                                putNullable("protein_grams", record.proteinGrams)
                                putNullable("carbs_grams", record.carbsGrams)
                                putNullable("fat_grams", record.fatGrams)
                                putNullable("dietary_fiber_grams", record.dietaryFiberGrams)
                                putNullable("sugar_grams", record.sugarGrams)
                                putNullable("saturated_fat_grams", record.saturatedFatGrams)
                                putNullable("trans_fat_grams", record.transFatGrams)
                                putNullable("unsaturated_fat_grams", record.unsaturatedFatGrams)
                                putNullable("monounsaturated_fat_grams", record.monounsaturatedFatGrams)
                                putNullable("polyunsaturated_fat_grams", record.polyunsaturatedFatGrams)
                                putNullable("cholesterol_milligrams", record.cholesterolMilligrams)
                                putNullable("sodium_milligrams", record.sodiumMilligrams)
                                putNullable("potassium_milligrams", record.potassiumMilligrams)
                                putNullable("calcium_milligrams", record.calciumMilligrams)
                                putNullable("iron_milligrams", record.ironMilligrams)
                                putNullable("magnesium_milligrams", record.magnesiumMilligrams)
                                putNullable("phosphorus_milligrams", record.phosphorusMilligrams)
                                putNullable("chloride_milligrams", record.chlorideMilligrams)
                                putNullable("chromium_milligrams", record.chromiumMilligrams)
                                putNullable("copper_milligrams", record.copperMilligrams)
                                putNullable("iodine_milligrams", record.iodineMilligrams)
                                putNullable("manganese_milligrams", record.manganeseMilligrams)
                                putNullable("molybdenum_milligrams", record.molybdenumMilligrams)
                                putNullable("selenium_milligrams", record.seleniumMilligrams)
                                putNullable("zinc_milligrams", record.zincMilligrams)
                                putNullable("vitamin_a_milligrams", record.vitaminAMilligrams)
                                putNullable("vitamin_b6_milligrams", record.vitaminB6Milligrams)
                                putNullable("vitamin_b12_milligrams", record.vitaminB12Milligrams)
                                putNullable("vitamin_c_milligrams", record.vitaminCMilligrams)
                                putNullable("vitamin_d_milligrams", record.vitaminDMilligrams)
                                putNullable("vitamin_e_milligrams", record.vitaminEMilligrams)
                                putNullable("vitamin_k_milligrams", record.vitaminKMilligrams)
                                putNullable("biotin_milligrams", record.biotinMilligrams)
                                putNullable("folate_milligrams", record.folateMilligrams)
                                putNullable("folic_acid_milligrams", record.folicAcidMilligrams)
                                putNullable("niacin_milligrams", record.niacinMilligrams)
                                putNullable("pantothenic_acid_milligrams", record.pantothenicAcidMilligrams)
                                putNullable("riboflavin_milligrams", record.riboflavinMilligrams)
                                putNullable("thiamin_milligrams", record.thiaminMilligrams)
                                putNullable("caffeine_milligrams", record.caffeineMilligrams)
                                putNullable("name", record.name)
                                put("meal_type", record.mealType)
                                put("start_time", record.startTime.toString())
                                put("end_time", record.endTime.toString())
                            }
                        )
                    }
                }
            }
        }

        return json.toString()
    }
}

sealed class SyncResult {
    object NoData : SyncResult()
    data class Success(val syncCounts: Map<HealthDataType, Int>) : SyncResult()
}
