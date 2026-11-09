package com.guerrerocarlos.offlinenotes

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class AppSmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun showsTestValueWarning() { rule.onNodeWithText("Research prototype. TEST VALUE ONLY.").assertExists() }
}
