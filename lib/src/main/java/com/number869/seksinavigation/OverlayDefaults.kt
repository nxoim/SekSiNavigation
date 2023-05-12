 package com.number869.seksinavigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize

data class OverlayAnimationSpecs(
	val positionToExpanded: SpringSpec<Offset>,
	val positionToCollapsed: SpringSpec<Offset>,
	val sizeToExpanded: SpringSpec<IntSize>,
	val sizeToCollapsed: SpringSpec<IntSize>,
	val bounceThroughTheCenter: Boolean
)

 data class OverlayParameters(
	 val size: DpSize = DpSize.Unspecified,
	 val animationSpecs: OverlayAnimationSpecs = OverlayDefaults.defaultOverlayAnimationSpecs,
 )

interface OverlayDefaults {
	companion object {
		val defaultOverlayAnimationSpecs = OverlayAnimationSpecs(
			positionToExpanded = spring(1.2f, 1700f),
			positionToCollapsed = spring(1.5f, 1800f),
			sizeToExpanded = spring(1.2f, 1700f),
			sizeToCollapsed = spring(1.4f, 1800f),
			bounceThroughTheCenter = false
		)

		val defaultOverlayParameters = OverlayParameters(
			size = DpSize.Unspecified,
			animationSpecs = defaultOverlayAnimationSpecs
		)
	}
}

val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)