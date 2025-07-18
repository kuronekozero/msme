package com.example.mysavings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mysavings.ui.theme.MySavingsTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.ShowChart
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.material.icons.outlined.History
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.Info


class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val savingEntryDao by lazy { database.savingEntryDao() }
    private val userCategoryDao by lazy { database.userCategoryDao() }
    private val settingsRepository by lazy { SettingsRepository(this) }
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(settingsRepository, savingEntryDao, goalDao, userCategoryDao, applicationContext)
    }

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(savingEntryDao, userCategoryDao)
    }
    private val statisticsViewModel: StatisticsViewModel by viewModels {
        StatisticsViewModelFactory(savingEntryDao)
    }

    private val goalDao by lazy { database.goalDao() }

    private val goalsViewModel: GoalsViewModel by viewModels {
        GoalsViewModelFactory(goalDao, savingEntryDao)
    }

    private val historyViewModel: HistoryViewModel by viewModels {
        HistoryViewModelFactory(savingEntryDao, userCategoryDao)
    }

    private val achievementsViewModel: AchievementsViewModel by viewModels {
        AchievementsViewModelFactory(savingEntryDao, settingsRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set the locale before the activity is created
        runBlocking {
            val lang = settingsRepository.languageCodeFlow.first()
            setLocale(lang)
        }

        super.onCreate(savedInstanceState)

        // Set up a collector to update the locale if it changes while the app is running
        lifecycleScope.launch {
            settingsRepository.languageCodeFlow.collect { languageCode ->
                setLocale(languageCode)
            }
        }

        setContent {
            MySavingsTheme {
                val hasSeenWelcome by settingsRepository.welcomeScreenSeenFlow.collectAsState(initial = null)

                when (hasSeenWelcome) {
                    true -> {
                        AppShell(
                            mainViewModel = mainViewModel,
                            statisticsViewModel = statisticsViewModel,
                            settingsViewModel = settingsViewModel,
                            goalsViewModel = goalsViewModel,
                            historyViewModel = historyViewModel,
                            achievementsViewModel = achievementsViewModel
                        )
                    }
                    false -> {
                        WelcomeScreen(viewModel = settingsViewModel, onDismiss = { settingsViewModel.onWelcomeDismissed() })
                    }
                    null -> {
                        Surface(modifier = Modifier.fillMaxSize()) { }
                    }
                }
            }
        }
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}

@Composable
fun AppShell(
    mainViewModel: MainViewModel,
    statisticsViewModel: StatisticsViewModel,
    settingsViewModel: SettingsViewModel,
    goalsViewModel: GoalsViewModel,
    historyViewModel: HistoryViewModel,
    achievementsViewModel: AchievementsViewModel
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                navController = navController,
                currentRoute = currentRoute,
                closeDrawer = { scope.launch { drawerState.close() } }

            )
        }
    ) {
        Scaffold(
            topBar = {
                AppTopBar(
                    currentRoute = currentRoute,
                    onNavigationIconClick = { scope.launch { drawerState.open() } }
                )
            }
        ) { innerPadding ->
            AppNavigationHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                mainViewModel = mainViewModel,
                statisticsViewModel = statisticsViewModel,
                settingsViewModel = settingsViewModel,
                goalsViewModel = goalsViewModel,
                historyViewModel = historyViewModel,
                achievementsViewModel = achievementsViewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(currentRoute: String?, onNavigationIconClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(text = getTitleForScreen(currentRoute))
        },
        navigationIcon = {
            IconButton(onClick = onNavigationIconClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.nav_menu_description)
                )
            }
        }
    )
}

