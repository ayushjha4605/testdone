package com.testdone.app.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    // gates
    const val SPLASH = "splash"
    const val SEED = "seed"
    const val AUTH = "auth"

    // main tabs
    const val HOME = "home"
    const val TESTS = "tests"
    const val ULTRA = "ultra"
    const val QBANK = "qbank"
    const val ANALYTICS = "analytics"

    // inner pages (full-screen within main scaffold)
    const val TEST_LIST = "testlist/{examId}"          // exam's mock/pyq list
    const val INSTRUCTIONS = "instructions/{examId}/{testId}"
    const val PROFILE = "profile"
    const val PLAN = "plan/{planId}"
    const val DOUBTS = "doubts"
    const val SUBMIT = "submit-questions"
    const val SETTINGS = "settings"
    const val CHANGE_PASSWORD = "change-password"
    const val FORGOT_PASSWORD = "forgot-password"

    // v2.3.14 — community & policy screens (route names = original release)
    const val ADMIN = "admin-review"
    const val PRIVACY = "privacy"

    // v2.3.15 — full About screen (menu About + Settings → TestDone)
    const val ABOUT = "about"

    // overlays (outside bottom bar)
    const val RUNNER = "runner/{examId}/{testId}"
    const val REPORT = "report/{attemptId}"

    fun testList(examId: String) = "testlist/$examId"
    fun instructions(examId: String, testId: String) = "instructions/$examId/$testId"
    fun plan(planId: String) = "plan/$planId"
    fun runner(examId: String, testId: String) = "runner/$examId/$testId"
    fun report(attemptId: String) = "report/$attemptId"
}

data class TabSpec(
    val route: String,
    val labelKey: String,
    val icon: ImageVector,
    val special: Boolean = false,
)

val BOTTOM_TABS = listOf(
    TabSpec(Routes.HOME, "home", Icons.Outlined.Home),
    TabSpec(Routes.TESTS, "tests", Icons.Outlined.FileCopy),
    TabSpec(Routes.ULTRA, "ultra", Icons.Rounded.AutoAwesome, special = true),
    TabSpec(Routes.QBANK, "qbank", Icons.Outlined.MenuBook),
    TabSpec(Routes.ANALYTICS, "analytics", Icons.Outlined.BarChart),
)
