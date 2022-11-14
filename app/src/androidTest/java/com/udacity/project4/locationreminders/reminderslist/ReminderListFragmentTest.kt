package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.get
import org.mockito.Mockito.mock
import org.koin.test.AutoCloseKoinTest
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@MediumTest
class ReminderListFragmentTest: AutoCloseKoinTest() {

    // Subjects under test
    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @get:Rule
    val instantExecutor = InstantTaskExecutorRule()

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

    /*/**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }*/

//    val reminder1 = ReminderDTO("Title1", "Description1", "location1", 29.975507526586643, 31.40644697381402)
//    val reminder2 = ReminderDTO("Title2", "Description2", "location2", 37.819927, -122.478256)
//    val reminder3 = ReminderDTO("Title3", "Description3", "location3", 37.795490, -122.394276)
    @Test
    fun remindersList_clickFAB_navigateToSaveReminderFragment () {
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        // So, now the fragment scenario is using the navController to be ready for testing.

        onView(withId(R.id.addReminderFAB)).perform(click())
        // check that after clicking { R.id.addReminderFAB }, the screen navigated
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }

    @Test
    fun remindersList_addData_listExampleDisplayed(): Unit = runBlocking {
        val reminder = ReminderDTO("Title1", "Description1", "location1", 29.975507526586643, 31.40644697381402)

        repository.saveReminder(reminder)

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // check that { R.id.remindersRecyclerView } is showing that data
        onView(withId(R.id.reminderssRecyclerView))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText(reminder.title))
                )
            )
    }

    @Test
    fun remindersList_noData_foundEmptyWithIconDisplayed(): Unit = runBlocking {
        val reminder = ReminderDTO("Title1", "Description1", "location1", 29.975507526586643, 31.40644697381402)

        repository.saveReminder(reminder)
        repository.deleteAllReminders()

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // check that { @drawable/ic_no_data } is shown
        onView(withId(R.id.noDataTextView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun remindersList_logoutMenu_displayed() = runBlocking {
        // Start up the main screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.logout)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        activityScenario.close()
    }

    /*/**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }*/
}