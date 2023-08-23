@file:OptIn(ExperimentalComposeUiApi::class)

package com.number869.seksinavigation

import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
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
import kotlinx.coroutines.launch

interface OverlayLayoutLookaheadAllTestScope {
	fun Modifier.animateOverlay(): Modifier

	val expandedOverlays: SnapshotStateList<String>
	val displayedOverlays: SnapshotStateList<String>
	val overlayContents: SnapshotStateMap<String, @Composable () -> Unit>
	val displayedOverlaysAsMovableContent: @Composable () -> Unit

	fun addToOverlayContents(route: String, zIndex: Float = 0f, content: @Composable () -> Unit)
	fun addToListOfDisplayedOverlays(route: String)

	@SuppressLint("ComposableNaming")
	@Composable
	fun getAnOverlaysContents(route: String): @Composable() (() -> Unit)?

	fun closeLastOverlay()
}

@Composable
fun rememberOLLTATSI(lookaheadScope: LookaheadScope): OverlayLayoutLookaheadAllTestScope {
	val displayedOverlays = remember { mutableStateListOf<String>() }
	val overlayContents: SnapshotStateMap<String, @Composable () -> Unit> = remember { mutableStateMapOf() }

	val displayedOverlaysAsMovableContent = remember {
		movableContentOf {
			displayedOverlays.forEach {
				Box(Modifier.animateOverlay(lookaheadScope)) {
					overlayContents[it]?.invoke()
				}
			}
		}
	}

	return remember {
		OverlayLayoutLookaheadAllTestScopeInstance(
			lookaheadScope,
			overlayContents,
			displayedOverlays,
			displayedOverlaysAsMovableContent
		)
	}
}


@OptIn(ExperimentalComposeUiApi::class)
class OverlayLayoutLookaheadAllTestScopeInstance(
	val lookaheadScope: LookaheadScope,
	override val overlayContents: SnapshotStateMap<String, @Composable () -> Unit>,
	override val displayedOverlays: SnapshotStateList<String>,
	override val displayedOverlaysAsMovableContent: @Composable () -> Unit
) : OverlayLayoutLookaheadAllTestScope {
	@OptIn(ExperimentalComposeUiApi::class)
	override fun Modifier.animateOverlay() = composed {
		var offsetAnimation: Animatable<IntOffset, AnimationVector2D>? by remember { mutableStateOf(null) }
		var targetOffset: IntOffset? by remember { mutableStateOf(null) }

		var sizeAnimation: Animatable<IntSize, AnimationVector2D>? by remember { mutableStateOf(null) }
		var targetSize: IntSize? by remember {
			mutableStateOf(null)
		}

		this.intermediateLayout { measurable, constraints ->
			val animatedConstraints = Constraints.fixed(
				width = (sizeAnimation?.value?.width ?: 0).coerceAtLeast(0),
				height = (sizeAnimation?.value?.height ?: 0).coerceAtLeast(0)
			)

			val placeable = measurable.measure(animatedConstraints)

			layout(placeable.width, placeable.height) {
				val coordinates = coordinates
				if (coordinates != null) {
					val offsetTargetbruh = with(lookaheadScope) {
						lookaheadScopeCoordinates
							.localLookaheadPositionOf(coordinates)
							.round().also { targetOffset = it }
					}
					val sizeTargetbruh = lookaheadSize.also { targetSize = it }

					if (offsetTargetbruh != offsetAnimation?.targetValue) {
						offsetAnimation?.run {
							launch { animateTo(offsetTargetbruh) }
						} ?: Animatable(offsetTargetbruh, IntOffset.VectorConverter).let {
							offsetAnimation = it
						}
					}

					if (sizeTargetbruh != sizeAnimation?.targetValue) {
						sizeAnimation?.run {
							launch { animateTo(sizeTargetbruh) }
						} ?: Animatable(sizeTargetbruh, IntSize.VectorConverter).let {
							sizeAnimation = it
						}
					}

					val placementOffset =
						lookaheadScopeCoordinates.localPositionOf(
							coordinates,
							Offset.Zero
						).round()

					val (x, y) = requireNotNull(offsetAnimation).run { value - placementOffset }
					placeable.place(x, y)
				} else {
					placeable.place(0, 0)
				}
			}
		}
	}

	override val expandedOverlays: SnapshotStateList<String> = mutableStateListOf()

	override fun addToOverlayContents(route: String, zIndex: Float, content: @Composable () -> Unit) {
		overlayContents.putIfAbsent(route, content)
	}

	override fun addToListOfDisplayedOverlays(route: String) {
		if (!expandedOverlays.contains(route)) expandedOverlays.add(route)
		if (!displayedOverlays.contains(route)) displayedOverlays.add(route)
	}
	@SuppressLint("ComposableNaming")
	@Composable
	override fun getAnOverlaysContents(route: String): @Composable() (() -> Unit)? = overlayContents[route]

	override fun closeLastOverlay() {
		displayedOverlays.remove(displayedOverlays.last())
	}
}

// display all of the composables here, as overlays. use ExpandableWrapper
// only to calculate the composables original/collapsed originalBounds and report
// its position on the screen. then display the overlayed one with the
// position and originalBounds from its related ExpandableWrapper until expanded.
// in case its expanded - switch to animated offset and originalBounds.
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OverlayLayoutLookaheadAllTest(
	thisOfActivity: ComponentActivity,
	nonOverlayContent: @Composable OverlayLayoutLookaheadAllTestScope.() -> Unit
) {
	LookaheadScope {
		val overlayScopeInstance = rememberOLLTATSI(this)

		overlayScopeInstance.apply {
			handleBackGestureLookahead(thisOfActivity)
			overlayScopeInstance.OverlayLayout(nonOverlayContent)
		}
	}
}

