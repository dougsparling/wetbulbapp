package dev.cyberdeck.wetbulbapp.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.cyberdeck.wetbulbapp.openmeteo.Conditions


// https://www.wbgt.env.go.jp/en/wbgt.php
// plus an entry for 35+ which is where you're pretty much gonna die
enum class Guideline(
    val color: Color,
    val range: OpenEndRange<Double>
) {
    Safe(Color(0xff218cff), Double.NEGATIVE_INFINITY.rangeUntil(21.0)),
    Caution(Color(0xffa0d2ff), 21.0.rangeUntil(25.0)),
    Warning(Color(0xfffaf500), 25.0.rangeUntil(28.0)),
    Severe(Color(0xffff9600), 28.0.rangeUntil(31.0)),
    Danger(Color(0xffff2800), 31.0.rangeUntil(35.0)),
    Death(Color(0xFF000000), 35.0.rangeUntil(Double.POSITIVE_INFINITY));

    companion object {
        fun forConditions(conditions: Conditions): Guideline {
            val webBulb = conditions.wetBulbEstimate
            return entries.find { webBulb in it.range }!!
        }
    }
}


@Composable
fun Guideline.description() = when (this) {
    Guideline.Safe -> "Generally safe at any activity level."
    Guideline.Caution -> "Prolonged periods of exercise should be accompanied by adequate replenishment of water."
    Guideline.Warning -> "Risk of heatstroke begins, rest periods should be taken every 30 minutes when performing heavy exercise."
    Guideline.Severe -> "Heavy exercise should be avoided, and both rest and water should be taken aggressively."
    Guideline.Danger -> "Approaching upper limit of survivability, avoid all exertion."
    Guideline.Death -> "Regardless of fitness levels, exposure leads to certain death within hours."
}

@Composable
fun Guideline.subheader() = when (this) {
    Guideline.Safe -> "Safe"
    Guideline.Caution -> "Caution"
    Guideline.Warning -> "Warning️"
    Guideline.Severe -> "Severe"
    Guideline.Danger -> "Danger"
    Guideline.Death -> "Death"
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