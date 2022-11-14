package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.R
import com.udacity.project4.getOrAwaitValue
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers.`is`
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    //Subjects under test
    private lateinit var remindersRepository: FakeDataSource
    private lateinit var savedReminderViewModel: SaveReminderViewModel
    private lateinit var reminder: ReminderDataItem

    // Set the main coroutines dispatcher for unit testing.
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()
    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()


    @Before
    fun setupViewModel() {
        remindersRepository = FakeDataSource()
        savedReminderViewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), remindersRepository)
        reminder = ReminderDataItem("Title", "Description", "Location", 29.975507526586643, 31.40644697381402)
    }


    /**
    @Test
    fun subjectUnderTest_actionOrInputCase_resultState () {
    // 1- Given (parameter(s) or data to be used)
    ... .. .. ...
    ... .. .. ...

    // 2- Call the function with the given data (inside a result variable)
    val result = actualFunction(parameter)

    // 3- Check the result using assertions [assertEquals () / assertThat () / .. ]
    assertThat(expected , actual/result)
     **/

    @Test
    fun validateEnteredData_emptyTitle_returnFalse() {
//        reminder.title = null     //or
        reminder.title = ""

        assertThat(savedReminderViewModel.validateEnteredData(reminder), `is`(false))
        assertThat(savedReminderViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
    }
    @Test
    fun validateEnteredData_emptyLocation_returnFalse() {
//        reminder.location = null     //or
        reminder.location = ""

        assertThat(savedReminderViewModel.validateEnteredData(reminder), `is`(false))
        assertThat(savedReminderViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_select_location))
    }

    @Test
    fun saveReminder_dataSavingProcess_LoadingState() {
        // Pause dispatcher so we can verify initial values
        mainCoroutineRule.pauseDispatcher()
        savedReminderViewModel.saveReminder(reminder)

        assertThat(savedReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions
        mainCoroutineRule.resumeDispatcher()

        assertThat(savedReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun saveReminder_afterDataSaving_showToast() {
        savedReminderViewModel.saveReminder(reminder)

        val result = (ApplicationProvider.getApplicationContext() as Application).getString(R.string.reminder_saved)

        assertEquals(savedReminderViewModel.showToast.getOrAwaitValue(), result)
    }


    @After
    fun stop_Koin() {
        stopKoin()
    }
}