package com.number869.seksinavigation

import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.EaseInCirc
import androidx.compose.animation.core.EaseInExpo
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.intermediateLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.roundToIntRect
import com.number869.seksinavigation.OverlayAnimationBehaviors.ContainerMorph
import com.number869.seksinavigation.OverlayAnimationBehaviors.LookaheadFullscreenLayoutTest
import kotlinx.coroutines.launch
import java.lang.Float.max
import kotlin.math.min
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
	val isThereAtLeastOneOverlay by remember {
		derivedStateOf {
			firstOverlayKey != null
		}
	}

	val firstOverlayExpansionFraction by remember{
		derivedStateOf {
			state.itemsState[firstOverlayKey]?.animationProgress?.combined ?: 0f
		}
	}

	handleBackGesture(state, thisOfActivity)

	val nonOverlayScrimColor by animateColorAsState(
		MaterialTheme.colorScheme.scrim.copy(
			if (!isThereAtLeastOneOverlay)
				0f
			else
				if(state.listOfExpandedOverlays.contains(firstOverlayKey))
					0.3f
				else
					0.3f * max(0f, firstOverlayExpansionFraction)
		),
		label = ""
	)

	val processedNonOverlayScale by remember(firstOverlayExpansionFraction) {
		derivedStateOf {
			1f - firstOverlayExpansionFraction * 0.05f
		}
	}

	// TODO make scale fraction add up
	Box(
		Modifier
			.fillMaxSize()
			.onSizeChanged {
				if (screenSize == IntSize.Zero) screenSize = IntSize(
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
					scaleX = processedNonOverlayScale
					scaleY = processedNonOverlayScale
				}
		) {
			nonOverlayContent()
		}

		// display the overlayed composables with the position and size
		// from its related ExpandableWrapper until expanded
		state.overlayStack.forEach { overlayKey ->
			val itemState by remember { derivedStateOf { state.itemsState[overlayKey]!! } }

			when (itemState.overlayParameters.animationBehaviorType) {
				ContainerMorph -> ContainerMorphOverlay(
					state = state,
					screenSize = screenSize,
					overlayKey = overlayKey
				)

				LookaheadFullscreenLayoutTest -> LookaheadFullscreenLayoutTestLayout(
					state = state,
					screenSize = screenSize,
					overlayKey = overlayKey
				)
			}
		}
	}
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LookaheadFullscreenLayoutTestLayout(
	state: OverlayLayoutState,
	screenSize: IntSize,
	overlayKey: String
) {
	val lastOverlayKey by remember { derivedStateOf { state.overlayStack.lastOrNull() } }
	val density = LocalDensity.current.density
	val itemState by remember { derivedStateOf { state.itemsState[overlayKey]!! } }
	val expanded by remember { derivedStateOf { state.getIsExpanded(overlayKey) }}

	val gestureProgress by remember {
		derivedStateOf {
			EaseOutCubic.transform(
				itemState.gestureData.progress
			)
		}
	}
	var initialGestureOffset by remember { mutableStateOf(Offset.Zero) }
	val gestureSwipeEdge by remember { derivedStateOf { itemState.gestureData.swipeEdge } }
	val gestureOffset by remember { derivedStateOf { itemState.gestureData.offset } }
	val gestureDistanceFromStartingPoint by remember {
		derivedStateOf {
			Offset (
				gestureOffset.x - initialGestureOffset.x,
				gestureOffset.y - initialGestureOffset.y
			)
		}
	}

	// to count how much the user has swiped, not where the
	// finger is on the screen
	LaunchedEffect(itemState.isBeingSwiped) {
		initialGestureOffset = gestureOffset
	}

	val swipeOffsetDeviation by remember {
		derivedStateOf {
			IntOffset(
				x = if (gestureSwipeEdge == 0)
					// if swipe is from the left side
					((((screenSize.width * 0.05f) - 8) * gestureProgress) * density).roundToInt()
				else
					// if swipe is from the right side
					((-((screenSize.width * 0.05f) - 8) * gestureProgress) * density).roundToInt(),
				y = ((((gestureDistanceFromStartingPoint.y * 0.05f) * density) * min(gestureProgress * 10f, 1f))).roundToInt()
				// we use min(progress * 10f, 1f) above to mask
				// offset not being set properly in the first milliseconds lmao
			)
		}
	}

	var offsetCollapseAnimationFinished by remember { mutableStateOf(false) }
	var sizeCollapseAnimationFinished by remember { mutableStateOf(false) }
	val animationIsFinished = offsetCollapseAnimationFinished && sizeCollapseAnimationFinished

	val originalOffset by remember { derivedStateOf { itemState.originalBounds.topLeft.round() } }
	var initialTargetOffset by remember { mutableStateOf(originalOffset) }
	val targetOffset by remember {
		derivedStateOf {
			if (expanded) IntOffset(0, 0) else initialTargetOffset
		}
	}
	val offsetDeviationFromTarget by remember {
		derivedStateOf {
			if (expanded)
				IntOffset.Zero
			else
				IntOffset(
					(itemState.originalBounds.topLeft.x - initialTargetOffset.x).roundToInt(),
					(itemState.originalBounds.topLeft.y - initialTargetOffset.y).roundToInt()
				)
		}
	}
	val totalOffsetDeviation by remember {
		derivedStateOf {
			offsetDeviationFromTarget + swipeOffsetDeviation
		}
	}

	val originalSize by remember {
		derivedStateOf {
			itemState.originalBounds.roundToIntRect().size
		}
	}
	val maxSize = if (itemState.overlayParameters.size == DpSize.Unspecified)
		IntSize(
			(screenSize.width * density).roundToInt(),
			(screenSize.height * density).roundToInt()
		)
	else
		IntSize(
			(itemState.overlayParameters.size.width.value * density).toInt() ,
			(itemState.overlayParameters.size.height.value * density).toInt()
		)

	val targetSize by remember {
		derivedStateOf { if (expanded) maxSize else originalSize }
	}

	val isOverlayAboveOtherOverlays by remember { derivedStateOf { lastOverlayKey == overlayKey } }

	val lastOverlayExpansionFraction by remember {
		derivedStateOf {
			if (isOverlayAboveOtherOverlays) {
				0f
			} else {
				state.itemsState[lastOverlayKey]?.animationProgress?.combined ?: 0f
			}
		}
	}

	LookaheadScope {
		val movableContent = remember {
			movableContentOf {
				var sizeAnimation: Animatable<IntSize, AnimationVector2D>? by remember { mutableStateOf(null) }
				var offsetAnimation: Animatable<IntOffset, AnimationVector2D>? by remember { mutableStateOf(null) }

				LaunchedEffect(Unit, expanded) {
					initialTargetOffset = originalOffset
				}

				Box(
					Modifier.intermediateLayout { measurable, constraints ->
						val animatedConstraints = Constraints.fixed(
							width = (sizeAnimation?.value?.width ?: originalSize.width).coerceAtLeast(0),
							height = (sizeAnimation?.value?.height ?: originalSize.height).coerceAtLeast(0)
						)

						val placeable = measurable.measure(animatedConstraints)

						val expansionProgress = {
							(((((sizeAnimation?.value?.width ?: 0) - originalSize.width) / (maxSize.width - originalSize.width)) + (((sizeAnimation?.value?.height ?: 0) - originalSize.height) / (maxSize.height - originalSize.height))) / 2)
						}

						val animatedScale = {
							(
								// scale when another overlay is being displayed
								(1f - lastOverlayExpansionFraction * 0.05f)
								+
								// scale with gestures
								((gestureProgress) * - 0.1f)
								// scale back to normal when gestures are completed
								*
								expansionProgress()
							)
						}

						layout(placeable.width, placeable.height) {
							if (targetOffset != offsetAnimation?.targetValue) {
								offsetAnimation?.run {
									launch { animateTo(targetOffset, tween(6000)) }
								} ?: Animatable(
									targetOffset,
									IntOffset.VectorConverter
								).let { offsetAnimation = it }
							}

							if (targetSize != sizeAnimation?.targetValue) {
								sizeAnimation?.run {
									launch {
										animateTo(targetSize, tween(6000))
										if (!expanded) state.removeFromOverlayStack(overlayKey)
									}
								} ?: Animatable(
									targetSize,
									IntSize.VectorConverter
								).let { sizeAnimation = it }
							}

							if (coordinates != null) {
								val placementOffset = lookaheadScopeCoordinates.localPositionOf(
									coordinates!!,
									Offset.Zero
								).round()

								val (x, y) = (offsetAnimation?.value ?: targetOffset) + (
									offsetDeviationFromTarget + IntOffset(
										x = (swipeOffsetDeviation.x * expansionProgress()),
										y = (swipeOffsetDeviation.y * expansionProgress())
									)
								) - placementOffset

								placeable.placeWithLayer(x, y) {
									scaleX = animatedScale()
									scaleY = animatedScale()
								}
							} else {
								placeable.place(0, 0)
							}
						}
					}
				) {
					state.getItemsContent(overlayKey)()
				}
			}
		}


		DisposableEffect(Unit) {
			onDispose {
				state.removeFromOverlayStack(overlayKey)
			}
		}

		movableContent()

		LaunchedEffect(Unit) {
			state.setIsExpandedToTrue(overlayKey)
		}
	}
}

