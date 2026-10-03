package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.OfflineBanner
import com.example.ui.components.VidoMixBottomBar
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.VidoMixTheme
import com.example.ui.viewmodel.VidoMixViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: VidoMixViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.core.AppCrashReporter.installGlobalHandler()
        enableEdgeToEdge()
        setContent {
            val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()

            // RTL Layout Direction for native Arabic experience
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                VidoMixTheme(darkTheme = isDark) {
                    VidoMixApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun VidoMixApp(viewModel: VidoMixViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Lifecycle monitoring: pause video player when app goes into background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    viewModel.playerController.pausePlayback()
                }
                Lifecycle.Event.ON_RESUME -> {
                    // Check state if needed
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Show Bottom Bar on primary tabs only
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Shorts.route,
        Screen.Upload.route,
        Screen.Videos.route,
        Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                VidoMixBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { targetRoute ->
                        if (targetRoute != currentRoute) {
                            navController.navigate(targetRoute) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else androidx.compose.ui.unit.Dp.Unspecified)
        ) {
            // Global Offline Status Notification Banner
            OfflineBanner(
                isOnline = isOnline,
                onRetry = { viewModel.refreshAllData() },
                modifier = Modifier.statusBarsPadding()
            )

            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.weight(1f)
            ) {
            // Home Feed
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToVideo = { videoId ->
                        navController.navigate(Screen.VideoDetail.createRoute(videoId))
                    },
                    onNavigateToShorts = {
                        navController.navigate(Screen.Shorts.route)
                    },
                    onNavigateToChannel = { channelId ->
                        navController.navigate(Screen.Channel.createRoute(channelId))
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search.route)
                    },
                    onNotificationsClick = {
                        navController.navigate(Screen.Notifications.route)
                    },
                    onProfileClick = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onNavigateToLive = { streamId ->
                        navController.navigate(Screen.LiveBroadcast.createRoute(streamId))
                    },
                    onStartLiveClick = {
                        navController.navigate(Screen.LiveSetup.route)
                    }
                )
            }

            // Shorts (Vertical Pager)
            composable(Screen.Shorts.route) {
                ShortsScreen(
                    viewModel = viewModel,
                    onNavigateToChannel = { channelId ->
                        navController.navigate(Screen.Channel.createRoute(channelId))
                    }
                )
            }

            // Videos Explorer
            composable(Screen.Videos.route) {
                VideosScreen(
                    viewModel = viewModel,
                    onNavigateToVideo = { videoId ->
                        navController.navigate(Screen.VideoDetail.createRoute(videoId))
                    },
                    onNavigateToChannel = { channelId ->
                        navController.navigate(Screen.Channel.createRoute(channelId))
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search.route)
                    }
                )
            }

            // Upload Screen
            composable(Screen.Upload.route) {
                UploadScreen(
                    viewModel = viewModel,
                    onUploadSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onStartLiveClick = {
                        navController.navigate(Screen.LiveSetup.route)
                    },
                    onNavigateToVideo = { videoId ->
                        navController.navigate(Screen.VideoDetail.createRoute(videoId))
                    }
                )
            }

            // Profile Screen
            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = viewModel,
                    onNavigateToVideo = { videoId ->
                        navController.navigate(Screen.VideoDetail.createRoute(videoId))
                    },
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route)
                    },
                    onNavigateToAdmin = {
                        navController.navigate(Screen.Admin.route)
                    },
                    onNavigateToChannel = { channelId ->
                        navController.navigate(Screen.Channel.createRoute(channelId))
                    },
                    onNavigateToMonetization = {
                        navController.navigate(Screen.Monetization.route)
                    },
                    onNavigateToStudio = {
                        navController.navigate(Screen.CreatorStudio.route)
                    },
                    onNavigateToPayments = {
                        navController.navigate(Screen.Payments.route)
                    },
                    onNavigateToWallet = {
                        navController.navigate(Screen.Wallet.route)
                    },
                    onNavigateToLiveSetup = {
                        navController.navigate(Screen.LiveSetup.route)
                    }
                )
            }

            // Monetization Screen
            composable(Screen.Monetization.route) {
                MonetizationScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToStudio = { navController.navigate(Screen.CreatorStudio.route) },
                    onNavigateToPayments = { navController.navigate(Screen.Payments.route) }
                )
            }

            // Creator Studio Screen
            composable(Screen.CreatorStudio.route) {
                CreatorStudioScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPayments = { navController.navigate(Screen.Payments.route) },
                    onNavigateToMonetization = { navController.navigate(Screen.Monetization.route) }
                )
            }

            // Payments Screen
            composable(Screen.Payments.route) {
                PaymentsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Video Detail & Player
            composable(
                route = Screen.VideoDetail.route,
                arguments = listOf(navArgument("videoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                VideoDetailScreen(
                    videoId = videoId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChannel = { channelId ->
                        navController.navigate(Screen.Channel.createRoute(channelId))
                    },
                    onNavigateToVideo = { nextVideoId ->
                        navController.navigate(Screen.VideoDetail.createRoute(nextVideoId))
                    }
                )
            }

            // Channel Screen
            composable(
                route = Screen.Channel.route,
                arguments = listOf(navArgument("channelId") { type = NavType.StringType })
            ) { backStackEntry ->
                val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                ChannelProfileScreen(
                    channelId = channelId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToVideo = { videoId ->
                        navController.navigate(Screen.VideoDetail.createRoute(videoId))
                    },
                    onNavigateToShorts = {
                        navController.navigate(Screen.Shorts.route)
                    }
                )
            }

            // Search Screen
            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToVideo = { videoId ->
                        navController.navigate(Screen.VideoDetail.createRoute(videoId))
                    },
                    onNavigateToShorts = {
                        navController.navigate(Screen.Shorts.route)
                    },
                    onNavigateToChannel = { channelId ->
                        navController.navigate(Screen.Channel.createRoute(channelId))
                    }
                )
            }

            // Notifications Screen
            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToVideo = { videoId ->
                        navController.navigate(Screen.VideoDetail.createRoute(videoId))
                    }
                )
            }

            // Auth Screen
            composable(Screen.Auth.route) {
                AuthScreen(
                    viewModel = viewModel,
                    onAuthSuccess = { navController.popBackStack() },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Admin Dashboard
            composable(Screen.Admin.route) {
                AdminDashboardScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Wallet Screen
            composable(Screen.Wallet.route) {
                WalletScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAuth = { navController.navigate(Screen.Auth.route) }
                )
            }

            // Live Stream Setup Screen
            composable(Screen.LiveSetup.route) {
                LiveSetupScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onStreamStarted = { streamId ->
                        navController.navigate(Screen.LiveBroadcast.createRoute(streamId)) {
                            popUpTo(Screen.LiveSetup.route) { inclusive = true }
                        }
                    },
                    onNavigateToAuth = { navController.navigate(Screen.Auth.route) }
                )
            }

            // Live Broadcast Screen
            composable(
                route = Screen.LiveBroadcast.route,
                arguments = listOf(navArgument("streamId") { type = NavType.StringType })
            ) { backStackEntry ->
                val streamId = backStackEntry.arguments?.getString("streamId") ?: ""
                LiveBroadcastScreen(
                    streamId = streamId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToWallet = { navController.navigate(Screen.Wallet.route) },
                    onNavigateToChannel = { channelId ->
                        navController.navigate(Screen.Channel.createRoute(channelId))
                    }
                )
            }
        }
    }
}
}
