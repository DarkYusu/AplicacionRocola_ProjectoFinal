package com.aplicaciones_android.aplicacionrocola_projectofinal

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aplicaciones_android.aplicacionrocola_projectofinal.ui.admin.ManageDishesActivity
import org.hamcrest.CoreMatchers.containsString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManageDishesActivityTest {
    @get:Rule
    val scenarioRule = ActivityScenarioRule(ManageDishesActivity::class.java)

    @Test
    fun showsEmptyStateInitially() {
        onView(withId(R.id.dish_list_empty))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun saveWithoutDataShowsValidationMessage() {
        onView(withId(R.id.save_dish_button)).perform(click())
        onView(withId(R.id.dish_form_status))
            .check(matches(withText(containsString("Completa"))))
    }
}
