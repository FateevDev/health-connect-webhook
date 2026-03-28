# Plan: Resend, Custom Lookback, Send All Data

## Context

The app forwards Health Connect data to webhooks but has three gaps:
1. No way to resend already-sent data without advancing the sync cursor
2. Lookback window is hardcoded to 48 hours (sync frequency is already configurable)
3. Data is silently dropped: no pagination, missing fields, zero-value filtering, single-webhook delivery

## Decisions Made

- **"Custom sync interval"** = configurable lookback window (frequency already exists in UI)
- **Resend** = explicit toggle only; normal manual syncs still advance cursor (current behavior preserved)
- **Data completeness** = pagination fix + webhook fix + expand Nutrition, Exercise, Sleep, BloodGlucose, BloodPressure fields + zero-value fix. Defer record metadata and HeartRate restructuring.

---

## Implementation Steps

### Step 1: Pagination Helper (~45 LOC)

**File:** `HealthConnectManager.kt`

Add a generic helper after line ~176 that loops on `pageToken`:

```kotlin
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
```

Replace `healthConnectClient.readRecords(request)` in all 15 non-aggregated read methods (Sleep, HeartRate, HRV, TotalCalories, Weight, Height, BloodPressure, BloodGlucose, OxygenSaturation, BodyTemperature, RespiratoryRate, RestingHeartRate, Exercise, Hydration, Nutrition) with `readAllRecords<XxxRecord>(...)`. Steps, Distance, ActiveCalories use `aggregate()` — no change needed.

### Step 2: Expand Data Classes (~80 LOC)

**File:** `HealthConnectManager.kt`

| Data Class | Add Fields |
|---|---|
| `NutritionData` | +33 nutrient fields (fiber, sugar, saturatedFat, transFat, cholesterol, sodium, potassium, calcium, iron, all vitamins, minerals, caffeine, name, mealType) |
| `ExerciseData` | +title, notes, segments (`List<ExerciseSegmentData>`), laps (`List<ExerciseLapData>`) — new nested data classes |
| `SleepData` | +startTime |
| `BloodGlucoseData` | +specimenSource, mealType, relationToMeal (all Int) |
| `BloodPressureData` | +bodyPosition, measurementLocation (both Int) |

### Step 3: Update readXxx Methods (~50 LOC)

**File:** `HealthConnectManager.kt`

Update the `.map {}` blocks in `readNutritionData`, `readExerciseData`, `readSleepData`, `readBloodGlucoseData`, `readBloodPressureData` to populate new fields from Health Connect SDK record objects.

Key SDK property mappings:
- Nutrition: `it.dietaryFiber?.inGrams`, `it.sugar?.inGrams`, etc. (Mass → `.inGrams`/`.inMilligrams`; Energy → `.inKilocalories`)
- Exercise: `record.title`, `record.notes`, `record.segments.map { ... }`, `record.laps.map { ... }`
- BloodGlucose: `it.specimenSource`, `it.mealType`, `it.relationToMeal` (Int constants)
- BloodPressure: `it.bodyPosition`, `it.measurementLocation` (Int constants)

### Step 4: Update `buildJsonPayload` (~100 LOC)

**File:** `SyncManager.kt`

Add a `putNullable` helper for nullable doubles:
```kotlin
fun JsonObjectBuilder.putNullable(key: String, value: Double?) {
    put(key, value?.let { JsonPrimitive(it) } ?: JsonNull)
}
```

Expand JSON serialization for:
- **Nutrition** (~40 fields): all nutrients as snake_case keys, nulls as JSON null (not omitted)
- **Exercise**: add `title`, `notes`, nested `segments` array (type, start_time, end_time, repetitions), nested `laps` array (start_time, end_time, length_meters)
- **Sleep**: add `start_time`
- **BloodGlucose**: add `specimen_source`, `meal_type`, `relation_to_meal`
- **BloodPressure**: add `body_position`, `measurement_location`

### Step 5: Zero-Value Fix (-6 LOC)

**File:** `HealthConnectManager.kt`

Remove three `if > 0` guards:
- `readStepsData` line 288: remove `if (daySteps > 0)` wrapper
- `readDistanceData` line ~382: remove `if (dayDistance > 0.0)` wrapper
- `readActiveCaloriesData` line ~424: remove `if (dayCalories > 0.0)` wrapper

### Step 6: WebhookManager — Post to ALL Webhooks (~5 LOC)

**File:** `WebhookManager.kt`

Replace early-return-on-success with post-to-all:
```kotlin
var anySuccess = false
var lastFailure: Exception? = null
for (config in webhookConfigs) {
    val result = postToUrl(config, jsonPayload)
    if (result.isSuccess) anySuccess = true
    else lastFailure = result.exceptionOrNull() as? Exception ?: Exception("Unknown error")
}
return if (anySuccess) Result.success(Unit)
else Result.failure(lastFailure ?: IOException("All webhook posts failed"))
```

