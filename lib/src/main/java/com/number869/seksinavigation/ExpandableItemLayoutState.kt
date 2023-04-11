package com.number869.seksinavigation

import android.service.controls.ControlsProviderService.TAG
import android.util.Log
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.CoroutineScope


data class ExpandableItemState(
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

class ExpandableItemsState(
	val coroutineScope: CoroutineScope,
) {
	// contains the item's state
	val itemsState = mutableStateMapOf<String, ExpandableItemState>()
	// contains the item's content
	private val itemsContent = mutableStateMapOf<String, @Composable () -> Unit>()
	// Define a list to keep track of the IDs of the overlays in the order they were opened
	val overlayStack = mutableStateListOf<String>()

	fun addToOverlayStack(key: Any) {
		val keyAsString = key.toString()

		if (overlayStack.contains(keyAsString)) {
			Log.d(TAG, "Something is wrong. $keyAsString is already present in overlayStack.")
		} else {
			overlayStack.add(keyAsString)
			itemsState.replace(
				keyAsString,
				itemsState[keyAsString]!!.copy(
					backGestureProgress = 0f,
					backGestureOffset = Offset.Zero,
				)
			)
			Log.d(TAG, "Added $key to overlayStack")
		}
	}

	fun closeLastOverlay() {
		// Get the ID of the most recently opened overlay
		val lastOverlayId = overlayStack.lastOrNull()

		if (lastOverlayId != null) {
			itemsState.replace(
				lastOverlayId,
				itemsState[lastOverlayId]!!.copy(
					isExpanded = false,
					isOverlaying = true
				)
			)

			// the removal happens in the ExpandableItemLayout in a
			// coroutine after the animation is done
			Log.d(TAG, "bruh closed" + lastOverlayId)
		} else {
			overlayStack.clear()
		}

		Log.d(TAG, "bruh remaining" + overlayStack.joinToString("\n"))
	}

	fun putItem(
		key: String,
		sizeOriginal: Rect,
		content: @Composable () -> Unit
	) {
		// defaults
		if (!itemsState.containsKey(key)) {
			itemsState.putIfAbsent(
				key,
				ExpandableItemState(
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

	fun setBounds(key: String, newRect: Rect) {
		itemsState.replace(key, itemsState[key]!!.copy(originalBounds = newRect))
	}

	fun setOffsetAnimationProgress(key: String, newProgress: Float) {
		itemsState.replace(
			key,
			itemsState[key]!!.copy(
				offsetAnimationProgress = newProgress
			)
		)
	}

	fun setSizeAgainstOriginalProgress(key: String, newProgress: SizeAgainstOriginalAnimationProgress) {
		itemsState.replace(
			key,
			itemsState[key]!!.copy(sizeAgainstOriginalAnimationProgress = newProgress)
		)
	}

	fun setScaleFraction(key: String, newFraction: ScaleFraction) {
		itemsState.replace(
			key,
			itemsState[key]!!.copy(scaleFraction = newFraction)
		)
	}

	@Composable
	fun getContent(key: String): @Composable() (() -> Unit) {
		return if (itemsContent[key] != null) itemsContent[key]!! else { { Text("sdfghjk") } }
	}
}

@Composable
fun rememberExpandableItemLayoutState(
	coroutineScope: CoroutineScope = rememberCoroutineScope(),
	animationSpec: AnimationSpec<Float> = spring()
) = remember {
	ExpandableItemsState(
		coroutineScope = coroutineScope,
	)
}