package com.dinesh.m3theme.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Preview(showBackground = true)
@Composable
internal fun ColorSelector(
    colors: List<Color> = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta),
    seedColor: Color? = Color.Blue,
    onColorSelected: (Color) -> Unit = {},
    modifier: Modifier = Modifier,
    itemSize: Dp = 50.dp,
    itemSpacing: Dp = 8.dp,
    selectionIndicatorColor: Color = Color.White,
    selectionBorderColor: Color = Color.Black,
    selectionBorderWidth: Dp = 4.dp
) {
    val distinctColors by remember(colors) { mutableStateOf(colors.distinct()) }

    LazyRow(
        modifier = modifier
//            .background(Color(0xFFFAAAAA))
            .height(itemSize + 40.dp),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            items = distinctColors,
            key = { color -> color.toArgb() }
        ) { color ->
            ColorItem(
                color = color,
                isSelected = color == seedColor,
                onSelect = { onColorSelected(color) },
                size = itemSize,
                selectionIndicatorColor = selectionIndicatorColor,
                selectionBorderColor = selectionBorderColor,
                selectionBorderWidth = selectionBorderWidth
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ColorItem(
    color: Color = Color.Green,
    isSelected: Boolean = true,
    onSelect: () -> Unit = {},
    size: Dp = 50.dp,
    selectionIndicatorColor: Color = Color.White,
    selectionBorderColor: Color = Color.Black,
    selectionBorderWidth: Dp = 4.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val transition: Transition<Boolean> = updateTransition(isSelected, label = "SelectionTransition")
    val contentColor = remember(color, selectionBorderColor, selectionIndicatorColor) {
        if (color.luminance() > 0.5f) selectionBorderColor else selectionIndicatorColor
    }

    val targetColor = remember(color, isSelected) {
        if (isSelected) color.darken(0.1f) else color
    }

    val animatedCheckIconColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "ColorAnimation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.5f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ScaleAnimation"
    )

    val selectionAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "SelectionAnimation"
    )

    val selectedSize by transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        },
        label = "SizeAnimation"
    ) { selected -> if (selected) 1.5f else 1f }

    val borderWidth by transition.animateDp(
        transitionSpec = { tween(durationMillis = 300) },
        label = "BorderAnimation"
    ) { selected -> if (selected) selectionBorderWidth else 0.dp }

    val rotation by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 300) },
        label = "RotationAnimation"
    ) { selected -> if (selected) 360f else 0f }

//    LaunchedEffect(isSelected) {
//        if (isSelected) {
//            Log.d("log_", "ColorSelectorPreview: Selected color --> ${color.toHexStringARGB()}")
//        }
//    }

    Box(
        modifier = Modifier
            .size((size.value * selectedSize).dp)
            .scale(scale)
            .graphicsLayer(rotationZ = rotation)
            .clip(CircleShape)
            .selectable(
                selected = isSelected, onClick = onSelect, role = Role.RadioButton, interactionSource = interactionSource, indication = null
            )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = animatedCheckIconColor,
            border = if (isSelected) BorderStroke(borderWidth, selectionBorderColor) else null
        ) {}
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SelectionIcon(
                size = size,
                contentColor = contentColor,
                alpha = selectionAlpha
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectionIcon(
    size: Dp = 50.dp,
    contentColor: Color = Color.Black,
    alpha: Float = 1f
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Selected",
            tint =  contentColor.copy(alpha = alpha),
            modifier = Modifier.size(size / 2)
        )
    }
}

private fun Color.darken(factor: Float): Color {
    return this.copy(
        red = red * (1 - factor),
        green = green * (1 - factor),
        blue = blue * (1 - factor),
    )
}

