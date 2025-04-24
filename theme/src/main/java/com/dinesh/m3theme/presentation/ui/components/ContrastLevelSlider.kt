package com.dinesh.m3theme.presentation.ui.components

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale

@Preview(showBackground = true)
@Composable
internal fun ContrastLevelSlider(
    @FloatRange(from = 0.0, to = 1.0) contrastLevel: Double = 0.4,
    onContrastLevelChanged: (Double) -> Unit = {},
//    onContrastLevelChangeFinished: () -> Unit = {},
) {
//    var localContrastLevel by remember(themeState.contrastLevel) {
//        mutableDoubleStateOf(themeState.contrastLevel)
//    }
    Column {
        ThemeHeaderLabel(title = "Contrast Level")

        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = contrastLevel.toFloat(),
                onValueChange = { onContrastLevelChanged(it.toDouble()) },
//                onValueChangeFinished = { onContrastLevelChangeFinished() },
                valueRange = 0f..1f,
                steps = 100,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .padding(horizontal = 0.dp)
                    .weight(1f)
            )
            Text(
                text = String.format(Locale.getDefault(), "%.2f", contrastLevel),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}