package com.example.stora.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.stora.utils.TokenManager
import com.example.stora.viewmodel.AuthViewModel
import com.example.stora.viewmodel.UserProfileViewModel
import com.example.stora.screens.AddItemScreen
import com.example.stora.screens.DetailInventoryScreen
import com.example.stora.screens.EditInventoryScreen
import com.example.stora.screens.InventoryScreen
import com.example.stora.screens.AuthScreen
import com.example.stora.screens.HomeScreen
import com.example.stora.screens.LoansScreen
import com.example.stora.screens.NewLoanScreen
import com.example.stora.screens.LoanFormScreen
import com.example.stora.screens.DetailLoanScreen
import com.example.stora.screens.DetailLoanHistoryScreen
import com.example.stora.screens.ProfileScreen
import com.example.stora.screens.EditProfileScreen
import com.example.stora.screens.SettingScreen
import androidx.compose.animation.fadeOut
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun AppNavHost(
    navController: NavHostController,
    userProfileViewModel: UserProfileViewModel,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = TokenManager.getInstance(context)
    val authState by authViewModel.uiState.collectAsState()

    // Navigate after successful login
    LaunchedEffect(authState.isLoggedIn, authState.isSuccess) {
        if (authState.isLoggedIn && authState.isSuccess) {
            navController.navigate(Routes.HOME_SCREEN) {
                popUpTo(Routes.AUTH_SCREEN) { inclusive = true }
            }
        } else if (!authState.isLoggedIn && !tokenManager.isLoggedIn()) {
            // User logged out, navigate to auth screen
            navController.navigate(Routes.AUTH_SCREEN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val startDestination = if (tokenManager.isLoggedIn()) {
        Routes.HOME_SCREEN
    } else {
        Routes.AUTH_SCREEN
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        composable(
            route = Routes.AUTH_SCREEN,

            exitTransition = {

                if (targetState.destination.route == Routes.HOME_SCREEN) {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(1000)
                    )
                } else {

                    fadeOut(animationSpec = tween(1000))
                }
            }
        ) {
            AuthScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable(
            route = Routes.HOME_SCREEN,

            enterTransition = {

                if (initialState.destination.route == Routes.AUTH_SCREEN) {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(1000)
                    )
                } else {

                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(500)
                    )
                }
            },

            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(500)
                )
            }
        ) {
            HomeScreen(navController = navController, userProfileViewModel = userProfileViewModel)
        }

        composable(
            route = Routes.INVENTORY_SCREEN,

            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(500)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(500)
                )
            }
        ) {
            InventoryScreen(navController = navController)
        }

        composable(
            route = Routes.DETAIL_SCREEN,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(500)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(500)
                )
            }
        ) { backStackEntry ->

            val itemId = backStackEntry.arguments?.getString("itemId")
            DetailInventoryScreen(navController = navController, itemId = itemId)
        }

        composable(
            route = Routes.ADD_ITEM_SCREEN,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(500)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(500)
                )
            }
        ) {
            AddItemScreen(navController = navController)
        }

        composable(
            route = Routes.EDIT_ITEM_SCREEN,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(500)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(500)
                )
            }
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")
            EditInventoryScreen(navController = navController, itemId = itemId)
        }

        composable(
            route = Routes.LOANS_SCREEN,
            arguments = listOf(
                navArgument("showDeleteSnackbar") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(500)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(500)
                )
            }
        ) { backStackEntry ->
            val showDeleteSnackbar = backStackEntry.arguments?.getBoolean("showDeleteSnackbar") ?: false
            LoansScreen(
                navController = navController,
                showDeleteSnackbar = showDeleteSnackbar
            )
        }

        composable(
            route = Routes.NEW_LOAN_SCREEN,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(500)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(500)
                )
            }
        ) {
            NewLoanScreen(navController = navController)
        }

        composable(
            route = Routes.LOAN_FORM_SCREEN,
            arguments = listOf(
                navArgument("selectedItems") { type = NavType.StringType }
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(500)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(500)
                )
            }
        ) { backStackEntry ->
            val selectedItemsString = backStackEntry.arguments?.getString("selectedItems") ?: ""
            val selectedItemsList = if (selectedItemsString.isNotEmpty()) {
                selectedItemsString.split(",")
            } else {
                emptyList()
            }
            LoanFormScreen(
                navController = navController,
                selectedItemIds = selectedItemsList
            )
        }

        composable(
            route = Routes.DETAIL_LOAN_SCREEN,
            arguments = listOf(
                navArgument("loanId") { type = NavType.IntType }
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(500)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(500)
                )
            }
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getInt("loanId") ?: 0
            DetailLoanScreen(
                navController = navController,
                loanId = loanId
            )
        }

        composable(
            route = Routes.DETAIL_LOAN_HISTORY_SCREEN,
            arguments = listOf(
                navArgument("loanId") { type = NavType.IntType }
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(500)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(500)
                )
            }
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getInt("loanId") ?: 0
            DetailLoanHistoryScreen(
                navController = navController,
                loanId = loanId
            )
        }

        composable(
            route = Routes.PROFILE_SCREEN,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(500)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(500)
                )
            }
        ) {
            ProfileScreen(
                navController = navController,
                viewModel = userProfileViewModel,
                authViewModel = authViewModel
            )
        }

        composable(
            route = Routes.EDIT_PROFILE_SCREEN,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(500)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(500)
                )
            }
        ) {
            EditProfileScreen(
                navController = navController,
                viewModel = userProfileViewModel
            )
        }

        composable(
            route = Routes.SETTING_SCREEN,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(500)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(500)
                )
            }
        ) {
            SettingScreen(navController = navController)
        }

        composable(
            route = Routes.REMINDER_SETTINGS_SCREEN,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(500)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(500)
                )
            }
        ) {
            com.example.stora.screens.ReminderSettingsScreen(navController = navController)
        }
    }
}
