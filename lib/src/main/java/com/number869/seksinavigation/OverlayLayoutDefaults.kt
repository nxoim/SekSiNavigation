 package com.number869.seksinavigation

import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

data class OverlayAnimationSpecs(
	val positionToExpandedAnimationSpec: TweenSpec<Offset>,
	val positionToCollapsedAnimationSpec: SpringSpec<Offset>,
	val alignmentToExpandedAnimationSpec: TweenSpec<Float>,
	val alignmentToCollapsedAnimationSpec: SpringSpec<Float>,
	val sizeToExpandedAnimationSpec: TweenSpec<IntSize>,
	val sizeToCollapsedAnimationSpec: SpringSpec<IntSize>
)

class OverlayLayoutDefaults {
	fun overlayDefaultAnimationSpecs(
		positionToExpandedAnimationSpec: TweenSpec<Offset> = tween(600, 0, easing = EaseOutExpo),
		positionToCollapsedAnimationSpec: SpringSpec<Offset> = spring(0.9f, 500f),
		alignmentToExpandedAnimationSpec: TweenSpec<Float> = tween(600, 0, easing = EaseOutExpo),
		alignmentToCollapsedAnimationSpec: SpringSpec<Float> = spring( 0.9f, 500f),
		sizeToExpandedAnimationSpec: TweenSpec<IntSize> = tween(600, 0, easing = EaseOutExpo),
		sizeToCollapsedAnimationSpec: SpringSpec<IntSize> = spring(1f, 600f)
	) = OverlayAnimationSpecs(
		positionToExpandedAnimationSpec = positionToExpandedAnimationSpec,
		positionToCollapsedAnimationSpec = positionToCollapsedAnimationSpec,
		alignmentToExpandedAnimationSpec = alignmentToExpandedAnimationSpec,
		alignmentToCollapsedAnimationSpec = alignmentToCollapsedAnimationSpec,
		sizeToExpandedAnimationSpec = sizeToExpandedAnimationSpec,
		sizeToCollapsedAnimationSpec = sizeToCollapsedAnimationSpec
	)
}