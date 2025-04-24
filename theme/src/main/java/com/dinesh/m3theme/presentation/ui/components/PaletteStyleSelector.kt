package com.dinesh.m3theme.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dinesh.m3theme.model.PaletteStyle


@Preview(showBackground = true)
@Composable
internal fun PaletteStyleSelector(
    selectedStyle: PaletteStyle = PaletteStyle.TonalSpot,
    onStyleSelected: (PaletteStyle) -> Unit = {},
    modifier: Modifier = Modifier
) {
    EnumSelector(
        title = "Palette Style",
        options = PaletteStyle.entries.toTypedArray(),
        selectedOption = selectedStyle,
        onOptionSelected = onStyleSelected,
        modifier = modifier
    )

    /*


    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PaletteStyle.entries.forEach { style ->
            FilterChip(
                selected = style == selectedStyle,
                onClick = { onStyleSelected(style) },
                label = {
                    Text(
                        text = style.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier
                    .padding(end = 4.dp)
                    .widthIn(min = 5.dp)
            )
        }
    }

     */
}