@Composable
fun AppDrawerContent(
    navController: NavController,
    currentRoute: String?,
    closeDrawer: () -> Unit
) {
    ModalDrawerSheet {
        Spacer(Modifier.height(12.dp))
        val menuItemsTop = listOf(
            Screen.MainScreen,
            Screen.HistoryScreen,
            Screen.StatisticsScreen,
            Screen.GoalsScreen,
            Screen.AchievementsScreen,
            Screen.SettingsScreen
        )
        val menuItemsBottom = listOf(Screen.DonateScreen, Screen.AboutScreen)
        menuItemsTop.forEach { screen ->
            val isSelected: Boolean
            val routeToNavigate: String

            if (screen is Screen.MainScreen) {
                isSelected = currentRoute == Screen.AddEntryChooserScreen.route || currentRoute?.startsWith(Screen.MainScreen.route) == true
                routeToNavigate = Screen.AddEntryChooserScreen.route
            } else {
                isSelected = currentRoute == screen.route
                routeToNavigate = screen.route
            }

            NavigationDrawerItem(
                icon = { Icon(getIconForScreen(screen.route), contentDescription = null) },
                label = { Text(getTitleForScreen(screen.route)) },
                selected = isSelected,
                onClick = {
                    navController.navigate(routeToNavigate) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    closeDrawer()
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
        Divider()
        menuItemsBottom.forEach { screen ->
            NavigationDrawerItem(
                icon = { Icon(getIconForScreen(screen.route), contentDescription = null) },
                label = { Text(getTitleForScreen(screen.route)) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    closeDrawer()
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}

@Composable
fun AppNavigationHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    statisticsViewModel: StatisticsViewModel,
    settingsViewModel: SettingsViewModel,
    goalsViewModel: GoalsViewModel,
    historyViewModel: HistoryViewModel,
    achievementsViewModel: AchievementsViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.AddEntryChooserScreen.route,
        modifier = modifier
    ) {
        composable(Screen.AddEntryChooserScreen.route) {
            AddEntryChooserScreen(navController = navController)
        }
        composable(
            route = "${Screen.MainScreen.route}/{entryType}",
            arguments = listOf(navArgument("entryType") { type = NavType.StringType })
        ) { backStackEntry ->
            val entryType = backStackEntry.arguments?.getString("entryType")
                ?.let { EntryType.valueOf(it) } ?: EntryType.SAVING
            MainScreen(
                viewModel = mainViewModel,
                entryType = entryType,
                navController = navController
            )
        }
        composable(Screen.StatisticsScreen.route) {
            StatisticsScreen(statisticsViewModel = statisticsViewModel, settingsViewModel = settingsViewModel)
        }
        composable(Screen.SettingsScreen.route) {
            SettingsScreen(viewModel = settingsViewModel)
        }
        composable(Screen.GoalsScreen.route) {
            GoalsListScreen(
                navController = navController,
                viewModel = goalsViewModel,
                settingsViewModel = settingsViewModel
            )
        }
        composable(Screen.AddGoalScreen.route) {
            AddGoalScreen(
                navController = navController,
                viewModel = goalsViewModel
            )
        }
        composable(Screen.HistoryScreen.route) {
            HistoryScreen(
                viewModel = historyViewModel,
                navController = navController,
                settingsViewModel = settingsViewModel
            )
        }
        composable(
            route = "${Screen.EditEntryScreen.route}/{entryId}",
            arguments = listOf(navArgument("entryId") { type = NavType.IntType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getInt("entryId")
            entryId?.let {
                EditEntryScreen(
                    navController = navController,
                    viewModel = historyViewModel,
                    entryId = it
                )
            }
        }
        composable(Screen.AchievementsScreen.route) {
            AchievementsScreen(viewModel = achievementsViewModel)
        }
        composable(Screen.DonateScreen.route) {
            DonateScreen()
        }
        composable(Screen.AboutScreen.route) { AboutScreen() }
    }
}

@Composable
fun getTitleForScreen(route: String?): String {
    return when (route) {
        Screen.MainScreen.route, Screen.AddEntryChooserScreen.route -> stringResource(id = R.string.screen_title_main)
        Screen.HistoryScreen.route -> stringResource(id = R.string.screen_title_history)
        Screen.StatisticsScreen.route -> stringResource(id = R.string.screen_title_statistics)
        Screen.GoalsScreen.route -> stringResource(id = R.string.screen_title_goals)
        Screen.SettingsScreen.route -> stringResource(id = R.string.screen_title_settings)
        Screen.AddGoalScreen.route -> stringResource(id = R.string.screen_title_add_goal)
        "edit_entry/{entryId}" -> stringResource(id = R.string.screen_title_edit_entry)
        Screen.AchievementsScreen.route -> stringResource(id = R.string.screen_title_achievements)
        Screen.DonateScreen.route -> stringResource(id = R.string.donate_nav_title)
        Screen.AboutScreen.route -> stringResource(id = R.string.about_nav_title)
        else -> ""
    }
}

@Composable
private fun getIconForScreen(route: String?): ImageVector {
    return when (route) {
        Screen.MainScreen.route, Screen.AddEntryChooserScreen.route -> Icons.Outlined.AddCircle
        Screen.HistoryScreen.route -> Icons.Outlined.History
        Screen.StatisticsScreen.route -> Icons.Outlined.ShowChart
        Screen.GoalsScreen.route -> Icons.Outlined.Flag
        Screen.SettingsScreen.route -> Icons.Outlined.Settings
        Screen.AchievementsScreen.route -> Icons.Outlined.EmojiEvents
        Screen.DonateScreen.route -> Icons.Outlined.VolunteerActivism
        Screen.AboutScreen.route -> Icons.Outlined.Info
        else -> Icons.Outlined.AddCircle // Default icon
    }
}