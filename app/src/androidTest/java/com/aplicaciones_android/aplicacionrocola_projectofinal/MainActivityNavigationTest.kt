package com.aplicaciones_android.aplicacionrocola_projectofinal

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityNavigationTest {
    @get:Rule
    val scenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun bottomNavNavigatesToMenu() {
        onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()))
        onView(withId(R.id.nav_menu)).perform(click())
        onView(withId(R.id.menu_recycler)).check(matches(isDisplayed()))
    }

    @Test
    fun bottomNavNavigatesToSearch() {
        onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()))
        onView(withId(R.id.nav_buscar)).perform(click())
        onView(withId(R.id.search_results)).check(matches(isDisplayed()))
    }

    @Test
    fun bottomNavNavigatesToPlaylist() {
        onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()))
        onView(withId(R.id.nav_playlist)).perform(click())
        onView(withId(R.id.playlist_empty)).check(matches(isDisplayed()))
    }

    @Test
    fun bottomNavNavigatesToHome() {
        onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()))
        onView(withId(R.id.nav_inicio)).perform(click())
        onView(withId(R.id.recommended_recycler)).check(matches(isDisplayed()))
    }
}