### Step 7: Configurable Lookback Window (~49 LOC)

**Files:** `PreferencesManager.kt`, `SettingsExport.kt`, `HealthConnectManager.kt`, `SyncManager.kt`, `ConfigurationScreen.kt`

1. **PreferencesManager**: Add `getLookbackHours()`/`setLookbackHours()` (default 48), add to export/import
2. **SettingsExport**: Add `lookbackHours: Long = 48L` field
3. **HealthConnectManager**: Add `lookbackHours: Long = 48L` param to `readHealthData()`, use instead of `LOOKBACK_HOURS` constant
4. **SyncManager**: Read preference, pass to `readHealthData()`
5. **ConfigurationScreen**: Add "Lookback Window (Hours)" number input + save button in Sync Schedule section (always visible regardless of sync mode)

### Step 8: Resend Mode (~28 LOC)

**Files:** `SyncManager.kt`, `ManualSyncCard.kt`

1. **SyncManager**: Add `resend: Boolean = false` param to `performSync()`. When `resend=true`: use `emptyMap()` for lastSyncTimestamps AND skip `updateSyncTimestamps()` call
2. **ManualSyncCard**: Add "Resend mode" switch with description "Re-send data without advancing sync cursor". Pass `resend=resendMode` to `performSync()`

---

## Implementation Order

```
Step 1 (Pagination) ─────┐
Step 2 (Data classes) ────┤─ all in HealthConnectManager.kt
Step 3 (readXxx methods) ─┤
Step 5 (Zero-value fix) ──┘
Step 4 (buildJsonPayload) ─── SyncManager.kt (depends on Steps 2-3)
Step 6 (Webhook fix) ──────── WebhookManager.kt (independent)
Step 7 (Lookback) ─────────── 5 files (independent)
Step 8 (Resend) ───────────── 2 files (independent, do after Step 7)
```

**Total: ~351 LOC across 7 files**

## Risk Areas

- **NutritionRecord SDK properties**: Exact names need verification (e.g. `dietaryFiber` vs `fiber`). Check `androidx.health.connect.client.records.NutritionRecord` API surface.
- **ExerciseSessionRecord.segments/laps**: Verify `ExerciseSegment.repetitions` type and `ExerciseLap.length?.inMeters` availability.
- **BloodGlucose/BloodPressure int constants**: `specimenSource`, `mealType`, `relationToMeal`, `bodyPosition`, `measurementLocation` are int constants, not Kotlin enums. Values map to `SPECIMEN_SOURCE_*`, `MEAL_TYPE_*`, etc.
- **Inline reified generics**: `readAllRecords` must be `private suspend inline` with `reified T` — cannot be called from Java, but codebase is all Kotlin.
- **Large payloads**: With pagination fix, heart rate data could produce very large payloads. No chunking planned — monitor in practice.

## Verification

1. `./gradlew assembleDebug` — must compile
2. Install on device, configure webhook URL(s)
3. **Pagination**: Generate >1000 heart rate records, verify all sent (check record count in payload)
4. **Fields**: Inspect webhook payload for:
   - Nutrition: all ~40 fields present, nulls as JSON null
   - Exercise: title, notes, segments, laps present
   - Sleep: start_time present
   - BloodGlucose: specimen_source, meal_type, relation_to_meal
   - BloodPressure: body_position, measurement_location
5. **Zero-value**: Verify days with 0 steps appear in payload
6. **Multi-webhook**: Configure 2 URLs, verify both receive payload
7. **Lookback**: Set to 168h (7 days), trigger sync, verify payload covers 7 days
8. **Resend**: Sync normally, then resend same range — verify next auto-sync still picks up new data (timestamps not advanced)

## Critical Files

- `app/src/main/java/com/hcwebhook/app/HealthConnectManager.kt` — Steps 1-3, 5
- `app/src/main/java/com/hcwebhook/app/SyncManager.kt` — Steps 4, 7d, 8a
- `app/src/main/java/com/hcwebhook/app/WebhookManager.kt` — Step 6
- `app/src/main/java/com/hcwebhook/app/PreferencesManager.kt` — Step 7a
- `app/src/main/java/com/hcwebhook/app/SettingsExport.kt` — Step 7b
- `app/src/main/java/com/hcwebhook/app/screens/ConfigurationScreen.kt` — Step 7e
- `app/src/main/java/com/hcwebhook/app/components/ManualSyncCard.kt` — Step 8b

---

## Deferred (Future)

- **Record metadata** (id, dataOrigin, device, lastModifiedTime) for all types — large cross-cutting change
- **HeartRate record grouping** — preserve parent record structure instead of flattening samples
- **Payload chunking/compression** — if large payloads become a problem in practice
- **Resend Tier 2** — stored payload replay (browse + replay cached payloads)
