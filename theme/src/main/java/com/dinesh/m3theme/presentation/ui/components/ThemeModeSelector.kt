package com.dinesh.m3theme.presentation.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dinesh.m3theme.model.PaletteStyle
import com.dinesh.m3theme.model.ThemeMode


@Preview(showBackground = true)
@Composable
internal fun ThemeModeSelector(
    modifier: Modifier = Modifier,
    selectedMode: ThemeMode = ThemeMode.System,
    onModeSelected: (ThemeMode) -> Unit = {},
) {
    EnumSelector(
        title = "Theme Mode",
        options = ThemeMode.entries.toTypedArray(),
        selectedOption = selectedMode,
        onOptionSelected = onModeSelected,
        getOptionLabel = { mode ->
            when (mode) {
                ThemeMode.Light -> "Light"
                ThemeMode.Dark -> "Dark"
                ThemeMode.System -> "System Default"
            }
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
internal fun <T: Enum<T>> EnumSelector(
    title: String = "Preview PaletteStyle",
    options: Array<T> = PaletteStyle.entries.toTypedArray() as Array<T>,
    selectedOption: T = PaletteStyle.TonalSpot as T,
    onOptionSelected: (T) -> Unit = {},
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
    titleFontWeight: FontWeight = FontWeight.Bold,
    titleBottomPadding: Dp = 0.dp,
    itemMinWidth: Dp = 50.dp,
    verticalPadding: Dp = 0.dp,
    getOptionLabel: (T) -> String = { it.name },
) {
    Column(modifier = modifier) {
        ThemeHeaderLabel(
            title = title,
            style = titleStyle,
            fontWeight = titleFontWeight,
            bottomPadding = titleBottomPadding
        )

        EnumSelectorOptions(
            options = options,
            selectedOption = selectedOption,
            onOptionSelected = onOptionSelected,
            getOptionLabel = getOptionLabel,
            itemMinWidth = itemMinWidth,
            verticalPadding = verticalPadding
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun ThemeHeaderLabel(
    title: String = "Sample Title",
    style: TextStyle = MaterialTheme.typography.titleMedium,
    fontWeight: FontWeight = FontWeight.Bold,
    bottomPadding: Dp = 8.dp
) {
    Text(
        text = title,
        style = style,
        fontWeight = fontWeight,
        modifier = Modifier.padding(bottom = bottomPadding)
    )
}




@Preview(showBackground = true)
@Composable
internal fun <T: Enum<T>> EnumSelectorOptions(
    options: Array<T> = PaletteStyle.entries.toTypedArray() as Array<T>,
    selectedOption: T = PaletteStyle.TonalSpot as T,
    onOptionSelected: (T) -> Unit = {},
    getOptionLabel: (T) -> String = { it.name },
    itemMinWidth: Dp = 50.dp,
    verticalPadding: Dp = 4.dp
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .horizontalScroll(scrollState)
            .padding(bottom = verticalPadding)
    ) {
        options.forEach { option ->
            EnumSelectorChip(
                option = option,
                isSelected = option == selectedOption,
                onOptionSelected = onOptionSelected,
                getOptionLabel = getOptionLabel,
                itemMinWidth = itemMinWidth
            )
        }
    }
    /*

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { style ->
                FilterChip(
                    selected = style == selectedOption,
                    onClick = { onOptionSelected(style) },
                    label = {
                        Text(
                            text = style.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .widthIn(min = 50.dp)
                )
            }
        }

     */
}


@Preview(showBackground = true)
@Composable
private fun <T: Enum<T>> EnumSelectorChip(
    option: T = PaletteStyle.TonalSpot as T,
    isSelected: Boolean = true,
    onOptionSelected: (T) -> Unit = {},
    getOptionLabel: (T) -> String = { it.name },
    itemMinWidth: Dp = 50.dp
) {
    FilterChip(
        selected = isSelected,
        onClick = { onOptionSelected(option) },
        label = {
            Text(
                text = getOptionLabel(option),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = Modifier
            .padding(end = 8.dp)
            .widthIn(min = itemMinWidth)
    )
}
