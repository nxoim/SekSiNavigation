 package com.number869.seksinavigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInCirc
import androidx.compose.animation.core.EaseInExpo
import androidx.compose.animation.core.EaseInQuart
import androidx.compose.animation.core.EaseOutCirc
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

data class OverlayAnimationSpecs(
	val positionToExpandedAnimationSpec: TweenSpec<Offset>,
	val positionToCollapsedAnimationSpec: SpringSpec<Offset>,
	val sizeToExpandedAnimationSpec: TweenSpec<IntSize>,
	val sizeToCollapsedAnimationSpec: TweenSpec<IntSize>
)

 class OverlayLayoutDefaults {
	fun overlayDefaultAnimationSpecs(
		positionToExpandedAnimationSpec: TweenSpec<Offset> = tween(500, 0, easing = EmphasizedDecelerate),
		positionToCollapsedAnimationSpec: SpringSpec<Offset> = spring(0.85f, 350f),
		sizeToExpandedAnimationSpec: TweenSpec<IntSize> = tween(500, 0, easing = EmphasizedDecelerate),
		sizeToCollapsedAnimationSpec: TweenSpec<IntSize> = tween(200, 0, easing = EaseOutQuart)
	) = OverlayAnimationSpecs(
		positionToExpandedAnimationSpec = positionToExpandedAnimationSpec,
		positionToCollapsedAnimationSpec = positionToCollapsedAnimationSpec,
		sizeToExpandedAnimationSpec = sizeToExpandedAnimationSpec,
		sizeToCollapsedAnimationSpec = sizeToCollapsedAnimationSpec
	)
}

val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)