@Composable
private fun ContainerMorphOverlay(
	state: OverlayLayoutState,
	screenSize: IntSize,
	overlayKey: String
) {
	val lastOverlayKey by remember { derivedStateOf { state.overlayStack.lastOrNull() } }
	val density = LocalDensity.current.density

	Box {
		val itemState by remember { derivedStateOf { state.itemsState[overlayKey]!! } }
		val animationSpecs = itemState.overlayParameters.animationSpecs.containerMorphAnimationSpecs

		val maxSize by remember {
			derivedStateOf {
				if (itemState.overlayParameters.size == DpSize.Unspecified)
					screenSize
				else
					IntSize(
						itemState.overlayParameters.size.width.value.toInt(),
						itemState.overlayParameters.size.height.value.toInt()
					)
			}
		}

		// this one is for the scrim
		val isOverlayAboveOtherOverlays by remember { derivedStateOf { lastOverlayKey == overlayKey } }

		val lastOverlayExpansionFraction by remember {
			derivedStateOf {
				if (isOverlayAboveOtherOverlays) {
					0f
				} else {
					state.itemsState[lastOverlayKey]?.animationProgress?.combined ?: 0f
				}
			}
		}

		val isExpanded by remember { derivedStateOf { state.getIsExpanded(overlayKey) } }
		var originalSize by remember {
			mutableStateOf(
				IntSize(
					(itemState.originalBounds.size.width / density).toInt(),
					(itemState.originalBounds.size.height / density).toInt()
				)
			)
		}

		val originalOffset by remember { derivedStateOf { itemState.originalBounds.topLeft  } }

		val gestureProgress by remember {
			derivedStateOf {
				EaseOutCubic.transform(
					itemState.gestureData.progress
				)
			}
		}

		val gestureSwipeEdge by remember { derivedStateOf { itemState.gestureData.swipeEdge } }
		val gestureOffset by remember { derivedStateOf { itemState.gestureData.offset } }

		val positionAnimationSpec by remember {
			derivedStateOf {
				if (isExpanded)
					animationSpecs.offsetToExpanded
				else
					animationSpecs.offsetToCollapsed
			}
		}

		val sizeAnimationSpec by remember {
			derivedStateOf {
				if (isExpanded)
					animationSpecs.sizeToExpanded
				else
					animationSpecs.sizeToCollapsed
			}
		}

		val offsetAnimationProgress by animateFloatAsState(
			if (isExpanded) 1f else 0f,
			animationSpec = if (isExpanded) spring(
				dampingRatio = positionAnimationSpec.dampingRatio,
				stiffness = positionAnimationSpec.stiffness
			) else spring(
				positionAnimationSpec.dampingRatio,
				positionAnimationSpec.stiffness
			),
			label = ""
		)
		val sizeAnimationProgress by animateFloatAsState(
			if (isExpanded) 1f else 0f,
			animationSpec = if (isExpanded) SpringSpec(
				dampingRatio = sizeAnimationSpec.dampingRatio,
				stiffness = sizeAnimationSpec.stiffness,
			) else SpringSpec(
				dampingRatio = sizeAnimationSpec.dampingRatio,
				stiffness = sizeAnimationSpec.stiffness,
			),
			label = ""
		)

		val animationProgress by remember {
			derivedStateOf {
				(offsetAnimationProgress + sizeAnimationProgress) * 0.5f
			}
		}
		val useGestureValues by remember { derivedStateOf {itemState.isBeingSwiped} }

		var isOffsetCollapseAnimationDone by remember { mutableStateOf(false) }
		var isSizeCollapseAnimationDone by remember { mutableStateOf(false) }

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

		val offsetDeviationFromTarget by remember {
			derivedStateOf {
				if (isExpanded)
					Offset.Zero
				else
					Offset(
						(itemState.originalBounds.topLeft.x - initialTargetOffset.x),
						(itemState.originalBounds.topLeft.y - initialTargetOffset.y)
					)
			}
		}

		val gestureDistanceFromStartingPoint by remember {
			derivedStateOf {
				Offset (
					gestureOffset.x - initialGestureOffset.x,
					gestureOffset.y - initialGestureOffset.y
				)
			}
		}

		val animatedSize by animateIntSizeAsState(
			if (isExpanded) maxSize else originalSize,
			sizeAnimationSpec,
			label = "",
			finishedListener = { if (!isExpanded) isSizeCollapseAnimationDone = true }
		)

		val targetOffset by remember {
			derivedStateOf {
				if (itemState.overlayParameters.targetOffset == Offset.Unspecified) {
					if (animationSpecs.bounceThroughTheCenter)
						Offset(
							((screenSize.width - animatedSize.width) * 0.5f) * density,
							((screenSize.height - animatedSize.height) * 0.5f) * density
						)
					else
						Offset(
							((screenSize.width - maxSize.width) * 0.5f) * density,
							((screenSize.height - maxSize.height) * 0.5f) * density
						)
				} else {
					itemState.overlayParameters.targetOffset
				}
			}
		}

		val offsetExpandedWithSwipeProgress by remember {
			derivedStateOf {
				Offset(
					x = if (gestureSwipeEdge == 0)
					// if swipe is from the left side
						targetOffset.x + (((screenSize.width * 0.05f) - 8) * gestureProgress)* density
					else
					// if swipe is from the right side
						targetOffset.x + (-((screenSize.width * 0.05f) - 8) * gestureProgress) * density,
					y = (targetOffset.y + ((gestureDistanceFromStartingPoint.y * 0.05f) * density)  * min(gestureProgress * 10f, 1f))
					// we use min(progress * 10f, 1f) above to mask
					// offset not being set properly in the first milliseconds lmao
				)
			}
		}

		val animatedOffset by animateOffsetAsState(
			if (isExpanded)
				offsetExpandedWithSwipeProgress
			else
				initialTargetOffset,
			positionAnimationSpec,
			label = "",
			finishedListener = { if (!isExpanded) isOffsetCollapseAnimationDone = true }
		)

		val animatedScrim by animateColorAsState(
			MaterialTheme.colorScheme.scrim.copy(
				if (isExpanded && isOverlayAboveOtherOverlays)
					0f
				else
					0.3f * max(0f, lastOverlayExpansionFraction)
			), label = ""
		)

		val processedSize by remember {
			derivedStateOf {
				DpSize(
					animatedSize.width.dp,
					animatedSize.height.dp
				)
			}
		}

		val processedOffset by remember {
			derivedStateOf {
				if (useGestureValues)
					IntOffset(
						offsetExpandedWithSwipeProgress.x.roundToInt(),
						offsetExpandedWithSwipeProgress.y.roundToInt()
					)
				else
					IntOffset(
						(animatedOffset.x + offsetDeviationFromTarget.x).roundToInt(),
						(animatedOffset.y + offsetDeviationFromTarget.y).roundToInt()
					)
			}
		}


		val processedScale by remember {
			derivedStateOf {
				(
						// scale when another overlay is being displayed
						(1f - lastOverlayExpansionFraction * 0.05f)
								+
								// scale with gestures
								((gestureProgress) * -0.1f)
								// scale back to normal when gestures are completed
								*
								sizeAnimationProgress
						)
			}
		}

		val processedCornerRadius by remember {
			derivedStateOf {
				androidx.compose.ui.unit.max(
					0.dp,
					// when progress is 1f - corner radius is 36.dp.
					// when item is not expanded - progress doesn't
					// matter and corner radius is whatever the default has
					// been set in OverlayItemWrapper's parameters.
					((32.dp *  gestureProgress) * animationProgress)
							+ (itemState.originalCornerRadius * (1f - animationProgress))
				)
			}
		}

		LaunchedEffect(isExpanded) {
			// only report this once
			if (!isExpanded) initialTargetOffset = originalOffset
		}

		// to count how much the user has swiped, not where the
		// finger is on the screen
		LaunchedEffect(useGestureValues) {
			initialGestureOffset = gestureOffset
		}

		// needed for the animations to start as soon as the
		// composable is rendered
		LaunchedEffect(Unit) {
			// remember the offset and size before the animation
			initialTargetOffset = itemState.originalBounds.topLeft
			originalSize = IntSize(
				(itemState.originalBounds.size.width / density).toInt(),
				(itemState.originalBounds.size.height / density).toInt()
			)

			state.setIsExpandedToTrue(overlayKey)
		}

		LaunchedEffect(isOffsetCollapseAnimationDone, isSizeCollapseAnimationDone) {
			if (isOffsetCollapseAnimationDone && isSizeCollapseAnimationDone) {
				// TODO handle composables flying across the screen
				// when back is invoked quickly and many times
				state.removeFromOverlayStack(overlayKey)
			}
		}

		// i dont remember why i thought this was needed
		LaunchedEffect(animationProgress) {
			state.setItemsOffsetAnimationProgress(
				overlayKey,
				offsetAnimationProgress
			)

			state.setItemsSizeAnimationProgress(
				overlayKey,
				sizeAnimationProgress
			)

			state.setItemsCombinedAnimationProgress(
				overlayKey,
				animationProgress
			)

			// TODO fix scale fraction
//				val widthScaleFraction = animatedSize.width / screenSize.width.toFloat()
//				val heightScaleFraction = animatedSize.height / screenSize.height.toFloat()
//
//				state.setScaleFraction(
//					key,
//					ScaleFraction(widthScaleFraction, heightScaleFraction)
//				)
		}

		Box(
			Modifier
				// full screen scrim and then draw content
				.fillMaxSize()
				.drawWithContent {
					drawContent()
					drawRect(animatedScrim)
				}
		) {
			val aboveAndBehindAlpha by remember {
				derivedStateOf {
					if (isExpanded)
						EaseInCirc.transform(sizeAnimationProgress)
					else
						EaseInExpo.transform(sizeAnimationProgress)
				}
			}

			Box(Modifier.alpha(aboveAndBehindAlpha)) {
				state.getScreenBehindAnItem(overlayKey)()
			}

			Box(
				Modifier
					.offset { processedOffset }
					.size(processedSize)
					.clickable(
						indication = null,
						interactionSource = remember { MutableInteractionSource() }
					) {
						// workaround that fixes elements being clickable
						// under the overlay
					}
					.graphicsLayer {
						scaleX = processedScale
						scaleY = processedScale
					}
					.clip(RoundedCornerShape(processedCornerRadius))
			) {
				// display content
				state.getItemsContent(overlayKey)()
			}

			Box(Modifier.alpha(aboveAndBehindAlpha)) {
				state.getScreenAboveAnItem(overlayKey)()
			}
		}
	}
}

