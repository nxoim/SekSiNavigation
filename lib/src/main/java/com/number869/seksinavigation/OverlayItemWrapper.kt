package com.number869.seksinavigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

// this should get cloned inside the overlay composable
// only the cloned version changes its originalBounds, position when expanded, and its alignment
// this is the overlay composable that clones the item and renders it
// with the updated originalBounds, position and alignment
// TODO handle content possibly being null
@Composable
fun OverlayItemWrapper(
	modifierForOriginal: Modifier = Modifier,
	overlaySize: DpSize = DpSize.Unspecified,
	isOriginalItemStatic: Boolean = false,
	originalCornerRadius: Dp = 0.dp,
	key: Any,
	state: OverlayLayoutState,
	content: @Composable () -> Unit
) {
	val isOverlaying by remember { derivedStateOf { state.itemsState[key.toString()]?.isOverlaying ?: false } }
	var updatedBounds by remember { mutableStateOf(Rect.Zero) }

	// render the content only when item is expanded or has transitioned
	Box(
		modifierForOriginal
			.onGloballyPositioned {
				if (isOriginalItemStatic) {
					if (!isOverlaying) updatedBounds = it.boundsInWindow()
				} else {
					updatedBounds = it.boundsInWindow()
				}
			}
			.clip(RoundedCornerShape(originalCornerRadius))
			.alpha(if (isOverlaying) 0f else 1f)
	) {
		 content()
	}

	state.putItem(
		key,
		updatedBounds,
		content,
		overlaySize,
		originalCornerRadius
	)

	// TODO fix scale fraction
//	val widthScaleFraction = state.screenSize.width.toFloat() / updatedBounds.width
//	val heightScaleFraction = state.screenSize.height.toFloat() / updatedBounds.height
//
//	if (state.itemsState[key]?.scaleFraction!!.byWidth == 0f) {
//		state.setScaleFraction(
//			key.toString(),
//			ScaleFraction(widthScaleFraction, heightScaleFraction)
//		)
//	}

	// pass the overlay originalBounds and position to the state and update the item
	LaunchedEffect(updatedBounds) {
		state.setItemsBounds(key, updatedBounds)
	}
}


@Composable
fun OverlayItemWrapper(
	modifierForOriginal: Modifier = Modifier,
	originalContent: @Composable () -> Unit,
	overlayContent: @Composable () -> Unit,
	overlaySize: DpSize = DpSize.Unspecified,
	isOriginalItemStatic: Boolean = false,
	originalCornerRadius: Dp = 0.dp,
	key: Any,
	state: OverlayLayoutState
) {
	val isOverlaying by remember { derivedStateOf { state.itemsState[key.toString()]?.isOverlaying ?: false } }
	var updatedBounds by remember { mutableStateOf(Rect.Zero) }

	// render the content only when item is expanded or has transitioned
	Box(
		modifierForOriginal
			.onGloballyPositioned {
				if (isOriginalItemStatic) {
					if (!isOverlaying) updatedBounds = it.boundsInWindow()
				} else {
					updatedBounds = it.boundsInWindow()
				}
			}
			.clip(RoundedCornerShape(originalCornerRadius))
			.alpha(if (isOverlaying) 0f else 1f)
	) {
		originalContent()
	}

	state.putItem(
		key,
		updatedBounds,
		overlayContent,
		overlaySize,
		originalCornerRadius
	)

	// TODO fix scale fraction
//	val widthScaleFraction = state.screenSize.width.toFloat() / updatedBounds.width
//	val heightScaleFraction = state.screenSize.height.toFloat() / updatedBounds.height
//
//	if (state.itemsState[key]?.scaleFraction!!.byWidth == 0f) {
//		state.setScaleFraction(
//			key.toString(),
//			ScaleFraction(widthScaleFraction, heightScaleFraction)
//		)
//	}

	// pass the overlay originalBounds and position to the state and update the item
	LaunchedEffect(updatedBounds) {
		state.setItemsBounds(key, updatedBounds)
	}
}