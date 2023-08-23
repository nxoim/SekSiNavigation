package com.number869.seksinavigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize


data class OverlayParameters(
	// if unspecified, it will be the size of the screen
	val size: DpSize = DpSize.Unspecified,
	val animationSpecs: OverlayAnimationSpecs = OverlayDefaults.defaultOverlayAnimationSpecs,
	val animationBehaviorType: OverlayAnimationBehaviors = OverlayAnimationBehaviors.ContainerMorph,
	// it's centered if unspecified
	val targetOffset: Offset = Offset.Unspecified
)

enum class OverlayAnimationBehaviors {
	ContainerMorph,
	LookaheadFullscreenLayoutTest
}

data class OverlayAnimationSpecs(
	val containerMorphAnimationSpecs: ContainerMorphAnimationSpecs
)

data class ContainerMorphAnimationSpecs(
	val offsetToExpanded: SpringSpec<Offset>,
	val offsetToCollapsed: SpringSpec<Offset>,
	val sizeToExpanded: SpringSpec<IntSize>,
	val sizeToCollapsed: SpringSpec<IntSize>,
	// this does nothing if target offset is specified in OverlayParameters
	val bounceThroughTheCenter: Boolean
)

interface OverlayDefaults {
	companion object {
		val defaultContainerMorphAnimationSpecs = ContainerMorphAnimationSpecs(
			offsetToExpanded = spring(1.2f, 1700f),
			offsetToCollapsed = spring(1.5f, 1800f),
			sizeToExpanded = spring(1.2f, 1700f),
			sizeToCollapsed = spring(1.4f, 1800f),
			bounceThroughTheCenter = false
		)

		val defaultOverlayAnimationSpecs = OverlayAnimationSpecs(
			containerMorphAnimationSpecs = defaultContainerMorphAnimationSpecs
		)

		val defaultOverlayParameters = OverlayParameters(
			size = DpSize.Unspecified,
			animationSpecs = defaultOverlayAnimationSpecs
		)
	}
}

val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

