plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
}

android {
	namespace = "com.number869.seksinavigation"

	defaultConfig {
		applicationId = "com.number869.seksinavigation"
		minSdk = 26
		targetSdk = 34
		versionCode = 1
		versionName = "1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		vectorDrawables {
			useSupportLibrary = true
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
			signingConfig = signingConfigs.getByName("debug")
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}
	kotlinOptions {
		jvmTarget = "1.8"
	}
	buildFeatures {
		compose = true
	}
	composeOptions {
		kotlinCompilerExtensionVersion = "1.5.1"
	}
	packagingOptions {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}
	compileSdk = 34
}

dependencies {
	implementation("androidx.core:core-ktx:1.12.0-beta01")
	implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
	implementation("androidx.activity:activity-compose:1.8.0-alpha06")
	implementation(platform("androidx.compose:compose-bom:2023.06.01"))
	implementation("androidx.compose.ui:ui:1.6.0-alpha02")
	implementation("androidx.compose.ui:ui-graphics:1.6.0-alpha02")
	implementation("androidx.compose.ui:ui-tooling-preview:1.6.0-alpha02")
	implementation("androidx.compose.material3:material3:1.2.0-alpha02")
	implementation("androidx.navigation:navigation-compose:2.7.0")
	implementation(project(mapOf("path" to ":lib")))
	testImplementation("junit:junit:4.13.2")
	androidTestImplementation("androidx.test.ext:junit:1.2.0-alpha02")
	androidTestImplementation("androidx.test.espresso:espresso-core:3.6.0-alpha02")
	androidTestImplementation(platform("androidx.compose:compose-bom:2023.06.01"))
	androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.0-alpha02")
	debugImplementation("androidx.compose.ui:ui-tooling:1.6.0-alpha02")
	debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.0-alpha02")
}