package com.number869.seksinavigation

import android.service.controls.ControlsProviderService.TAG
import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect


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

class OverlayLayoutState() {
	// contains the item's state
	private val _itemsState = mutableStateMapOf<String, OverlayItemWrapperState>()
	val itemsState get() = _itemsState
	// contains the item's content
	private val itemsContent = mutableStateMapOf<String, @Composable () -> Unit>()
	// Define a list to keep track of the IDs of the overlays in the order they were opened
	private val _overlayStack = mutableStateListOf<String>()
	val overlayStack get() = _overlayStack

	fun addToOverlayStack(key: Any) {
		val keyAsString = key.toString()

		if (_overlayStack.contains(keyAsString)) {
			Log.d(TAG, "Something is wrong. $keyAsString is already present in _overlayStack.")
		} else {
			_overlayStack.add(keyAsString)
			_itemsState.replace(
				keyAsString,
				_itemsState[keyAsString]!!.copy(
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
		key: String,
		sizeOriginal: Rect,
		content: @Composable () -> Unit
	) {
		// defaults
		if (!_itemsState.containsKey(key)) {
			_itemsState.putIfAbsent(
				key,
				OverlayItemWrapperState(
					originalBounds = sizeOriginal,
					isExpanded = false,
					isOverlaying = false,
					backGestureProgress = 0f,
					backGestureSwipeEdge = 0,
					backGestureOffset = Offset.Zero
				)
			)
			Log.d(TAG, "$key put into itemState")
		}

		if (!itemsContent.containsKey(key)) {
			itemsContent.putIfAbsent(
				key,
				content
			)
		}
	}

	fun setItemsBounds(key: String, newRect: Rect) {
		_itemsState.replace(key, _itemsState[key]!!.copy(originalBounds = newRect))
	}

	fun setItemsOffsetAnimationProgress(key: String, newProgress: Float) {
		_itemsState.replace(
			key,
			_itemsState[key]!!.copy(
				offsetAnimationProgress = newProgress
			)
		)
	}

	fun setItemsSizeAgainstOriginalProgress(key: String, newProgress: SizeAgainstOriginalAnimationProgress) {
		_itemsState.replace(
			key,
			_itemsState[key]!!.copy(sizeAgainstOriginalAnimationProgress = newProgress)
		)
	}

	fun setItemsScaleFraction(key: String, newFraction: ScaleFraction) {
		_itemsState.replace(
			key,
			_itemsState[key]!!.copy(scaleFraction = newFraction)
		)
	}

	@Composable
	fun getItemsContent(key: String): @Composable() (() -> Unit) {
		return if (itemsContent[key] != null) itemsContent[key]!! else { { Text("sdfghjk") } }
	}
}

@Composable
fun rememberOverlayLayoutState() = remember {
	OverlayLayoutState()
}