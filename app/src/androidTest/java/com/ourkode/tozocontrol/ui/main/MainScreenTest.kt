package com.ourkode.tozocontrol.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/** UI tests for [com.ourkode.tozocontrol.ui.main.MainScreen]. */
class MainScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun appHeader_displaysTozoBrand() {
        composeTestRule.setContent {
            MainScreen()
        }
        composeTestRule.onNodeWithText("TOZO").assertExists()
    }
}
