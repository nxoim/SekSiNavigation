package com.number869.seksinavigation

import android.annotation.SuppressLint
import android.service.controls.ControlsProviderService.TAG
import android.util.Log
import android.window.BackEvent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp


data class OverlayItemWrapperState(
	val originalBounds: Rect,
	val gestureData: GestureData,
	val scaleFraction: ScaleFraction = ScaleFraction(),
	val animationProgress: AnimationProgress = AnimationProgress(),
	val overlayParameters: OverlayParameters,
	val originalCornerRadius: Dp,
	// isBeingSwiped wouldnt update in GestureData so its here:)
	val isBeingSwiped: Boolean = false
)

data class AnimationProgress(
	// progress values will sometimes be less than 0f and that might cause
	// crashes if you use them in Color or padding animations.
	// use something like val progress = max(0f, aProgress) to prevent
	// the potential crash
	val ofOffset: Float = 0f,
	val ofSize: Float = 0f,
	val combined: Float = 0f,
)

data class GestureData(
	val progress: Float = 0f,
	val swipeEdge: Int = 0,
	val offset: Offset = Offset.Zero
)
data class ScaleFraction(
	// TODO make this work
	val byWidth: Float = 1f,
	val byHeight: Float = 1f
)

class OverlayLayoutState() {
	// contains the item's state
	private val _itemsState = mutableStateMapOf<String, OverlayItemWrapperState>()
	val itemsState get() = _itemsState
	// contains the item's content
	private val itemsContent = mutableStateMapOf<String, @Composable () -> Unit>()
	private val screensBehindItems = mutableStateMapOf<String, @Composable () -> Unit>()
	private val screensAboveItems = mutableStateMapOf<String, @Composable () -> Unit>()

	// Define a list to keep track of the IDs of the overlays in the order they were opened
	private val _overlayStack = mutableStateListOf<String>()
	val overlayStack get() = _overlayStack

	private val _listOfExpandedOverlays = mutableStateListOf<String>()
	val listOfExpandedOverlays get() = _listOfExpandedOverlays

	val emptyOverlayItemValues = OverlayItemWrapperState(
		originalBounds = Rect.Zero,
		gestureData = GestureData(
			progress = 0f,
			swipeEdge = 0,
			offset = Offset.Zero,
		),
		overlayParameters = OverlayDefaults.defaultOverlayParameters,
		originalCornerRadius = 0.dp
	)

	fun getIsExpanded(route: Any) = _listOfExpandedOverlays.contains(route.toString())
	fun getIsOverlaying(route: Any) = _overlayStack.contains(route.toString())

	fun setIsExpandedToTrue(route: Any) {
		if (!_listOfExpandedOverlays.contains(route.toString())) {
			_listOfExpandedOverlays.add(route.toString())
		}
	}

	@SuppressLint("InlinedApi")
	fun removeFromOverlayStack(route: Any) {
		if (_overlayStack.contains(route.toString())) {
			_overlayStack.remove(route.toString())
		} else {
			Log.d(TAG, "overlay stack doesnt have $route")
		}
	}

	fun addToOverlayStack(route: Any) {
		if (_overlayStack.contains(route.toString())) {
			Log.d(TAG, "Something is wrong. $route.toString() is already present in _overlayStack.")
		} else {
			_overlayStack.add(route.toString())
			_itemsState.replace(
				route.toString(),
				_itemsState[route.toString()]!!.copy(
					gestureData = GestureData(
						progress = 0f,
						offset = Offset.Zero
					)
				)
			)
			Log.d(TAG, "Added $route to _overlayStack")
		}
	}

	fun closeLastOverlay() {
		// Get the ID of the most recently opened overlay
		val lastOverlayId = _overlayStack.lastOrNull()

		if (lastOverlayId != null) {
			_listOfExpandedOverlays.removeLastOrNull()

			// the removal happens in the ExpandableItemLayout in a
			// coroutine after the animation is done
			Log.d(TAG, "bruh closed" + lastOverlayId)
			Log.d(TAG, "bruh remaining" + _overlayStack.joinToString("\n"))
		} else {
			_overlayStack.clear()
			_listOfExpandedOverlays.clear()
			Log.d(TAG, "bruh overlayStack is clear already")
		}
	}

