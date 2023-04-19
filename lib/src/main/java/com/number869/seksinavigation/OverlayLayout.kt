package com.number869.seksinavigation

import android.annotation.SuppressLint
import android.os.Build
import android.window.BackEvent
import android.window.OnBackAnimationCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseInExpo
import androidx.compose.animation.core.EaseInQuad
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.EaseOutExpo
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
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
	var screenSize by remember { mutableStateOf(IntSize.Zero) }
	val density = LocalDensity.current.density

	val firstOverlayKey by remember { derivedStateOf { state.overlayStack.firstOrNull() } }
	val lastOverlayKey by remember { derivedStateOf { state.overlayStack.lastOrNull() } }
	val isOverlaying by remember {
		derivedStateOf {
			firstOverlayKey != null
		}
	}

	val firstOverlayExpansionFraction by remember{
		derivedStateOf {
			state.itemsState[firstOverlayKey]?.sizeAgainstOriginalAnimationProgress?.heightProgress ?: 0f
		}
	}

	handleBackGesture(state, thisOfActivity)

	val nonOverlayScrimColor by animateColorAsState(
		MaterialTheme.colorScheme.scrim.copy(
			if (!isOverlaying)
				0f
			else
				if(state.itemsState[firstOverlayKey]?.isExpanded == true)
					0.3f
				else
					0.3f * (firstOverlayExpansionFraction)
		),
		label = ""
	)

	val processedNonOverlayScale: () -> Float = { 1f - firstOverlayExpansionFraction * 0.1f }

	// TODO make scale fraction add up
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
					drawRect(nonOverlayScrimColor)
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

			// needed for the animations to start as soon as the
			// composable is rendered
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
			val isOverlayAboveOtherOverlays = lastOverlayKey == key

			val lastOverlayExpansionFraction = if (isOverlayAboveOtherOverlays) {
				0f
			} else {
				state.itemsState[lastOverlayKey]?.sizeAgainstOriginalAnimationProgress?.heightProgress ?: 0f
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

			val gestureProgress by remember{
				derivedStateOf {
					EaseOutCubic.transform(
						itemState.backGestureProgress
					)
				}
			}
			val gestureSwipeEdge by remember { derivedStateOf { itemState.backGestureSwipeEdge } }
			val gestureOffset by remember { derivedStateOf { itemState.backGestureOffset } }

			val positionAnimationSpec = if (isExpanded)
				tween<Offset>(600, 0, easing = EaseOutExpo)
			else
				spring(0.9f, 500f)

			val alignmentAnimationSpec: AnimationSpec<Float> = if (isExpanded)
				tween(600, 0, easing = EaseOutExpo)
			else
				spring( 0.9f, 500f)

			val sizeAnimationSpec = if (isExpanded)
				tween<IntSize>(600, 0, easing = EaseOutExpo)
			else
				spring(0.90f, 600f)


			val onSwipeScaleChangeExtent = 0.4f
			val gestureTransformEffectAmount = 0.2f

			// the higher the number above is - the earlier the gesture will
			// fully depend the vertical swipe offset

			// interpolates from 0 to 1 over the specified duration
			// into "animationProgress"
			var animationProgress by remember { mutableStateOf(0f) }
			var isAnimating by remember { mutableStateOf(true) }
			var useGestureValues by remember { mutableStateOf(false) }

			// by default is original offset and becomes a static target
			//	for the animation once isExpanded is false so that
			// distance of the swipe gesture can be calculated. this is
			// needed for the whole animation to move with the content as
			// soon as the finger moves. the alternative, simple use of
			// animate*AsState, would bring a lot of input latency
			var initialTargetOffset by remember { mutableStateOf(originalOffset) }

			// used to calculate how much the user swiped across the
			// screen in Dp
			var initialGestureOffset by remember { mutableStateOf(Offset.Zero) }

			val offsetDeviationFromTarget = if (isExpanded)
				Offset.Zero
			else
				Offset(
				(originalOffset.x - initialTargetOffset.x),
				(originalOffset.y - initialTargetOffset.y)
			)

			val gestureDistanceFromStartingPoint = Offset (
				gestureOffset.x - initialGestureOffset.x,
				gestureOffset.y - initialGestureOffset.y
			)

			val offsetExpandedWithSwipeProgress: () -> Offset = {
				Offset(
					if (gestureSwipeEdge == 0)
					// if swipe is from the left side
						((screenSize.width * gestureTransformEffectAmount) * gestureProgress)
					else
					// if swipe is from the right side
						(-(screenSize.width * gestureTransformEffectAmount) * gestureProgress),
					gestureDistanceFromStartingPoint.y * gestureTransformEffectAmount
				)
			}

			val animatedSize by animateIntSizeAsState(
				if (isExpanded) {
					if (itemState.expandedSize == DpSize.Unspecified)
						screenSize
					else IntSize(
						itemState.expandedSize.width.value.toInt(),
						itemState.expandedSize.height.value.toInt()
					)
				} else {
					originalSize
				},
				sizeAnimationSpec,
				label = ""
			)

			val animatedOffset by animateOffsetAsState(
				if (isExpanded && !useGestureValues)
					Offset.Zero
				else if (!isExpanded)
					initialTargetOffset
				else
					offsetExpandedWithSwipeProgress(),
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
							0.3f * (lastOverlayExpansionFraction)
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
						(animatedOffset.x + offsetDeviationFromTarget.x).roundToInt(),
						(animatedOffset.y + offsetDeviationFromTarget.y).roundToInt()
					)
			}

			val processedSize: () -> DpSize = {
				DpSize(
					animatedSize.width.dp,
					animatedSize.height.dp
				)
			}

			val processedScale: () -> Float = {
				// scale when another overlay is being displayed
				((1f - lastOverlayExpansionFraction * 0.1f)
				+
				// scale with gestures
				((gestureProgress / 2) * -onSwipeScaleChangeExtent)
				// scale back to normal when gestures are completed
				*
				animationProgress)
			}

			val processedCornerRadius: () -> Dp = {
				androidx.compose.ui.unit.max(
					0.dp,
					// when gestureProgress is 1f - corner radius is 36.dp.
					// when item is not expanded - gestureProgress doesn't
					// matter and corner radius is whatever the default has
					// been set in OverlayItemWrapper's parameters.
					((36.dp *  gestureProgress) * animationProgress)
							+ (itemState.originalCornerRadius * (1f - animationProgress))
				)
			}

			LaunchedEffect(isExpanded) {
				// only report this once
				if (!isExpanded) initialTargetOffset = originalOffset
			}

			// doing this because backGestureProgress is 0f
			// at launch which means this way useGestureValues
			// is false at launch
			LaunchedEffect(gestureOffset, animatedOffset) {
				useGestureValues = gestureProgress != 0f && isExpanded
			}

			// to count how much the user has swiped, not where the
			// finger is on the screen
			LaunchedEffect(gestureProgress != 0f) {
				initialGestureOffset = gestureOffset
			}

			LaunchedEffect(animatedOffset) {
				// bruh
				// calculate from the overlays actual location on screen
				// TODO needs rework
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
						// full screen scrim and then draw content
						.fillMaxSize()
						.drawWithContent {
							drawContent()
							drawRect(animatedScrim)
						}
				) {
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
							.graphicsLayer {
								scaleX = processedScale()
								scaleY = processedScale()
							}
							.clip(RoundedCornerShape(processedCornerRadius()))

					) {
						// display content
						state.getItemsContent(key)()
					}
				}
			}
		}
	}
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun handleBackGesture(state: OverlayLayoutState, thisOfActivity: ComponentActivity) {
	val firstOverlayKey by remember { derivedStateOf { state.overlayStack.firstOrNull() } }
	val lastOverlayKey by remember { derivedStateOf { state.overlayStack.lastOrNull() } }
	val isOverlaying by remember { derivedStateOf { firstOverlayKey != null } }

	val scope = rememberCoroutineScope()

	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || Build.VERSION.CODENAME == "UpsideDownCake") {
		rememberCoroutineScope().launch {
			val onBackPressedCallback = @RequiresApi(34) object: OnBackAnimationCallback {
				override fun onBackInvoked() = state.closeLastOverlay()

				override fun onBackProgressed(backEvent: BackEvent) {
					super.onBackProgressed(backEvent)

					// does running it in a coroutine even help performance
					scope.launch {
						val itemState = state.itemsState[lastOverlayKey]

						if (itemState != null) {
							lastOverlayKey?.let {
								state.updateGestureValues(it, backEvent)
							}
						}
					}
				}
			}

			// why doesnt his work TODO
			if (isOverlaying)  {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
					thisOfActivity.onBackInvokedDispatcher.registerOnBackInvokedCallback(
						OnBackInvokedDispatcher.PRIORITY_OVERLAY,
						onBackPressedCallback
					)
				}
			} else {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
					thisOfActivity.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(onBackPressedCallback)
				}
			}
		}
	} else {
		BackHandler(isOverlaying) { state.closeLastOverlay() }
	}
}