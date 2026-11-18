package com.parcelinbox.app

import android.content.Context
import android.content.Intent
import android.view.WindowManager
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun resetLocalSettings() {
        context.getSharedPreferences("local_settings", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("capture_diagnostics", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun firstLaunchReachesWelcomeScreen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitUntil(timeoutMillis = 7_000) {
                composeRule.onAllNodesWithText("开始使用").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("开始使用").assertExists()
        }
    }

    @Test
    fun parcelContentIsProtectedFromScreenshots() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(
                    activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
                )
            }
        }
    }

    @Test
    fun sharedSensitiveTextIsRejectedLocally() {
        context.getSharedPreferences("local_settings", Context.MODE_PRIVATE).edit()
            .putBoolean("onboarding_complete", true)
            .putBoolean("permission_intro_complete", true)
            .putString("app_language", "ENGLISH")
            .commit()
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(
                Intent.EXTRA_TEXT,
                "Amazon Order details\nOrder # ABCD123456\nCheckout payment\nPayment password"
            )

        ActivityScenario.launch<MainActivity>(intent).use {
            composeRule.waitUntil(timeoutMillis = 7_000) {
                composeRule.onAllNodesWithText("Import a parcel").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Import locally").performClick()
            composeRule.onNodeWithText(
                "Payment, password or verification content was detected and rejected. Share order or delivery details only."
            ).assertExists()
        }
    }
}
