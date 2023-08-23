package com.number869.seksinavigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntSizeAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.number869.seksinavigation.data.pagerItems


@Composable
fun StoriesExampleCollapsed(state: OverlayLayoutState, key: String, index: Int) {
	val isExpanded by remember { derivedStateOf { state.getIsExpanded(key) } }

	// this means the image will be 9:16 when expanded

	Column(horizontalAlignment = Alignment.CenterHorizontally) {
		Image(
			painter = painterResource(id = pagerItems[index].avatar),
			contentDescription = "story image",
			modifier = Modifier
				.size(64.dp)
				.clip(CircleShape)
				.background(MaterialTheme.colorScheme.secondaryContainer),
			contentScale = ContentScale.FillBounds
		)

		AnimatedVisibility(visible = !isExpanded) {
			Text(text = key)
		}
	}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StoriesExampleExpanded(state: OverlayLayoutState, key: String, index: Int, pagerState: PagerState) {
//	HorizontalPager(
//		pageCount = pagerItems.size,
//		state = pagerState
//	) {
//		val isExpanded by remember { derivedStateOf { state.getIsExpanded(key) } }
//
//		val pageName = pagerItems[it].nickname
//		val pageContent = painterResource(id = pagerItems[it].content)
//		val pageAvatar = painterResource(id = pagerItems[it].avatar)
//
//		Box {
//			val animatedContentSize by animateIntSizeAsState(
//				if (isExpanded)
//					IntSize(
//						state.itemsState[key]!!.overlayParameters.size.width.value.toInt(),
//						state.itemsState[key]!!.overlayParameters.size.height.value.toInt()
//					)
//				else
//					IntSize(64, 64), label = ""
//			)
//
//			val animatedContentShape by animateDpAsState(
//				if (isExpanded) 8.dp else 160.dp, label = "",
//			)
//
//			Image(
//				painter = pageContent,
//				contentDescription = pageName,
//				Modifier
//					.size(animatedContentSize.width.dp, animatedContentSize.height.dp)
//					.clip(RoundedCornerShape(animatedContentShape)),
//				contentScale = ContentScale.FillBounds
//			)
//
//			Box(
//				Modifier
//					.fillMaxWidth()
//					.height(92.dp)
//					.background(
//						brush = Brush.verticalGradient(
//							listOf(
//								MaterialTheme.colorScheme.surface.copy(0.3f),
//								MaterialTheme.colorScheme.surface.copy(0f)
//							)
//						)
//					)
//			)
//
//			Column(horizontalAlignment = Alignment.CenterHorizontally) {
//				val animatedPadding by animateDpAsState(
//					if (isExpanded) 16.dp else 0.dp, label = "",
//				)
//
//				Row(Modifier.padding(animatedPadding), verticalAlignment = Alignment.CenterVertically) {
//					val animatedAvatarSize by animateDpAsState(
//						if (isExpanded) 42.dp else 64.dp, label = "",
//					)
//
//					Image(
//						painter = pageAvatar,
//						contentDescription = "story image",
//						modifier = Modifier
//							.size(animatedAvatarSize)
//							.clip(CircleShape)
//							.background(MaterialTheme.colorScheme.secondaryContainer),
//						contentScale = ContentScale.FillBounds
//					)
//
//					Spacer(modifier = Modifier.width(8.dp))
//
//					AnimatedVisibility(
//						visible = isExpanded,
//						enter = fadeIn(tween(200, 100)),
//						exit = fadeOut(tween(100))
//					) {
//						Text(text = pageName)
//					}
//				}
//
//
//				AnimatedVisibility(
//					visible = !isExpanded,
//					enter = fadeIn(),
//					exit = fadeOut()
//				) {
//					Text(text = key)
//				}
//			}
//		}
//	}
}