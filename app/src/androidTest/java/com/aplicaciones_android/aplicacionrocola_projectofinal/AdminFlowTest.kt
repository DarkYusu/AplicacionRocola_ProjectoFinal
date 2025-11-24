package com.aplicaciones_android.aplicacionrocola_projectofinal

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aplicaciones_android.aplicacionrocola_projectofinal.ui.admin.AdminActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdminFlowTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(AdminActivity::class.java)

    @Test
    fun adminScreenShowsManageButton() {
        onView(withId(R.id.manage_dishes_button))
            .check(matches(isDisplayed()))
            .perform(click())
    }
}
