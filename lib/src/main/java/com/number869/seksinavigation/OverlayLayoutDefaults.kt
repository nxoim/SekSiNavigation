 package com.number869.seksinavigation

import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.EaseOutCirc
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.EaseOutQuint
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
		positionToExpandedAnimationSpec: TweenSpec<Offset> = tween(600, 0, easing = EaseOutQuint),
		positionToCollapsedAnimationSpec: SpringSpec<Offset> = spring(0.93f, 500f),
		alignmentToExpandedAnimationSpec: TweenSpec<Float> = tween(200, 0, easing = EaseOutQuint),
		alignmentToCollapsedAnimationSpec: SpringSpec<Float> = spring( 0.93f, 500f),
		sizeToExpandedAnimationSpec: TweenSpec<IntSize> = tween(200, 0, easing = EaseOutExpo),
		sizeToCollapsedAnimationSpec: SpringSpec<IntSize> = spring(0.93f, 300f)
	) = OverlayAnimationSpecs(
		positionToExpandedAnimationSpec = positionToExpandedAnimationSpec,
		positionToCollapsedAnimationSpec = positionToCollapsedAnimationSpec,
		alignmentToExpandedAnimationSpec = alignmentToExpandedAnimationSpec,
		alignmentToCollapsedAnimationSpec = alignmentToCollapsedAnimationSpec,
		sizeToExpandedAnimationSpec = sizeToExpandedAnimationSpec,
		sizeToCollapsedAnimationSpec = sizeToCollapsedAnimationSpec
	)
}