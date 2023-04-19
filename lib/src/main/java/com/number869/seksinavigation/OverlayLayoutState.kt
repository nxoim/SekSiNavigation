package com.number869.seksinavigation

import android.service.controls.ControlsProviderService.TAG
import android.util.Log
import android.window.BackEvent
import androidx.annotation.RequiresApi
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
	var isExpanded: Boolean,
	var isOverlaying: Boolean,
	val backGestureProgress: Float,
	val backGestureSwipeEdge: Int,
	val backGestureOffset: Offset,
	val offsetAnimationProgress: Float = 0f,
	val scaleFraction: ScaleFraction = ScaleFraction(),
	val sizeAgainstOriginalAnimationProgress: SizeAgainstOriginalAnimationProgress = SizeAgainstOriginalAnimationProgress(),
	val expandedSize: DpSize,
	val originalCornerRadius: Dp,
)

data class SizeAgainstOriginalAnimationProgress(
	val widthProgress: Float = 0f,
	val heightProgress: Float = 0f,
	val combinedProgress: Float = 0f
)

data class ScaleFraction(
	val byWidth: Float = 0f,
	val byHeight: Float = 0f
)

class OverlayLayoutState(overlayAnimationSpecs: OverlayAnimationSpecs) {
	// contains the item's state
	private val _itemsState = mutableStateMapOf<String, OverlayItemWrapperState>()
	val itemsState get() = _itemsState
	// contains the item's content
	private val itemsContent = mutableStateMapOf<String, @Composable () -> Unit>()
	// Define a list to keep track of the IDs of the overlays in the order they were opened
	private val _overlayStack = mutableStateListOf<String>()
	val overlayStack get() = _overlayStack

	val overlayDefaultAnimationSpecs  = overlayAnimationSpecs

	val emptyOverlayItemValues = OverlayItemWrapperState(
		originalBounds = Rect.Zero,
		isExpanded = false,
		isOverlaying = false,
		backGestureProgress = 0f,
		backGestureSwipeEdge = 0,
		backGestureOffset = Offset.Zero,
		expandedSize = DpSize.Unspecified,
		originalCornerRadius = 0.dp
	)

	fun addToOverlayStack(key: Any) {
		if (_overlayStack.contains(key.toString())) {
			Log.d(TAG, "Something is wrong. $key.toString() is already present in _overlayStack.")
		} else {
			_overlayStack.add(key.toString())
			_itemsState.replace(
				key.toString(),
				_itemsState[key.toString()]!!.copy(
					backGestureProgress = 0f,
					backGestureOffset = Offset.Zero,
				)
			)
			Log.d(TAG, "Added $key to _overlayStack")
		}
	}

	fun closeLastOverlay() {
		// Get the ID of the most recently opened overlay
		val lastOverlayId = _overlayStack.lastOrNull()

		if (lastOverlayId != null) {
			_itemsState.replace(
				lastOverlayId,
				_itemsState[lastOverlayId]!!.copy(
					isExpanded = false,
					isOverlaying = true
				)
			)

			// the removal happens in the ExpandableItemLayout in a
			// coroutine after the animation is done
			Log.d(TAG, "bruh closed" + lastOverlayId)
		} else {
			_overlayStack.clear()
		}

		Log.d(TAG, "bruh remaining" + _overlayStack.joinToString("\n"))
	}

	fun putItem(
		key: Any,
		sizeOriginal: Rect,
		content: @Composable () -> Unit,
		expandedSize: DpSize,
		originalCornerRadius: Dp
	) {
		// defaults
		if (!_itemsState.containsKey(key)) {
			_itemsState.putIfAbsent(
				key.toString(),
				OverlayItemWrapperState(
					originalBounds = sizeOriginal,
					isExpanded = false,
					isOverlaying = false,
					backGestureProgress = 0f,
					backGestureSwipeEdge = 0,
					backGestureOffset = Offset.Zero,
					expandedSize = expandedSize,
					originalCornerRadius = originalCornerRadius
				)
			)
			Log.d(TAG, "$key put into itemState")
		}

		if (!itemsContent.containsKey(key)) {
			itemsContent.putIfAbsent(
				key.toString(),
				content
			)
		}
	}

	fun setItemsBounds(key: Any, newRect: Rect) {
		_itemsState.replace(key.toString(), _itemsState[key.toString()]!!.copy(originalBounds = newRect))
	}

	fun setItemsOffsetAnimationProgress(key: Any, newProgress: Float) {
		_itemsState.replace(
			key.toString(),
			_itemsState[key.toString()]!!.copy(
				offsetAnimationProgress = newProgress
			)
		)
	}

	@RequiresApi(34)
	fun updateGestureValues(key: Any, backEvent: BackEvent) {
		_itemsState[key.toString()] = itemsState[key.toString()]!!.copy(
			backGestureProgress = backEvent.progress,
			backGestureSwipeEdge = backEvent.swipeEdge,
			backGestureOffset = Offset(backEvent.touchX, backEvent.touchY)
		)
	}

	fun setItemsSizeAgainstOriginalProgress(key: Any, newProgress: SizeAgainstOriginalAnimationProgress) {
		_itemsState.replace(
			key.toString(),
			_itemsState[key.toString()]!!.copy(sizeAgainstOriginalAnimationProgress = newProgress)
		)
	}

	fun setItemsScaleFraction(key: Any, newFraction: ScaleFraction) {
		_itemsState.replace(
			key.toString(),
			_itemsState[key.toString()]!!.copy(scaleFraction = newFraction)
		)
	}

	@Composable
	fun getItemsContent(key: Any): @Composable() (() -> Unit) {
		return if (itemsContent[key.toString()] != null) itemsContent[key.toString()]!! else { { Text("sdfghjk") } }
	}
}

@Composable
fun rememberOverlayLayoutState(
	overlayAnimationSpecs: OverlayAnimationSpecs = OverlayLayoutDefaults().overlayDefaultAnimationSpecs()
) = remember {
	OverlayLayoutState(overlayAnimationSpecs = overlayAnimationSpecs)
}