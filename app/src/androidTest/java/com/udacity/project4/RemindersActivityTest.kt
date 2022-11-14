package com.udacity.project4

import android.Manifest
import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.Root
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import androidx.test.espresso.action.ViewActions
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule

@RunWith(AndroidJUnit4::class)
@LargeTest      /** Finally, end-to-End test & Snackbar and Toast messages are shown on the screen in many scenarios. **/
class RemindersActivityTest : AutoCloseKoinTest() {

    // Subjects under test
    private lateinit var repository: ReminderDataSource
//    val viewModel : SaveReminderViewModel = get()
    private lateinit var appContext: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
    )

    @Before
    // Using Service Locator pattern by Koin
    fun init() {
        stopKoin()  //stop the original one if it's still running
        appContext = getApplicationContext()

        val myModule = module {
            viewModel {
                RemindersListViewModel(app = appContext, dataSource = get() as ReminderDataSource)
            }

            single {
                SaveReminderViewModel(app = appContext, dataSource =  get() as ReminderDataSource)
            }
            single {
                RemindersLocalRepository(remindersDao = get()) as ReminderDataSource
            }
            single {
                LocalDB.createRemindersDao(context = appContext)
            }
        }

        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }

        // Start fresh
        runBlocking {
            repository = get()  as ReminderDataSource
            repository.deleteAllReminders()
        }
    }

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }


    /**
    @Test
    fun subjectUnderTest_actionOrInputCase_resultState () {
    // 1- Given (parameter(s) or data to be used)
    ... .. .. ...
    ... .. .. ...

    // 2- Call the function with the given data (inside a result variable)
    val result = actualFunction(parameter)

    // 3- Perform action(s) on view(s) then check the response
    onView(..).perform(..)
    onView(..).check(..)

     // 4- Close the scenario
     **/

    @Test
    fun addReminder_testActivityScenario() {
        runBlocking { repository.deleteAllReminders() }

        // Start up list screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(click())

        onView(withId(R.id.reminderTitle)).perform(typeText("Title"))
        onView(withId(R.id.reminderDescription)).perform(typeText("Description"))

        closeSoftKeyboard()
        /** For the map selection process **/
//  You can use this..
        // val viewModel : SaveReminderViewModel = get()
        // viewModel.testingNow = true
// or this..
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.map)).check(matches(isDisplayed()))
        onView(withId(R.id.map)).perform(longClick())
        onView(withId(R.id.btn_okay)).perform(click())
        /***/

        onView(withId(R.id.saveReminder)).perform(click())

        // check the [ R.layout.it_reminder ] values
        onView(withText("Title")).check(matches(isDisplayed()))
        onView(withText("Description")).check(matches(isDisplayed()))

        activityScenario.close()
    }

    @Test
    fun saveReminderScreen_showSnackBarTitleError() {

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())

        val snackBarMessage = appContext.getString(R.string.err_enter_title)
        onView(withText(snackBarMessage)).check(matches(isDisplayed()))

        activityScenario.close()
    }

    @Test
    fun saveReminderScreen_showSnackBarDescriptionError() {

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText("Title"))
        closeSoftKeyboard()
        onView(withId(R.id.saveReminder)).perform(click())

        val snackBarMessage = appContext.getString(R.string.err_enter_description)
        onView(withText(snackBarMessage)).check(matches(isDisplayed()))

        activityScenario.close()
    }

    @Test
    fun saveReminderScreen_showSnackBarLocationError() {

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText("Title"))
        onView(withId(R.id.reminderDescription)).perform(typeText("Description"))
        closeSoftKeyboard()
        onView(withId(R.id.saveReminder)).perform(click())

        val snackBarMessage = appContext.getString(R.string.err_select_location)
        onView(withText(snackBarMessage)).check(matches(isDisplayed()))

        activityScenario.close()
    }

    @Test
    fun saveReminderScreen_showToastMessage() {

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        lateinit var activity: Activity
        activityScenario.onActivity {
            activity = it
        }

        onView(withId(R.id.addReminderFAB)).perform(click())

        onView(withId(R.id.reminderTitle)).perform(typeText("Title"))

        closeSoftKeyboard()

        onView(withId(R.id.reminderDescription)).perform(typeText("Description"))

        closeSoftKeyboard()

        /** For the map selection process **/
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.map)).perform(longClick())
        onView(withId(R.id.btn_okay)).perform(click())
        /***/

        onView(withId(R.id.saveReminder)).perform(click())

        // Through the activity's screen, you can see what can be displayed
//        onView(withId(R.string.reminder_saved))
//            .inRoot(withDecorView(
//                not(`is`((activity).window.decorView)))
//            )
//            .check(matches(isDisplayed()))
        onView(withText(R.string.reminder_saved))
            .inRoot(ToastMatcher().apply {
                matches(isDisplayed())
            });

        activityScenario.close()
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    class ToastMatcher : TypeSafeMatcher<Root?>()
    {
        override fun matchesSafely(item: Root?): Boolean {
            val type: Int? = item?.windowLayoutParams?.get()?.type
            if (type == WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW) {
                val windowToken: IBinder = item.decorView.windowToken
                val appToken: IBinder = item.decorView.applicationWindowToken
                if (windowToken === appToken) { // means this window isn't contained by any other windows.
                    return true
                }
            }
            return false
        }

        override fun describeTo(description: Description?) {
            description?.appendText("is toast")
        }
    }
}
