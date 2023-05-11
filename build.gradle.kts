buildscript {
	val agp_version by extra("8.2.0-alpha03")
	dependencies {
		classpath("com.android.tools.build:gradle:$agp_version")
	}
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
	alias(libs.plugins.org.jetbrains.kotlin.android) apply false
	alias(libs.plugins.com.android.library) apply false
	`maven-publish`
}
