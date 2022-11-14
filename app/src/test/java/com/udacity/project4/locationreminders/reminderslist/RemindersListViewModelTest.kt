package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.getOrAwaitValue
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import org.junit.Assert.*
import org.hamcrest.CoreMatchers.`is`
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest{

    // Subjects under test
    private lateinit var remindersListViewModel: RemindersListViewModel
    private lateinit var fakeDataSource: FakeDataSource

    // Set the main coroutines dispatcher for unit testing.
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()
    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // fakeDataSource & remindersListViewModel => Declared and ready for testing
    @Before
    fun setupViewModel() = runBlockingTest {
        fakeDataSource = FakeDataSource()
        val reminder1 = ReminderDTO("Title1", "Description1", "location1", 29.975507526586643, 31.40644697381402)
        val reminder2 = ReminderDTO("Title2", "Description2", "location2", 37.819927, -122.478256)
        val reminder3 = ReminderDTO("Title3", "Description3", "location3", 37.795490, -122.394276)
        fakeDataSource.saveReminder(reminder1)
        fakeDataSource.saveReminder(reminder2)
        fakeDataSource.saveReminder(reminder3)

        remindersListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
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
     assertEquals(expected , actual/result)
     **/

    @Test
    fun loadReminders_checkLoadingState() {
        // Pause dispatcher so we can verify initial values
        mainCoroutineRule.pauseDispatcher()
        // Load reminders
        remindersListViewModel.loadReminders()

        // Then progress indicator is shown
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions
        mainCoroutineRule.resumeDispatcher()

        // Then progress indicator is hidden
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadReminders_checkIfEmpty() = runBlockingTest {
        // Deleting data then loading the empty data
        fakeDataSource.deleteAllReminders()
        remindersListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
        remindersListViewModel.loadReminders()

        // check that { @drawable/ic_no_data } is shown
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun loadReminders_preformingError_SnackBarState() {
        // Preforming error
        fakeDataSource.setReturnError(true)

        mainCoroutineRule.pauseDispatcher()
        remindersListViewModel.loadReminders()
        mainCoroutineRule.resumeDispatcher()

        // check preforming note without crashing
        assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue()).isEqualTo("Error happened")
    }


    @After
    fun stop_Koin() {
        stopKoin()
    }
}