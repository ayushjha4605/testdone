package com.testdone.app.ui

import android.app.Activity
import android.media.AudioAttributes
import android.media.SoundPool

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.testdone.app.data.content.ContentSeeder
import com.testdone.app.di.AppContainer
import com.testdone.app.ui.components.TdButton
import com.testdone.app.ui.components.ToastHost
import com.testdone.app.ui.components.pressableScale
import com.testdone.app.ui.menu.MenuDrawer
import com.testdone.app.ui.nav.BOTTOM_TABS
import com.testdone.app.ui.nav.Routes
import com.testdone.app.ui.splash.CinematicSplash
import com.testdone.app.ui.theme.TdExt
import com.testdone.app.R
import androidx.compose.material.icons.rounded.Menu
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

/** Root of the whole app: gates (splash/seed/auth) + main scaffold + toasts. */
@Composable
fun TestDoneAppRoot(
    container: AppContainer,
) {
    val vm: AppViewModel = viewModel(factory = simpleFactory { AppViewModel(container) })
    val phase by vm.phase.collectAsState()

    val theme by vm.theme.collectAsState()

    // v2.3.16 — force-update gate: when Remote Config demands a newer build,
    // the whole app is replaced by the update screen (non-dismissable).
    val forceUpdate by vm.forceUpdate.collectAsState()

    com.testdone.app.ui.theme.TestDoneTheme(darkTheme = theme == com.testdone.app.data.local.prefs.ThemeMode.DARK) {
        // status/nav bar icon color follows the APP theme (not system theme)
        SystemBarsAppearance(dark = theme == com.testdone.app.data.local.prefs.ThemeMode.DARK)
        if (forceUpdate != null) {
            com.testdone.app.ui.gates.ForceUpdateScreen(info = forceUpdate!!)
        } else {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (phase) {
                AppViewModel.Phase.SPLASH -> {
                    SplashOpenSound(vm)
                    CinematicSplash(onDone = { vm.onSplashFinished() })
                }
                AppViewModel.Phase.SEEDING -> {
                    val seedState by vm.seedState().collectAsState()
                    SeedGate(seedState = seedState, onRetry = { vm.retrySeed() })
                }
                AppViewModel.Phase.AUTH -> com.testdone.app.ui.auth.AuthFlow(vm = vm)
                AppViewModel.Phase.MAIN -> MainAppScaffold(vm = vm)
            }

            // global toasts
            val toast by vm.toast.collectAsState()
            Box(Modifier.align(Alignment.TopCenter)) {
                ToastHost(toast = toast, onDismiss = { vm.dismissToast() })
            }
        }
        }
    }
}

/**
 * Keeps system bar icon color in sync with the APP theme: light theme gets
 * dark icons, dark theme gets light ones (enableEdgeToEdge alone follows the
 * SYSTEM theme, which breaks when the two disagree).
 */
@Composable
private fun SystemBarsAppearance(dark: Boolean) {
    val view = LocalView.current
    androidx.compose.runtime.DisposableEffect(dark) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
        onDispose {}
    }
}

/**
 * Plays the soft two-tone app-open chime alongside the splash logo reveal.
 * Respects the "App Sounds" setting (Settings → Appearance); SoundPool is
 * released as soon as the splash leaves composition. The 110ms lead delay
 * lines the first note up with the logo zoom-settle.
 */
@Composable
private fun SplashOpenSound(vm: AppViewModel) {
    val context = LocalContext.current
    val soundPool = remember {
        SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
    }
    DisposableEffect(Unit) {
        onDispose { soundPool.release() }
    }
    LaunchedEffect(Unit) {
        val enabled = vm.container.settings.sounds.firstOrNull() ?: true
        if (!enabled) return@LaunchedEffect
        val loaded = CompletableDeferred<Boolean>()
        soundPool.setOnLoadCompleteListener { _, _, status -> loaded.complete(status == 0) }
        val id = soundPool.load(context, R.raw.splash_open, 1)
        delay(110) // sync first note with the logo reveal
        val ok = withTimeoutOrNull(1200) { loaded.await() } ?: false
        if (ok) soundPool.play(id, 0.8f, 0.8f, 1, 0, 1f)
    }
}

