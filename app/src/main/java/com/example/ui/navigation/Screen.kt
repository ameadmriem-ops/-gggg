package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Shorts : Screen("shorts")
    data object Upload : Screen("upload")
    data object Videos : Screen("videos")
    data object Profile : Screen("profile")
    data object Search : Screen("search")
    data object Notifications : Screen("notifications")
    data object Auth : Screen("auth")
    data object Admin : Screen("admin")
    data object Monetization : Screen("monetization")
    data object CreatorStudio : Screen("creator_studio")
    data object Payments : Screen("payments")

    data object VideoDetail : Screen("video_detail/{videoId}") {
        fun createRoute(videoId: String) = "video_detail/$videoId"
    }

    data object Channel : Screen("channel/{channelId}") {
        fun createRoute(channelId: String) = "channel/$channelId"
    }
}

data class BottomNavItem(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

val bottomNavItems = listOf(
    BottomNavItem(
        title = "الرئيسية",
        route = Screen.Home.route,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_home"
    ),
    BottomNavItem(
        title = "قصيرة",
        route = Screen.Shorts.route,
        selectedIcon = Icons.Filled.PlayCircle,
        unselectedIcon = Icons.Outlined.PlayCircle,
        testTag = "nav_shorts"
    ),
    BottomNavItem(
        title = "رفع",
        route = Screen.Upload.route,
        selectedIcon = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircleOutline,
        testTag = "nav_upload"
    ),
    BottomNavItem(
        title = "الفيديوهات",
        route = Screen.Videos.route,
        selectedIcon = Icons.Filled.VideoLibrary,
        unselectedIcon = Icons.Outlined.VideoLibrary,
        testTag = "nav_videos"
    ),
    BottomNavItem(
        title = "حسابي",
        route = Screen.Profile.route,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "nav_profile"
    )
)
