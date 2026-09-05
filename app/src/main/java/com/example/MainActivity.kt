package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ads.AdMobConfig
import com.example.data.local.AppDatabase
import com.example.data.repository.ReviewTaskRepository
import com.example.data.service.AdMobService
import com.example.data.service.FcmNotificationService
import com.example.ui.components.BannerAdView
import com.example.ui.screens.*
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val fcmService = FcmNotificationService(applicationContext)
        val repository = ReviewTaskRepository(database, fcmService)
        val adMobService = AdMobService(applicationContext)

        setContent {
            MyApplicationTheme {
                ReviewTaskApp(
                    repository = repository,
                    adMobService = adMobService
                )
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    object Splash : Screen("splash", "Splash")
    object Login : Screen("login", "Login")
    object Register : Screen("register", "Register")
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Tasks : Screen("tasks", "Tasks", Icons.Default.Assignment)
    object Wallet : Screen("wallet", "Wallet", Icons.Default.Payments)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object TaskDetail : Screen("task_detail/{campaignId}", "Task Details") {
        fun createRoute(campaignId: String) = "task_detail/$campaignId"
    }
    object Feedback : Screen("feedback/{taskId}/{campaignId}", "Feedback") {
        fun createRoute(taskId: String, campaignId: String) = "feedback/$taskId/$campaignId"
    }
    object TaskSuccess : Screen("task_success/{campaignId}", "Task Success") {
        fun createRoute(campaignId: String) = "task_success/$campaignId"
    }
    object AdminPortal : Screen("admin_portal", "Admin Console")
}

@Composable
fun ReviewTaskApp(
    repository: ReviewTaskRepository,
    adMobService: AdMobService
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val currentUserId by repository.currentUserId.collectAsStateWithLifecycle(initialValue = null)
    val isAuthenticated = currentUserId != null

    val bottomNavScreens = listOf(Screen.Home, Screen.Tasks, Screen.Wallet, Screen.Profile)
    val showBottomBar = currentDestination in bottomNavScreens.map { it.route }

    val isAdsEnabled by adMobService.isAdsEnabled.collectAsStateWithLifecycle()
    val adStatusMessage by adMobService.adStatusMessage.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (showBottomBar) {
                Column {
                    // Google AdMob Reusable Banner Component
                    if (isAdsEnabled) {
                        BannerAdView(
                            modifier = Modifier.fillMaxWidth(),
                            adUnitId = AdMobConfig.bannerAdUnitId,
                            showLabel = true
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 0.dp,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        bottomNavScreens.forEach { screen ->
                            val selected = currentDestination == screen.route
                            NavigationBarItem(
                                icon = {
                                    screen.icon?.let {
                                        Icon(imageVector = it, contentDescription = screen.title)
                                    }
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                },
                                selected = selected,
                                onClick = {
                                    if (currentDestination != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_item_${screen.route}")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    isAuthenticated = isAuthenticated,
                    onNavigateNext = { authenticated ->
                        if (authenticated) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    repository = repository,
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    repository = repository,
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    onNavigateBackToLogin = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    repository = repository,
                    onNavigateToTasks = {
                        navController.navigate(Screen.Tasks.route)
                    },
                    onNavigateToWallet = {
                        navController.navigate(Screen.Wallet.route)
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onSelectTask = { campaignId ->
                        navController.navigate(Screen.TaskDetail.createRoute(campaignId))
                    }
                )
            }

            composable(Screen.Tasks.route) {
                TaskListScreen(
                    repository = repository,
                    onSelectTask = { campaignId ->
                        navController.navigate(Screen.TaskDetail.createRoute(campaignId))
                    }
                )
            }

            composable(
                route = Screen.TaskDetail.route,
                arguments = listOf(navArgument("campaignId") { type = NavType.StringType })
            ) { backStackEntry ->
                val campaignId = backStackEntry.arguments?.getString("campaignId") ?: ""
                TaskDetailScreen(
                    campaignId = campaignId,
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() },
                    onProceedToFeedback = { taskId ->
                        navController.navigate(Screen.Feedback.createRoute(taskId, campaignId))
                    }
                )
            }

            composable(
                route = Screen.Feedback.route,
                arguments = listOf(
                    navArgument("taskId") { type = NavType.StringType },
                    navArgument("campaignId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                val campaignId = backStackEntry.arguments?.getString("campaignId") ?: ""
                FeedbackScreen(
                    taskId = taskId,
                    campaignId = campaignId,
                    repository = repository,
                    adMobService = adMobService,
                    onNavigateBack = { navController.popBackStack() },
                    onSubmitSuccess = {
                        navController.navigate(Screen.TaskSuccess.createRoute(campaignId)) {
                            popUpTo(Screen.Tasks.route)
                        }
                    }
                )
            }

            composable(
                route = Screen.TaskSuccess.route,
                arguments = listOf(navArgument("campaignId") { type = NavType.StringType })
            ) { backStackEntry ->
                val campaignId = backStackEntry.arguments?.getString("campaignId") ?: ""
                TaskSuccessScreen(
                    campaignId = campaignId,
                    repository = repository,
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onNavigateToWallet = {
                        navController.navigate(Screen.Wallet.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            composable(Screen.Wallet.route) {
                WalletScreen(
                    repository = repository,
                    adMobService = adMobService
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    repository = repository,
                    onNavigateToAdmin = {
                        navController.navigate(Screen.AdminPortal.route)
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.AdminPortal.route) {
                AdminPortalScreen(
                    repository = repository,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
