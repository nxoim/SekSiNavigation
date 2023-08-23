package com.number869.seksinavigation.data

import com.number869.seksinavigation.R

data class PagerItem(val avatar: Int, val nickname: String, val content: Int)

val pagerItems = listOf(
	PagerItem(R.drawable.ic_launcher_foreground, "story 0", R.drawable.ic_launcher_background),
	PagerItem(R.drawable.ic_launcher_foreground, "story 1", R.drawable.ic_launcher_background),
	PagerItem(R.drawable.ic_launcher_foreground, "story 2", R.drawable.ic_launcher_background),
	PagerItem(R.drawable.ic_launcher_foreground, "story 3", R.drawable.ic_launcher_background),
	PagerItem(R.drawable.ic_launcher_foreground, "story 4", R.drawable.ic_launcher_background),
	PagerItem(R.drawable.ic_launcher_foreground, "story 5", R.drawable.ic_launcher_background),
	PagerItem(R.drawable.ic_launcher_foreground, "story 6", R.drawable.ic_launcher_background),
	PagerItem(R.drawable.ic_launcher_foreground, "story 7", R.drawable.ic_launcher_background),
	PagerItem(R.drawable.ic_launcher_foreground, "story 8", R.drawable.ic_launcher_background),
	PagerItem(R.drawable.ic_launcher_foreground, "story 9", R.drawable.ic_launcher_background)
)