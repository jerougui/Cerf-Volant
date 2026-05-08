package com.kilotracker.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.kilotracker.ui.CalibrationActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalibrationFlowTest {

    @get:Rule
    val activityRule = ActivityTestRule(CalibrationActivity::class.java)

    @Test
    fun calibration_displaysUI_andAllowsInput() {
        // TODO: Use Espresso to check UI elements present
        // onView(withId(R.id.distanceInput)).check(matches(isDisplayed()))
        // onView(withId(R.id.btnCalibrate)).check(matches(isDisplayed()))
    }

    @Test
    fun calibration_rejectsTooShortDistance() {
        // TODO: Enter 2m, click Calibrate, expect warning
    }
}
