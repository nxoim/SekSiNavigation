package com.number869.seksinavigation

import android.os.Bundle
import android.service.controls.ControlsProviderService.TAG
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key.Companion.Home
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.number869.seksinavigation.data.pagerItems
import com.number869.seksinavigation.ui.theme.SekSiNavigationTheme


class MainActivity : ComponentActivity() {
	@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {

			val overlayLayoutState = rememberOverlayLayoutState()
			val storiesRowState = rememberLazyListState()
			val pagerState = rememberPagerState(
				initialPage = 0,
				initialPageOffsetFraction = 0f
			) {
				10
			}
			var currentPage by remember { mutableStateOf(0) }

			LaunchedEffect(currentPage) {
				pagerState.scrollToPage(currentPage)

				Log.d(TAG, "currentPage: $currentPage")
				Log.d(TAG, "overlayLayoutState: ${overlayLayoutState.overlayStack.last()}")
			}

			LaunchedEffect(pagerState.currentPage) {
				currentPage = pagerState.currentPage
			}

			SekSiNavigationTheme {
				Surface {
					OverlayLayoutLookaheadAllTest(this) {
						Nav()
					}
				}
			}
		}
	}
}

@Composable
fun OverlayLayoutLookaheadAllTestScope.Nav() {
	val navController = rememberNavController()

	NavHost(
		navController,
		startDestination = "Home",
		enterTransition = { fadeIn(tween(0)) },
		exitTransition = { fadeOut(tween(0)) }
	) {
		composable("Home") {
			Home(navController)
		}

		composable("Favs") {
			Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
				getAnOverlaysContents("search")?.invoke()
				BottomBar(navController)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayLayoutLookaheadAllTestScope.Home(navController: NavController) {
	Scaffold(
		topBar = {
			CenterAlignedTopAppBar(
				title = { Text(text = "Sek Si Navigation Demo") },
				windowInsets = WindowInsets.statusBars
			)
		},
		bottomBar = { BottomBar(navController) }
	) { scaffoldPadding ->
		Surface(Modifier.padding(scaffoldPadding)) {
			LazyColumn(
				contentPadding = PaddingValues(horizontal = 16.dp),
				verticalArrangement = Arrangement.spacedBy(8.dp)
			) {
				item {
					val key = "search"
					OverlayItemLookaheadWrapper(
						Modifier.zIndex(1f),
						key = "search"
					) {
						SearchExample(
							Modifier
								.let {
									return@let if (displayedOverlays.contains(key))
										it.fillMaxSize()
									else
										it
											.fillMaxWidth()
											.height(64.dp)
								}
								.clickable {
									if (displayedOverlays.contains(key)) closeLastOverlay() else addToListOfDisplayedOverlays(
										key
									)
								}
						)
					}
					Spacer(modifier = Modifier.height(16.dp))
				}

//				item {
//					LazyRow(
//						Modifier.padding(bottom = 16.dp),
//						state = storiesRowState,
//						horizontalArrangement = spacedBy(16.dp)
//					) {
//						itemsIndexed(pagerItems) { index, it ->
//							val key = it.nickname
//
//							val screenWidth =
//								LocalConfiguration.current.screenWidthDp
//							val storySize = DpSize(
//								screenWidth.dp,
//								(screenWidth * 1.7777778f).dp
//							)
//
//							OverlayItemWrapper(
//								originalContent = {
//									StoriesExampleCollapsed(overlayLayoutState, key, index)
//								},
//								overlayContent = {
//									StoriesExampleExpanded(overlayLayoutState, key, currentPage, pagerState)
//								},
//								screenBehindContent = {
//									val backgroundAlpha =
//										(1f - overlayLayoutState.itemsState[key]?.gestureData?.progress!! * 0.5f)
//									Box(
//										Modifier
//											.background(
//												Color.Black.copy(
//													backgroundAlpha
//												)
//											)
//											.fillMaxSize(),
//										contentAlignment = Alignment.TopCenter
//									) {
//
//									}
//								},
//								screenAboveContent = {
//									Box(
//										Modifier.fillMaxSize(),
//										contentAlignment = Alignment.BottomCenter
//									) {
//										val density = LocalDensity.current
//										val screenHeight = 	(LocalConfiguration.current.screenHeightDp + WindowInsets.statusBars.getTop(density))
//										val bottomThingSize = DpSize(
//											LocalConfiguration.current.screenWidthDp.dp,
//											screenHeight.dp - storySize.height
//										)
//										Box(Modifier.size(bottomThingSize), contentAlignment = Alignment.Center) {
//											Box(
//												Modifier
//													.padding(horizontal = 16.dp)
//													.fillMaxWidth()
//													.height(64.dp)
//													.border(
//														1.dp,
//														Color.White,
//														CircleShape
//													),
//												contentAlignment = Alignment.Center
//											) {
//												Text(
//													text = "Bottom thing",
//													color = Color.White
//												)
//											}
//										}
//									}
//								},
//								originalModifier = Modifier
//									.width(64.dp)
//									.clickable {
//										currentPage = index
//										overlayLayoutState.addToOverlayStack(key)
//									},
//								overlayParameters = OverlayParameters(
//									size = storySize,
//									targetOffset = Offset(
//										0f,
//										WindowInsets.statusBars.getTop(LocalDensity.current).toFloat()
//									),
//									animationBehaviorType = OverlayAnimationBehaviors.LookaheadFullscreenLayoutTest
//								),
//								key = key,
//								state = overlayLayoutState
//							)
//						}
//					}
//				}
//
//				repeat(100) {
//					item {
//						val key = it.toString()
//
//						OverlayItemWrapper(
//							Modifier
//								.clickable {
//									overlayLayoutState.addToOverlayStack(
//										key
//									)
//								}
//								.height(64.dp)
//								.fillMaxWidth(),
//							originalContent = {
//								UmWhateverExampleIdk(
//									overlayLayoutState,
//									key
//								)
//							},
//							overlayContent = {
//								UmWhateverExampleIdk(
//									overlayLayoutState,
//									key
//								)
//							},
//							overlayParameters = OverlayParameters(
//								size = DpSize(400.dp, 500.dp),
//								animationSpecs = OverlayDefaults.defaultOverlayAnimationSpecs.copy(
//									containerMorphAnimationSpecs = OverlayDefaults.defaultContainerMorphAnimationSpecs.copy(
//										bounceThroughTheCenter = true
//									),
//								),
//								animationBehaviorType = OverlayAnimationBehaviors.LookaheadFullscreenLayoutTest
//							),
//							originalCornerRadius = 16.dp,
//							key = key,
//							state = overlayLayoutState
//						)
//					}
//				}
			}
		}
	}
}

@Composable
fun BottomBar(navController: NavController) {
	NavigationBar(windowInsets = WindowInsets.navigationBars) {
		NavigationBarItem(
			selected = navController.currentDestination?.route == "Home",
			onClick = { navController.navigate("Home") },
			icon = {
				Icon(Icons.Default.Home, contentDescription = "")
			},
			label = {
				Text(text = "Home")
			}
		)
		NavigationBarItem(
			selected = navController.currentDestination?.route == "Favs",
			onClick = { navController.navigate("Favs") },
			icon = {
				Icon(Icons.Default.FavoriteBorder, contentDescription = "")
			},
			label = {
				Text(text = "Favourites")
			}
		)
	}
}
