package com.ammar.wallflow.ui.screens.settings.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.ObjectDetectionDelegate
import com.ammar.wallflow.model.WallpaperTarget
import kotlinx.datetime.DateTimePeriod

@Composable
internal fun delegateString(delegate: ObjectDetectionDelegate) = stringResource(
    when (delegate) {
        ObjectDetectionDelegate.NONE -> R.string.cpu
        ObjectDetectionDelegate.NNAPI -> R.string.nnapi
        ObjectDetectionDelegate.GPU -> R.string.gpu
    },
)

@Composable
internal fun wallpaperTargetString(wallpaperTarget: WallpaperTarget) = stringResource(
    when (wallpaperTarget) {
        WallpaperTarget.HOME -> R.string.home_screen
        WallpaperTarget.LOCKSCREEN -> R.string.lock_screen
    },
)

@Suppress("SimplifiableCallChain")
@Composable
internal fun getTargetsSummary(targets: Set<WallpaperTarget>) = targets.map {
    wallpaperTargetString(it)
}.joinToString(", ")

internal enum class FrequencyChronoUnit {
    HOURS,
    MINUTES,
}

@Composable
internal fun chronoUnitString(chronoUnit: FrequencyChronoUnit) = stringResource(
    when (chronoUnit) {
        FrequencyChronoUnit.HOURS -> R.string.hours
        FrequencyChronoUnit.MINUTES -> R.string.minutes
    },
)

@Composable
internal fun getFrequencyString(
    frequency: DateTimePeriod,
): String {
    val hours = frequency.hours
    val minutes = frequency.minutes
    val value: Int
    val unit: FrequencyChronoUnit
    if (hours != 0 && minutes == 0) {
        value = frequency.hours
        unit = FrequencyChronoUnit.HOURS
    } else {
        value = frequency.hours * 60 + frequency.minutes
        unit = FrequencyChronoUnit.MINUTES
    }
    return pluralStringResource(
        when (unit) {
            FrequencyChronoUnit.HOURS -> R.plurals.every_x_hrs
            FrequencyChronoUnit.MINUTES -> R.plurals.every_x_mins
        },
        value,
        value,
    )
}