	fun putItem(
		key: Any,
		originalSize: Rect,
		screenBehindContent: @Composable () -> Unit,
		screenAboveContent: @Composable () -> Unit,
		overlayContent: @Composable () -> Unit,
		overlayParameters: OverlayParameters = OverlayDefaults.defaultOverlayParameters,
		originalCornerRadius: Dp
	) {
		// defaults
		if (!_itemsState.containsKey(key)) {
			_itemsState.putIfAbsent(
				key.toString(),
				OverlayItemWrapperState(
					originalBounds = originalSize,
					gestureData = GestureData(
						progress = 0f,
						swipeEdge = 0,
						offset = Offset.Zero
					),
					overlayParameters = overlayParameters,
					originalCornerRadius = originalCornerRadius
				)
			)
			Log.d(TAG, "$key put into itemState")
		}

		if (!itemsContent.containsKey(key)) {
			itemsContent.putIfAbsent(
				key.toString(),
				overlayContent
			)

			screensBehindItems.putIfAbsent(
				key.toString(),
				screenBehindContent
			)

			screensAboveItems.putIfAbsent(
				key.toString(),
				screenAboveContent
			)
		}
	}

	fun setItemsBounds(key: Any, newRect: Rect) {
		_itemsState.replace(key.toString(), _itemsState[key.toString()]!!.copy(originalBounds = newRect))
	}

	@RequiresApi(34)
	fun updateGestureValues(key: Any, backEvent: BackEvent) {
		_itemsState[key.toString()] = itemsState[key.toString()]!!.copy(
			gestureData = GestureData(
				progress = backEvent.progress,
				swipeEdge = backEvent.swipeEdge,
				offset = Offset(backEvent.touchX, backEvent.touchY)
			)
		)
	}

	fun setSwipeState(forRoute: Any, isBeingSwiped: Boolean) {
		_itemsState.replace(
			forRoute.toString(),
			_itemsState[forRoute.toString()]!!.copy(
				isBeingSwiped = isBeingSwiped
			)
		)

		if (isBeingSwiped) {
			_itemsState.replace(
				forRoute.toString(),
				_itemsState[forRoute.toString()]!!.copy(
					gestureData = GestureData(
						progress = 0f,
						offset = Offset.Zero
					)
				)
			)
		}
	}

	fun setItemsOffsetAnimationProgress(key: Any, newProgress: Float) {
		_itemsState.replace(
			key.toString(),
			_itemsState[key.toString()]!!.copy(
				animationProgress = AnimationProgress(
					ofOffset = newProgress
				)
			)
		)
	}

	fun setItemsCombinedAnimationProgress(key: Any, newProgress: Float) {
		_itemsState.replace(
			key.toString(),
			_itemsState[key.toString()]!!.copy(
				animationProgress = AnimationProgress(
					combined = newProgress
				)
			)
		)
	}

	fun setItemsSizeAnimationProgress(key: Any, newProgress: Float) {
		_itemsState.replace(
			key.toString(),
			_itemsState[key.toString()]!!.copy(
				animationProgress = AnimationProgress(
					ofSize = newProgress
				)
			)
		)
	}

	fun setItemsScaleFraction(key: Any, newFraction: ScaleFraction) {
		_itemsState.replace(
			key.toString(),
			_itemsState[key.toString()]!!.copy(scaleFraction = newFraction)
		)
	}

	@Composable
	fun getScreenBehindAnItem(key: Any): @Composable() (() -> Unit) {
		return if (screensBehindItems[key.toString()] != null) screensBehindItems[key.toString()]!! else { { Box { } } }
	}

	@Composable
	fun getScreenAboveAnItem(key: Any): @Composable() (() -> Unit) {
		return if (screensAboveItems[key.toString()] != null) screensAboveItems[key.toString()]!! else { { Box { } } }
	}

	@Composable
	fun getItemsContent(key: Any): @Composable() (() -> Unit) {
		return if (itemsContent[key.toString()] != null) itemsContent[key.toString()]!! else { { Text("sdfghjk") } }
	}
}

@Composable
fun rememberOverlayLayoutState() = remember {
	OverlayLayoutState()
}