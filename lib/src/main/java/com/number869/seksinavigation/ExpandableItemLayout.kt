package com.number869.seksinavigation

import android.os.Build
import android.window.BackEvent
import android.window.OnBackAnimationCallback
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.animateIntSizeAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt


// display all of the composables here, as overlays. use ExpandableWrapper
// only to calculate the composables original/collapsed originalBounds and report
// its position on the screen. then display the overlayed one with the
// position and originalBounds from its related ExpandableWrapper until expanded.
// in case its expanded - switch to animated offset and originalBounds.
@Composable
fun ExpandableItemLayout(
	state: ExpandableItemsState,
	onBackInvokedDispatcher: OnBackInvokedDispatcher,
	nonOverlayContent: @Composable () -> Unit
) {
	val isOverlaying by remember {
		derivedStateOf {
			state.overlayStack.lastOrNull() != null
		}
	}
	var screenSize by remember { mutableStateOf(IntSize.Zero) }
	val density = LocalDensity.current.density

	val onBackPressedCallback = if (Build.VERSION.SDK_INT >= 34 || Build.VERSION.CODENAME == "UpsideDownCake") {
		@RequiresApi(34) object: OnBackAnimationCallback {
			override fun onBackInvoked() {
				state.closeLastOverlay()
			}

			override fun onBackProgressed(backEvent: BackEvent) {
				super.onBackProgressed(backEvent)
				val key = state.overlayStack.last()
				val itemState = state.itemsState[key]

				if (itemState != null) {
					state.itemsState.replace(
						key,
						itemState.copy(
							backGestureProgress = backEvent.progress,
							backGestureSwipeEdge = backEvent.swipeEdge,
							backGestureOffset = Offset(backEvent.touchX, backEvent.touchY)
						)
					)
				}
			}
		}
	} else {
		OnBackInvokedCallback { state.closeLastOverlay() }
	}

	onBackInvokedDispatcher.registerOnBackInvokedCallback(
		OnBackInvokedDispatcher.PRIORITY_OVERLAY,
		onBackPressedCallback
	)

	val baseUiScrimFraction = if (isOverlaying)
		state.itemsState[state.overlayStack.lastOrNull()]?.sizeAgainstOriginalAnimationProgress?.combinedProgress ?: 0f
	else
		0f

	val baseUiScrimColor by animateColorAsState(
		MaterialTheme.colorScheme.scrim.copy(0.2f * baseUiScrimFraction),
		label = ""
	)

	Box(
		Modifier
			.fillMaxSize()
			.onSizeChanged {
				screenSize = IntSize(
					(it.width / density).toInt(),
					(it.height / density).toInt()
				)
			}
	) {
		Box(
			Modifier
				.drawWithContent {
					drawContent()
					drawRect(baseUiScrimColor)
				}
				.graphicsLayer {
					scaleX = (1f - baseUiScrimFraction) + (0.9f * baseUiScrimFraction)
					scaleY = (1f - baseUiScrimFraction) + (0.9f * baseUiScrimFraction)
				}
		) {
			nonOverlayContent()
		}

		// display the overlayed composables with the position and size
		// from its related ExpandableWrapper until expanded
		state.overlayStack.forEach { key ->
			val itemState by remember { derivedStateOf { state.itemsState[key]!! } }
			LaunchedEffect(Unit) {
				state.itemsState.replace(
					key,
					itemState.copy(
						isExpanded = true,
						isOverlaying = true
					)
				)
			}
			// this one is for the scrim
			val isOverlayAboveOtherOverlays by remember{ derivedStateOf { state.overlayStack.lastOrNull() == key } }
			val isExpanded by remember{ derivedStateOf { itemState.isExpanded } }

			val originalSize by remember{
				derivedStateOf {
					IntSize(
						(itemState.originalBounds.size.width / density).toInt(),
						(itemState.originalBounds.size.height / density).toInt()
					)
				}
			}
			val originalOffset by remember{ derivedStateOf { itemState.originalBounds.topLeft  } }
			var overlayBounds by remember { mutableStateOf(Rect.Zero) }

			val backGestureProgress by remember{ derivedStateOf { itemState.backGestureProgress } }
			val backGestureSwipeEdge by remember{ derivedStateOf { itemState.backGestureSwipeEdge } }
			val backGestureOffset by remember{ derivedStateOf { itemState.backGestureOffset } }
			val positionAnimationSpec = if (isExpanded)
				tween<Offset>(400, 0, easing = EaseOutExpo)
			else
				spring(0.7f, 650f)

			val alignmentAnimationSpec: AnimationSpec<Float> = if (isExpanded)
				tween(400, 0, easing = EaseOutExpo)
			else
				spring( 0.7f, 650f)

			val sizeAnimationSpec = if (isExpanded)
				tween<IntSize>(400, 0, easing = EaseOutExpo)
			else
				spring(0.9f, 1050f)

			// there must be a way to calculate animation duration without
			// hardcoding a number
			val onSwipeSizeChangeExtent = 0.15f
			val onSwipeOffsetXChangeExtent = 0.1f
			val onSwipeOffsetYChangeExtent = 0.1f
			val onSwipeOffsetYPrevalence = backGestureProgress * 1f
			// the higher the number above is - the earlier the gesture will
			// fully depend the vertical swipe offset

			// interpolates from 0 to 1 over the specified duration
			// into "animationProgress"
			var animationProgress by remember { mutableStateOf(0f) }

			var isAnimating by remember { mutableStateOf(true) }

			var useGestureValues by remember { mutableStateOf(false) }

			// all these calculations to tune the animation
			val sizeExpandedWithSwipeProgress: () -> IntSize = {
				IntSize(
					(screenSize.width * (1f - backGestureProgress * onSwipeSizeChangeExtent)).toInt(),
					(screenSize.height * (1f - backGestureProgress * onSwipeSizeChangeExtent)).toInt()
				)
			}

			val offsetExpandedWithSwipeProgress: () -> Offset = {
				Offset(
					if (backGestureSwipeEdge == 0)
					// if swipe is from the left side
						((screenSize.width * onSwipeOffsetXChangeExtent) * backGestureProgress)
					else
					// if swipe is from the right side
						(-(screenSize.width * onSwipeOffsetXChangeExtent) * backGestureProgress),
					((backGestureOffset.y + (-screenSize.height * backGestureProgress * 2)) * onSwipeOffsetYChangeExtent) * onSwipeOffsetYPrevalence
				)
			}

			val animatedSize by animateIntSizeAsState(
				if (isExpanded || useGestureValues) {
					sizeExpandedWithSwipeProgress()
				} else {
					originalSize
				},
				sizeAnimationSpec,
				label = ""
			)

			val animatedOffset by animateOffsetAsState(
				if (isExpanded) offsetExpandedWithSwipeProgress() else originalOffset,
				positionAnimationSpec,
				label = ""
			)

			val animatedAlignment by animateAlignmentAsState(
				if (isExpanded) Alignment.Center else Alignment.TopStart,
				alignmentAnimationSpec
			)

			val processedOffset: () -> IntOffset = {
				if (useGestureValues) IntOffset(
					offsetExpandedWithSwipeProgress().x.roundToInt(),
					offsetExpandedWithSwipeProgress().y.roundToInt()
				) else
					IntOffset(
						animatedOffset.x.roundToInt(),
						animatedOffset.y.roundToInt()
					)
			}

			val processedSize: () -> DpSize = {
				if (useGestureValues) DpSize(
					sizeExpandedWithSwipeProgress().width.dp,
					sizeExpandedWithSwipeProgress().height.dp
				) else
					DpSize(
						animatedSize.width.dp,
						animatedSize.height.dp
					)
			}

			LaunchedEffect(animatedOffset) {
				animationProgress = -(overlayBounds.top - itemState.originalBounds.top) / (itemState.originalBounds.top - Rect.Zero.top)
				state.setOffsetAnimationProgress(
					key,
					animationProgress
				)

				if (animationProgress == 0f) {
					// when the items are in place - wait a bit and then
					// decide that the animation is done
					// because it might cross the actual position before
					// the spring animation is done
					delay(10)
					isAnimating = false

					// TODO force to smoothly transition to non overlay
					// after a certain period of time to avoid weird bouncy
					// dragging effect
				}

				if (backGestureOffset.x != 0f && isExpanded) {
					useGestureValues = true
				} else if (!isExpanded) {
					useGestureValues = false
				}

				if (!isExpanded && !isAnimating) {
					state.itemsState.replace(key, itemState.copy(isOverlaying = false))
					state.overlayStack.remove(key)
				}

				// TODO fix scale fraction
//				val widthScaleFraction = animatedSize.width / screenSize.width.toFloat()
//				val heightScaleFraction = animatedSize.height / screenSize.height.toFloat()
//
//				state.setScaleFraction(
//					key,
//					ScaleFraction(widthScaleFraction, heightScaleFraction)
//				)
			}

			// i dont remember why i thought this was needed
			LaunchedEffect(animatedSize) {
				val widthForOriginalProgressCalculation = (processedSize().width.value - originalSize.width) / (screenSize.width - originalSize.width)
				val heightForOriginalProgressCalculation = (processedSize().height.value - originalSize.height) / (screenSize.height - originalSize.height)

				state.setSizeAgainstOriginalProgress(
					key,
					SizeAgainstOriginalAnimationProgress(
						max(widthForOriginalProgressCalculation, 0f),
						max(heightForOriginalProgressCalculation, 0f),
						max((widthForOriginalProgressCalculation + heightForOriginalProgressCalculation) / 2, 0f)
					)
				)
			}

			val lastOverlayScrimFraction by remember{
				derivedStateOf {
					state.itemsState[state.overlayStack.lastOrNull()]?.sizeAgainstOriginalAnimationProgress?.combinedProgress ?: 0f
				}
			}
			val overlayScrim by animateColorAsState(
				MaterialTheme.colorScheme.scrim.copy(
					if (isOverlayAboveOtherOverlays)
						0f
					else
						0.2f * (lastOverlayScrimFraction)
				), label = ""
			)
			if (itemState.isOverlaying) {
				Box(
					Modifier
						.offset { processedOffset() }
						.size(processedSize())
						.align(animatedAlignment)
						.onGloballyPositioned { overlayBounds = it.boundsInWindow() }
						.drawWithContent {
							drawContent()
							drawRect(overlayScrim)
						}
						.graphicsLayer {
							scaleX =
								if (state.overlayStack.lastOrNull() != key) (1f - lastOverlayScrimFraction * 0.1f) else 1f
							scaleY =
								if (state.overlayStack.lastOrNull() != key) (1f - lastOverlayScrimFraction * 0.1f) else 1f
						}
				) {
					// display content
					// TODO fix color scheme default colors not being applied
					// on text and icons
					state.getContent(key)()
				}
			}
		}
	}
}