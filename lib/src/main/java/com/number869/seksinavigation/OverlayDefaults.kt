 package com.number869.seksinavigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize

data class OverlayAnimationSpecs(
	val positionToExpanded: SpringSpec<Offset>,
	val positionToCollapsed: SpringSpec<Offset>,
	val sizeToExpanded: SpringSpec<IntSize>,
	val sizeToCollapsed: SpringSpec<IntSize>
)

 data class OverlayParameters(
	 val size: DpSize = DpSize.Unspecified,
	 val animationSpecs: OverlayAnimationSpecs = OverlayDefaults.defaultOverlayAnimationSpecs,
 )

interface OverlayDefaults {
	companion object {
		val defaultOverlayAnimationSpecs = OverlayAnimationSpecs(
			positionToExpanded = spring(1.6f, 2500f),
			positionToCollapsed = spring(1.6f, 3000f),
			sizeToExpanded = spring(1.6f, 2500f),
			sizeToCollapsed = spring(1.6f, 3000f)
		)

		val defaultOverlayParameters = OverlayParameters(
			size = DpSize.Unspecified,
			animationSpecs = defaultOverlayAnimationSpecs
		)
	}
}

val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)