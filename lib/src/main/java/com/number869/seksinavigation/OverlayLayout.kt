package com.number869.seksinavigation

import android.os.Build
import android.window.BackEvent
import android.window.OnBackAnimationCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.animateIntSizeAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt


// display all of the composables here, as overlays. use ExpandableWrapper
// only to calculate the composables original/collapsed originalBounds and report
// its position on the screen. then display the overlayed one with the
// position and originalBounds from its related ExpandableWrapper until expanded.
// in case its expanded - switch to animated offset and originalBounds.
@Composable
fun OverlayLayout(
	state: OverlayLayoutState,
	thisOfActivity: ComponentActivity,
	nonOverlayContent: @Composable () -> Unit
) {
	val lastOverlayKey by remember { derivedStateOf { state.overlayStack.lastOrNull() } }
	val isOverlaying by remember {
		derivedStateOf {
			lastOverlayKey != null
		}
	}
	val lastOverlayScrimFraction by remember{
		derivedStateOf {
			state.itemsState[lastOverlayKey]?.sizeAgainstOriginalAnimationProgress?.heightProgress ?: 0f
		}
	}

	var screenSize by remember { mutableStateOf(IntSize.Zero) }
	val density = LocalDensity.current.density

	handleBackGesture(state, thisOfActivity)

	val baseUiScrimColor by animateColorAsState(
		MaterialTheme.colorScheme.scrim.copy(
			if (!isOverlaying)
				0f
			else
				if(state.itemsState[lastOverlayKey]?.isExpanded == true)
					0.3f
				else
					0.3f * (lastOverlayScrimFraction)
		),
		label = ""
	)

	val processedNonOverlayScale: () -> Float = {
		if (!isOverlaying)
			1f
		else
			if(state.itemsState[lastOverlayKey]?.isExpanded == true && (state.itemsState[lastOverlayKey]?.backGestureProgress != 0f))
				0.9f
			else
				(1f - lastOverlayScrimFraction * 0.1f)
	}

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
					scaleX = processedNonOverlayScale()
					scaleY = processedNonOverlayScale()
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
			val isOverlayAboveOtherOverlays by remember{
				derivedStateOf { lastOverlayKey == key }
			}
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

			val backGestureProgress by remember{
				derivedStateOf {
					EaseOutQuart.transform(
						itemState.backGestureProgress
					)
				}
			}
			val backGestureSwipeEdge by remember { derivedStateOf { itemState.backGestureSwipeEdge } }
			val backGestureOffset by remember { derivedStateOf { itemState.backGestureOffset } }

			val positionAnimationSpec = if (isExpanded)
				tween<Offset>(600, 0, easing = EaseOutExpo)
			else
				spring(0.97f, 500f)

			val alignmentAnimationSpec: AnimationSpec<Float> = if (isExpanded)
				tween(600, 0, easing = EaseOutExpo)
			else
				spring( 0.97f, 500f)

			val sizeAnimationSpec = if (isExpanded)
				tween<IntSize>(600, 0, easing = EaseOutExpo)
			else
				spring(0.97f, 500f)

			// there must be a way to calculate animation duration without
			// hardcoding a number
			val onSwipeSizeChangeExtent = 0.15f
			val onSwipeOffsetXChangeExtent = 0.15f
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

			val animatedScrim by animateColorAsState(
				MaterialTheme.colorScheme.scrim.copy(
					if (isOverlayAboveOtherOverlays)
						0f
					else
						if(state.itemsState[lastOverlayKey]?.isExpanded == true)
							0.3f
						else
							0.3f * (lastOverlayScrimFraction)
				), label = ""
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

			val processedScale: () -> Float = {
				if (lastOverlayKey == key)
					1f
				else
					if(state.itemsState[lastOverlayKey]?.isExpanded == true && (state.itemsState[lastOverlayKey]?.backGestureProgress != 0f))
						0.9f
					else
						(1f - lastOverlayScrimFraction * 0.1f)
			}

			LaunchedEffect(animatedOffset) {
				// bruh
				animationProgress = ((-(overlayBounds.top - itemState.originalBounds.top) / (itemState.originalBounds.top - Rect.Zero.top)) +  -(overlayBounds.left - itemState.originalBounds.left) / (itemState.originalBounds.left - Rect.Zero.left)) / 2
				state.setItemsOffsetAnimationProgress(
					key,
					animationProgress
				)

				if (animationProgress == 0f) {
					// when the items are in place - wait a bit and then
					// decide that the animation is done
					// because it might cross the actual position before
					// the spring animation is done
					delay(50)
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

				state.setItemsSizeAgainstOriginalProgress(
					key,
					SizeAgainstOriginalAnimationProgress(
						max(widthForOriginalProgressCalculation, 0f),
						max(heightForOriginalProgressCalculation, 0f),
						max((widthForOriginalProgressCalculation + heightForOriginalProgressCalculation) / 2, 0f)
					)
				)
			}

			if (itemState.isOverlaying) {
				Box(
					Modifier
						.offset { processedOffset() }
						.size(processedSize())
						.align(animatedAlignment)
						.clickable(
							indication = null,
							interactionSource = remember { MutableInteractionSource() }
						) {
							// workaround that fixes elements being clickable
							// under the overlay
						}
						.onGloballyPositioned { overlayBounds = it.boundsInWindow() }
						.drawWithContent {
							drawContent()
							drawRect(animatedScrim)
						}
						.graphicsLayer {
							scaleX = processedScale()
							scaleY = processedScale()
						}
				) {
					// display content
					// TODO fix color scheme default colors not being applied
					// on text and icons
					state.getItemsContent(key)()
				}
			}
		}
	}
}

@Composable
fun handleBackGesture(state: OverlayLayoutState, thisOfActivity: ComponentActivity) {
	val lastOverlayKey by remember { derivedStateOf { state.overlayStack.lastOrNull() } }
	val isOverlaying by remember { derivedStateOf { lastOverlayKey != null } }

	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		rememberCoroutineScope().launch {
			val onBackPressedCallback = @RequiresApi(34) object: OnBackAnimationCallback {
				override fun onBackInvoked() = state.closeLastOverlay()

				override fun onBackProgressed(backEvent: BackEvent) {
					super.onBackProgressed(backEvent)
					val itemState = state.itemsState[lastOverlayKey]

					if (itemState != null) {
						lastOverlayKey?.let {
							state.itemsState.replace(
								it,
								itemState.copy(
									backGestureProgress = backEvent.progress,
									backGestureSwipeEdge = backEvent.swipeEdge,
									backGestureOffset = Offset(
										backEvent.touchX,
										backEvent.touchY
									)
								)
							)
						}
					}
				}
			}
			// why doesnt his work TODO
			if (isOverlaying)  {
				thisOfActivity.onBackInvokedDispatcher.registerOnBackInvokedCallback(
					OnBackInvokedDispatcher.PRIORITY_OVERLAY,
					onBackPressedCallback
				)
			} else {
				thisOfActivity.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(onBackPressedCallback)
			}
		}
	} else {
		BackHandler(isOverlaying) { state.closeLastOverlay() }
	}
}