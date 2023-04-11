package com.number869.seksinavigation

import android.annotation.SuppressLint
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.NoInspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Density

// This is kanged from stack overflow
@SuppressLint("UnrememberedMutableState")
@Composable
fun animateAlignmentAsState(
	targetAlignment: Alignment,
	animationSpec: AnimationSpec<Float> = spring()
): State<Alignment> {
	val biased = targetAlignment as BiasAlignment
	val horizontal by animateFloatAsState(biased.horizontalBias, animationSpec)
	val vertical by animateFloatAsState(biased.verticalBias, animationSpec)
	return derivedStateOf { BiasAlignment(horizontal, vertical) }
}

private class BoxChildData(
	var alignment: Alignment,
	var matchParentSize: Boolean = false,
	inspectorInfo: InspectorInfo.() -> Unit = NoInspectorInfo
) : ParentDataModifier, InspectorValueInfo(inspectorInfo) {
	override fun Density.modifyParentData(parentData: Any?) = this@BoxChildData

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		val otherModifier = other as? com.number869.seksinavigation.BoxChildData ?: return false

		return alignment == otherModifier.alignment &&
				matchParentSize == otherModifier.matchParentSize
	}

	override fun hashCode(): Int {
		var result = alignment.hashCode()
		result = 31 * result + matchParentSize.hashCode()
		return result
	}

	override fun toString(): String =
		"BoxChildData(alignment=$alignment, matchParentSize=$matchParentSize)"
}

fun Modifier.animateAlignment(
	targetAlignment: Alignment,
	animationSpec: AnimationSpec<Float> = spring()
): Modifier = composed {
	val animatedAlignment = animateAlignmentAsState(targetAlignment, animationSpec).value
	this.then(BoxChildData(
		alignment = animatedAlignment,
		matchParentSize = false,
		inspectorInfo = debugInspectorInfo {
			name = "align"
			value = animatedAlignment
		}
	))
}


@SuppressLint("UnrememberedMutableState")
@Composable
fun animateAlignmentBiasAsState(
	targetBiasHorizontal: Float,
	targetBiasVertical: Float,
	animationSpec: AnimationSpec<Float> = spring()
): State<Alignment> {
	val biasHorizontal by animateFloatAsState(targetBiasHorizontal, animationSpec)
	val biasVertical by animateFloatAsState(targetBiasVertical, animationSpec)
	return derivedStateOf { BiasAlignment(biasHorizontal, biasVertical) }
}


@SuppressLint("UnrememberedMutableState")
@Composable
fun animateHorizontalAlignmentAsState(
	targetBiasValue: Float,
	animationSpec: AnimationSpec<Float> = spring()
): State<BiasAlignment.Horizontal> {
	val bias by animateFloatAsState(targetBiasValue, animationSpec)
	return derivedStateOf { BiasAlignment.Horizontal(bias) }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun animateVerticalAlignmentAsState(
	targetBiasValue: Float,
	animationSpec: AnimationSpec<Float> = spring()
): State<BiasAlignment.Vertical> {
	val bias by animateFloatAsState(targetBiasValue, animationSpec)
	return derivedStateOf { BiasAlignment.Vertical(bias) }
}