/** First-launch content seeding progress screen. */
@Composable
private fun SeedGate(seedState: ContentSeeder.SeedState, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        val label = when (seedState) {
            is ContentSeeder.SeedState.Seeding -> "Preparing your exam library\n${seedState.done}/${seedState.total} exams"
            is ContentSeeder.SeedState.Failed -> "Setup failed at ${seedState.examId}"
            else -> "Preparing your exam library…"
        }
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        androidx.compose.foundation.layout.Spacer(Modifier.padding(20.dp))
        when (seedState) {
            is ContentSeeder.SeedState.Seeding -> {
                val progress = if (seedState.total == 0) 0f else seedState.done.toFloat() / seedState.total
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                )
            }
            is ContentSeeder.SeedState.Failed -> TdButton("Retry", onClick = onRetry)
            else -> androidx.compose.material3.CircularProgressIndicator()
        }
    }
}

// ── main scaffold ────────────────────────────────────────────────────────────

@Composable
fun MainAppScaffold(vm: AppViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // Bottom bar stays on every tab screen but HIDES on the immersive runner &
    // report AND on the focused pre-test flow (v2.3.15: instructions' Start Test
    // button kept getting half-covered by the bar on 3-button-nav devices —
    // "start test aadha chup jata hai"). Same treatment as the runner: the
    // screen owns its full height, the button clears the SYSTEM nav bar.
    val bottomBarVisible = currentRoute != null &&
        currentRoute != Routes.RUNNER && currentRoute != Routes.REPORT &&
        currentRoute != Routes.INSTRUCTIONS

    // Which tab should appear highlighted on the current screen:
    // inner pages inherit their parent tab (test list & instructions → Tests).
    val activeTab = when (currentRoute) {
        Routes.HOME -> Routes.HOME
        Routes.TESTS, Routes.TEST_LIST, Routes.INSTRUCTIONS -> Routes.TESTS
        Routes.ULTRA -> Routes.ULTRA
        Routes.QBANK -> Routes.QBANK
        Routes.ANALYTICS -> Routes.ANALYTICS
        else -> null
    }

    var menuOpen by remember { mutableStateOf(false) }
    val language by vm.language.collectAsState()
    val t = remember(language) { { key: String -> vm.container.i18n.t(key) } }

    // Provider wraps the WHOLE scaffold — every screen's header menu-button
    // reads this (previously it wrapped only the drawer itself, so the
    // MenuButton got a no-op lambda and tapping it did nothing).
    androidx.compose.runtime.CompositionLocalProvider(LocalMenuOpener provides { menuOpen = true }) {
        Box(Modifier.fillMaxSize()) {
            NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            // Global IME handling: every screen's content lifts above the keyboard,
            // so focused text fields (early-access email, login, doubts, submit…)
            // are never hidden under Gboard. Screens scroll the focused field into
            // the remaining viewport automatically.
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            // v2.3.15 — REAL, readable transitions. The old it/14 slide (~25dp)
            // read as "kuch hota hi nahi". Now each navigation class moves like
            // its Material role: tab switches crossfade+scale, pushes slide in
            // from the end quarter, profile/about arrive as modals (scale-up),
            // runner/report rise from the bottom (immersive takeover).
            enterTransition = {
                val target = targetState.destination.route
                val from = initialState.destination.route
                val tabRoutes = BOTTOM_TABS.map { it.route }.toSet()
                when {
                    target == Routes.RUNNER || target == Routes.REPORT ->
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Up,
                            tween(360, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        ) { it } + fadeIn(tween(280))
                    target == Routes.PROFILE || target == Routes.ABOUT ->
                        fadeIn(tween(240)) + scaleIn(
                            animationSpec = tween(320, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                            initialScale = 0.92f,
                        )
                    from in tabRoutes && target in tabRoutes ->
                        fadeIn(tween(210)) + scaleIn(
                            animationSpec = tween(260, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                            initialScale = 0.97f,
                        )
                    else ->
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Start,
                            tween(320, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        ) { it / 4 } + fadeIn(tween(240))
                }
            },
            exitTransition = {
                val target = targetState.destination.route
                if (target == Routes.RUNNER || target == Routes.REPORT) {
                    fadeOut(tween(200))
                } else {
                    fadeOut(tween(190)) + scaleOut(
                        animationSpec = tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        targetScale = 0.98f,
                    )
                }
            },
            popEnterTransition = {
                fadeIn(tween(230)) + scaleIn(
                    animationSpec = tween(280, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    initialScale = 0.98f,
                )
            },
            popExitTransition = {
                // NOTE: in pop-exit the LEAVING screen is `initialState`
                // (targetState is where the back-nav is headed).
                val leaving = initialState.destination.route
                when {
                    leaving == Routes.RUNNER || leaving == Routes.REPORT ->
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Down,
                            tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        ) { it } + fadeOut(tween(220))
                    leaving == Routes.PROFILE || leaving == Routes.ABOUT ->
                        scaleOut(
                            animationSpec = tween(260, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                            targetScale = 0.92f,
                        ) + fadeOut(tween(200))
                    else ->
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.End,
                            tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        ) { it / 4 } + fadeOut(tween(200))
                }
            },
        ) {
            composable(Routes.HOME) { com.testdone.app.ui.home.HomeScreen(navController, vm) }
            composable(Routes.TESTS) { com.testdone.app.ui.tests.TestsScreen(navController, vm) }
            composable(Routes.ULTRA) { com.testdone.app.ui.ultra.UltraScreen(navController, vm) }
            composable(Routes.QBANK) { com.testdone.app.ui.qbank.QBankScreen(navController, vm) }
            composable(Routes.ANALYTICS) { com.testdone.app.ui.analytics.AnalyticsScreen(navController, vm) }
            composable(Routes.TEST_LIST) { entry ->
                com.testdone.app.ui.tests.TestListScreen(navController, vm, entry.arguments?.getString("examId") ?: "")
            }
            composable(Routes.INSTRUCTIONS) { entry ->
                com.testdone.app.ui.tests.InstructionsScreen(
                    navController, vm,
                    entry.arguments?.getString("examId") ?: "",
                    entry.arguments?.getString("testId") ?: "",
                )
            }
            composable(Routes.RUNNER) { entry ->
                com.testdone.app.ui.runner.TestRunnerScreen(
                    navController, vm,
                    entry.arguments?.getString("examId") ?: "",
                    entry.arguments?.getString("testId") ?: "",
                )
            }
            composable(Routes.REPORT) { entry ->
                com.testdone.app.ui.runner.ReportScreen(
                    navController, vm,
                    entry.arguments?.getString("attemptId") ?: "",
                )
            }
            composable(Routes.PROFILE) { com.testdone.app.ui.profile.ProfileScreen(navController, vm) }
            composable(Routes.PLAN) { entry ->
                com.testdone.app.ui.plan.PlanScreen(navController, vm, entry.arguments?.getString("planId") ?: "testdone-pass")
            }
            composable(Routes.DOUBTS) { com.testdone.app.ui.doubts.DoubtsScreen(navController, vm) }
            composable(Routes.SUBMIT) { com.testdone.app.ui.submit.SubmitQuestionScreen(navController, vm) }
            composable(Routes.SETTINGS) { com.testdone.app.ui.settings.SettingsScreen(navController, vm) }
            composable(Routes.CHANGE_PASSWORD) { com.testdone.app.ui.password.ChangePasswordScreen(navController, vm) }
            composable(Routes.FORGOT_PASSWORD) { com.testdone.app.ui.password.ForgotPasswordScreen(navController, vm) }
            // v2.3.14 — admin moderation console + in-app privacy policy
            composable(Routes.ADMIN) { com.testdone.app.ui.admin.AdminReviewScreen(navController, vm) }
            composable(Routes.PRIVACY) { com.testdone.app.ui.settings.PrivacyScreen(navController, vm) }
            // v2.3.15 — full About screen (menu About + Settings → TestDone)
            composable(Routes.ABOUT) { com.testdone.app.ui.settings.AboutScreen(navController, vm) }
        }

        // bottom navigation — visible on EVERY screen except the immersive
        // runner & report, and it rides ABOVE the open keyboard (imePadding)
        // so Home is always reachable. Previously the bar hid while the IME
        // was visible; on devices with stale IME insets it never came back
        // until a back-press — the "stuck on Tests, Home tab dead" bug.
        AnimatedVisibility(
            visible = bottomBarVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding(),
            enter = slideInVertically(tween(260)) { it },
            exit = slideOutVertically(tween(200)) { it },
        ) {
            BottomNavBar(navController = navController, t = t, activeTab = activeTab)
        }

        // menu drawer — v2.3.15: always composed; MenuDrawer runs its own
        // enter/exit choreography (scrim fade + panel slide from the left).
        // Previously `if (menuOpen)` popped it in and out with NO animation,
        // which read as abrupt next to everything else.
        MenuDrawer(
            navController = navController,
            vm = vm,
            open = menuOpen,
            onClose = { menuOpen = false },
        )
        }
    }
}

/** Reusable menu-button that every tab header shows. */
@Composable
fun MenuButton(onOpen: () -> Unit, onGradient: Boolean = false) {
    Box(
        Modifier
            .size(42.dp)
            .pressableScale(0.9f, onOpen)
            .background(
                if (onGradient) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(14.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            androidx.compose.material.icons.Icons.Rounded.Menu,
            contentDescription = "Open menu",
            tint = if (onGradient) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** The 5-tab bottom bar with animated pill indicator + springy icons. */
@Composable
private fun BottomNavBar(
    navController: NavHostController,
    t: (String) -> String,
    activeTab: String?,
) {
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val hairline = MaterialTheme.colorScheme.outlineVariant

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .drawBehind {
                // hairline separating bar from content (definition on light bg)
                drawLine(
                    color = hairline,
                    strokeWidth = 1f,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                )
            }
            .navigationBarsPadding()
            .padding(top = 10.dp, bottom = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BOTTOM_TABS.forEach { tab ->
                val active = activeTab == tab.route
                Box(
                    Modifier
                        .weight(1f)
                        .pressableScale(0.92f) {
                            // dismiss any open keyboard first — otherwise the
                            // IME keeps the bar hidden and the tap looks dead
                            keyboard?.hide()
                            navController.navigateToTab(tab.route)
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = active,
                        modifier = Modifier.align(Alignment.Center),
                        enter = fadeIn(tween(200)) + slideInVertically(tween(240)) { it / 2 },
                        exit = fadeOut(tween(150)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.82f)
                                .height(46.dp)
                                .background(
                                    if (tab.special) androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.20f),
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                                        ),
                                    ) else androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
                                        ),
                                    ),
                                    RoundedCornerShape(16.dp),
                                ),
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // springy icon — bounces up when the tab activates
                        val iconScale by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (active) 1.16f else 1f,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = 0.45f,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
                            ),
                            label = "tabIconScale",
                        )
                        val iconOffset by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (active) -2f else 0f,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = 0.5f,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
                            ),
                            label = "tabIconOffset",
                        )
                        androidx.compose.material3.Icon(
                            tab.icon,
                            contentDescription = t(tab.labelKey),
                            tint = if (active) {
                                if (tab.special) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                            } else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(22.dp)
                                .scale(iconScale)
                                .offset(y = iconOffset.dp),
                        )
                        androidx.compose.animation.AnimatedVisibility(
                            visible = active,
                            enter = fadeIn(tween(180)) + androidx.compose.animation.expandVertically(
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = 0.6f,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
                                ),
                            ),
                            exit = fadeOut(tween(120)) + androidx.compose.animation.shrinkVertically(tween(140)),
                        ) {
                            Text(
                                t(tab.labelKey),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    if (route == Routes.HOME) {
        // Home = hard reset: clears the entire back stack and lands on a
        // fresh Home entry — works from ANY screen (Tests, test list,
        // instructions…), with no save/restore edge cases.
        navigate(route) {
            popUpTo(graph.findStartDestination().id) { inclusive = true }
            launchSingleTop = true
        }
    } else {
        navigate(route) {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

/** CompositionLocal that lets any tab header open the menu drawer. */
val LocalMenuOpener = androidx.compose.runtime.staticCompositionLocalOf<() -> Unit> { {} }

/** Minimal factory helper (avoids duplicating viewModelFactory boilerplate). */
@Suppress("UNCHECKED_CAST")
fun <T : androidx.lifecycle.ViewModel> simpleFactory(create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <U : androidx.lifecycle.ViewModel> create(modelClass: Class<U>): U = create() as U
    }
