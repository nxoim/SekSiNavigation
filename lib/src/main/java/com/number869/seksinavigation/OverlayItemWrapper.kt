package com.number869.seksinavigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalDensity
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
	originalModifier: Modifier = Modifier,
	overlayParameters: OverlayParameters = OverlayDefaults.defaultOverlayParameters,
	isOriginalItemStatic: Boolean = false,
	originalCornerRadius: Dp = 0.dp,
	key: Any,
	state: OverlayLayoutState,
	screenBehindContent: @Composable () -> Unit = { Box{ } },
	screenAboveContent: @Composable () -> Unit = { Box{ } },
	content: @Composable () -> Unit
) {
	val density = LocalDensity.current.density

	val isOverlaying by remember { derivedStateOf { state.getIsOverlaying(key) } }
	var updatedBounds by remember { mutableStateOf(Rect.Zero) }

	// render the content only when item is expanded or has transitioned
	Box(
		originalModifier
			.onGloballyPositioned {
				if (isOriginalItemStatic) {
					if (!isOverlaying) updatedBounds = it.boundsInWindow()
				} else {
					updatedBounds = it.boundsInWindow()
				}
			}
			.let {
				return@let if (isOverlaying && isOriginalItemStatic) it.size(
					(updatedBounds.size.width / density).dp,
					(updatedBounds.size.height / density).dp
				) else it
			}
			.clip(RoundedCornerShape(originalCornerRadius))
			.alpha(if (isOverlaying) 0f else 1f)
	) {
		 content()
	}

	LaunchedEffect(Unit) {
		state.putItem(
			key,
			updatedBounds,
			screenBehindContent,
			screenAboveContent,
			content,
			overlayParameters,
			originalCornerRadius
		)
	}



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
	originalModifier: Modifier = Modifier,
	originalContent: @Composable () -> Unit,
	overlayContent: @Composable () -> Unit,
	screenBehindContent: @Composable () -> Unit = { Box{ } },
	screenAboveContent: @Composable () -> Unit = { Box{ } },
	overlayParameters: OverlayParameters = OverlayDefaults.defaultOverlayParameters,
	isOriginalItemStatic: Boolean = false,
	originalCornerRadius: Dp = 0.dp,
	key: Any,
	state: OverlayLayoutState
) {
	val density = LocalDensity.current.density

	val isOverlaying by remember { derivedStateOf { state.getIsOverlaying(key) } }
	var updatedBounds by remember { mutableStateOf(Rect.Zero) }

	// render the content only when item is expanded or has transitioned
	Box(
		originalModifier
			.onGloballyPositioned {
				if (isOriginalItemStatic) {
					if (!isOverlaying) updatedBounds = it.boundsInWindow()
				} else {
					updatedBounds = it.boundsInWindow()
				}
			}
			.let {
				return@let if (isOverlaying && isOriginalItemStatic) it.size(
					(updatedBounds.size.width / density).dp,
					(updatedBounds.size.height / density).dp
				) else it
			}
			.clip(RoundedCornerShape(originalCornerRadius))
			.alpha(if (isOverlaying) 0f else 1f)
	) {
		originalContent()
	}

	LaunchedEffect(Unit) {
		state.putItem(
			key,
			updatedBounds,
			screenBehindContent,
			screenAboveContent,
			overlayContent,
			overlayParameters,
			originalCornerRadius
		)
	}

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