package dev.cyberdeck.wetbulbapp.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import dev.cyberdeck.wetbulbapp.R
import dev.cyberdeck.wetbulbapp.openmeteo.Conditions


/**
 * Guidelines on relative safety of different wet bulb temperature ranges:
 * https://www.wbgt.env.go.jp/en/wbgt.php
 *
 * Plus an entry for 35C+ which is where you're pretty much gonna die.
 */
enum class Guideline(
    val range: OpenEndRange<Double>
) {
    Safe(Double.NEGATIVE_INFINITY.rangeUntil(21.0)),
    Caution(21.0.rangeUntil(25.0)),
    Warning(25.0.rangeUntil(28.0)),
    Severe(28.0.rangeUntil(31.0)),
    Danger(31.0.rangeUntil(35.0)),
    Death(35.0.rangeUntil(Double.POSITIVE_INFINITY));

    companion object {
        fun forConditions(conditions: Conditions): Guideline {
            val webBulb = conditions.wetBulbEstimate
            return entries.find { webBulb in it.range }!!
        }
    }
}

val Guideline.color get() = when (this) {
    Guideline.Safe -> Color(0xff6ba833)
    Guideline.Caution -> Color(0xffa0d2ff)
    Guideline.Warning -> Color(0xfffaf500)
    Guideline.Severe -> Color(0xffff9600)
    Guideline.Danger -> Color(0xffff2800)
    Guideline.Death -> Color(0xFF000000)
}

@Composable
fun Guideline.description() = when (this) {
    Guideline.Safe -> stringResource(R.string.guideline_desc_safe)
    Guideline.Caution -> stringResource(R.string.guideline_desc_caution)
    Guideline.Warning -> stringResource(R.string.guideline_desc_warning)
    Guideline.Severe -> stringResource(R.string.guideline_desc_severe)
    Guideline.Danger -> stringResource(R.string.guideline_desc_danger)
    Guideline.Death -> stringResource(R.string.guideline_desc_death)
}

@Composable
fun Guideline.subHeader() = when (this) {
    Guideline.Safe -> stringResource(R.string.guideline_title_safe)
    Guideline.Caution -> stringResource(R.string.guideline_title_caution)
    Guideline.Warning -> stringResource(R.string.guideline_title_warning)
    Guideline.Severe -> stringResource(R.string.guideline_title_severe)
    Guideline.Danger -> stringResource(R.string.guideline_title_danger)
    Guideline.Death -> stringResource(R.string.guideline_title_death)
}

@Composable
fun Guideline.emoji() = when (this) {
    Guideline.Safe -> "✅"
    Guideline.Caution -> "☀️"
    Guideline.Warning -> "⚠️️"
    Guideline.Severe -> "‼️"
    Guideline.Danger -> "☠️"
    Guideline.Death -> "🪦"
}