@Composable
fun OverlayLayoutLookaheadAllTestScope.OverlayLayout(
	nonOverlayContent: @Composable() OverlayLayoutLookaheadAllTestScope.() -> Unit
) {
	var screenSize by remember { mutableStateOf(IntSize.Zero) }
	val density = LocalDensity.current.density

	val firstOverlayKey by remember { derivedStateOf { displayedOverlays.firstOrNull() } }
	val lastOverlayKey by remember { derivedStateOf { displayedOverlays.lastOrNull() } }
	val isThereAtLeastOneOverlay by remember {
		derivedStateOf {
			firstOverlayKey != null
		}
	}

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
		val overlays = displayedOverlaysAsMovableContent

		nonOverlayContent()
		overlays()
	}
}

@SuppressLint("CoroutineCreationDuringComposition", "ComposableNaming")
@Composable
private fun OverlayLayoutLookaheadAllTestScope.handleBackGestureLookahead(thisOfActivity: ComponentActivity) {
	val lastOverlayKey by remember { derivedStateOf { overlayContents.keys.last() } }
	val isAnyOverlayExpanded by remember { derivedStateOf { expandedOverlays.size != 0 } }
	val meetsVersionRequirements = Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU || Build.VERSION.CODENAME == "UpsideDownCake"

	if (meetsVersionRequirements) {
		rememberCoroutineScope().launch {
			val onBackPressedCallback = @RequiresApi(34) object: OnBackPressedCallback(isAnyOverlayExpanded) {
				override fun handleOnBackPressed() {
					if (isAnyOverlayExpanded) {
						closeLastOverlay()
//						lastOverlayKey?.let { state.setSwipeState(it, false) }
					} else {
						// TODO i tried everything. it doesnt work.
//						remove()
					}
				}

				override fun handleOnBackStarted(backEvent: BackEventCompat) {
					super.handleOnBackStarted(backEvent)
//					lastOverlayKey?.let { state.setSwipeState(it, true) }
				}

				override fun handleOnBackProgressed(backEvent: BackEventCompat) {
					super.handleOnBackProgressed(backEvent)

//					if (isAnyOverlayExpanded) {
//						scope.launch {
//							val itemState = state.itemsState[lastOverlayKey]
//
//							if (itemState != null) {
//								lastOverlayKey?.let {
//									state.updateGestureValues(it, backEvent)
//								}
//							}
//						}
//					}
				}

				override fun handleOnBackCancelled() {
					super.handleOnBackCancelled()

//					if (isAnyOverlayExpanded) {
//						lastOverlayKey?.let { state.setSwipeState(it, false) }
//					}
				}
			}

			if (isAnyOverlayExpanded) {
				thisOfActivity.onBackPressedDispatcher.addCallback(onBackPressedCallback)
			} else {
				onBackPressedCallback.remove()
			}
		}
	} else {
		BackHandler(isAnyOverlayExpanded) { closeLastOverlay() }
	}
}

fun Modifier.animateOverlay(lookaheadScope: LookaheadScope) = composed {
	var offsetAnimation: Animatable<IntOffset, AnimationVector2D>? by remember { mutableStateOf(null) }
	var targetOffset: IntOffset? by remember { mutableStateOf(null) }

	var sizeAnimation: Animatable<IntSize, AnimationVector2D>? by remember { mutableStateOf(null) }
	var targetSize: IntSize? by remember {
		mutableStateOf(null)
	}

	this.intermediateLayout { measurable, constraints ->
		val animatedConstraints = Constraints.fixed(
			width = (sizeAnimation?.value?.width ?: 0).coerceAtLeast(0),
			height = (sizeAnimation?.value?.height ?: 0).coerceAtLeast(0)
		)

		val placeable = measurable.measure(animatedConstraints)

		layout(placeable.width, placeable.height) {
			val coordinates = coordinates
			if (coordinates != null) {
				val offsetTargetbruh = with(lookaheadScope) {
					lookaheadScopeCoordinates
						.localLookaheadPositionOf(coordinates)
						.round().also { targetOffset = it }
				}
				val sizeTargetbruh = lookaheadSize.also { targetSize = it }

				if (offsetTargetbruh != offsetAnimation?.targetValue) {
					offsetAnimation?.run {
						launch { animateTo(offsetTargetbruh) }
					} ?: Animatable(offsetTargetbruh, IntOffset.VectorConverter).let {
						offsetAnimation = it
					}
				}

				if (sizeTargetbruh != sizeAnimation?.targetValue) {
					sizeAnimation?.run {
						launch { animateTo(sizeTargetbruh) }
					} ?: Animatable(sizeTargetbruh, IntSize.VectorConverter).let {
						sizeAnimation = it
					}
				}

				val placementOffset =
					lookaheadScopeCoordinates.localPositionOf(
						coordinates,
						Offset.Zero
					).round()

				val (x, y) = requireNotNull(offsetAnimation).run { value - placementOffset }
				placeable.place(x, y)
			} else {
				placeable.place(0, 0)
			}
		}
	}
}