@SuppressLint("CoroutineCreationDuringComposition", "ComposableNaming")
@Composable
private fun handleBackGesture(state: OverlayLayoutState, thisOfActivity: ComponentActivity) {
	val lastOverlayKey by remember { derivedStateOf { state.overlayStack.lastOrNull() } }
	val isAnyOverlayExpanded by remember { derivedStateOf { state.listOfExpandedOverlays.size != 0 } }
	val meetsVersionRequirements = Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU || Build.VERSION.CODENAME == "UpsideDownCake"

	val scope = rememberCoroutineScope()

	if (meetsVersionRequirements) {
		rememberCoroutineScope().launch {
			val onBackPressedCallback = @RequiresApi(34) object: OnBackPressedCallback(isAnyOverlayExpanded) {
				override fun handleOnBackPressed() {
					if (isAnyOverlayExpanded) {
						state.closeLastOverlay()
						lastOverlayKey?.let { state.setSwipeState(it, false) }
					} else {
						// TODO i tried everything. it doesnt work.
						remove()
					}
				}

				override fun handleOnBackStarted(backEvent: BackEventCompat) {
					super.handleOnBackStarted(backEvent)
					lastOverlayKey?.let { state.setSwipeState(it, true) }
				}

				override fun handleOnBackProgressed(backEvent: BackEventCompat) {
					super.handleOnBackProgressed(backEvent)

					if (isAnyOverlayExpanded) {
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

				override fun handleOnBackCancelled() {
					super.handleOnBackCancelled()

					if (isAnyOverlayExpanded) {
						lastOverlayKey?.let { state.setSwipeState(it, false) }
					}
				}
			}

			if (isAnyOverlayExpanded) {
				thisOfActivity.onBackPressedDispatcher.addCallback(onBackPressedCallback)
			} else {
				onBackPressedCallback.remove()
			}
		}
	} else {
		BackHandler(isAnyOverlayExpanded) { state.closeLastOverlay() }
	}
}

