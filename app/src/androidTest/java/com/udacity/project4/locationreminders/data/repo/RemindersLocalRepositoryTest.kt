package com.udacity.project4.locationreminders.data.repo

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemindersLocalRepositoryTest {    /** Test DAO (Data Access Object) and Repository classes **/

    //Subjects under test
    private lateinit var database: RemindersDatabase
    private lateinit var remindersRepository: RemindersLocalRepository
    private lateinit var reminder : ReminderDTO

    // Set the main coroutines dispatcher for unit testing.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDBandRepository() {
        // { Room.inMemoryDatabaseBuilder } => This DB instance let the stored information disappears when the process is killed
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        remindersRepository = RemindersLocalRepository(
            database.reminderDao(),
            Dispatchers.Main
        )

        reminder = ReminderDTO("Place", "I'm there", "The place", 29.975507526586643, 31.40644697381402)
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
    fun saveReminder_getReminderById_checkItsExistence() = runBlocking {
        remindersRepository.saveReminder(reminder)
        val result = remindersRepository.getReminder(reminder.id) as Result.Success

        assertThat(result.data.title, `is`(reminder.title))
        assertThat(result.data.description, `is`(reminder.description))
        assertThat(result.data.latitude, `is`(reminder.latitude))
        assertThat(result.data.longitude, `is`(reminder.longitude))
        assertThat(result.data.location, `is`(reminder.location))
    }

    @Test
//    fun getReminder_wrongId_NotFound() = runBlocking {
    fun getReminder_deletedId_NotFound() = runBlocking {
//        val reminderId = "12pla34pla"
        remindersRepository.saveReminder(reminder)
        remindersRepository.deleteAllReminders()

        val result = remindersRepository.getReminder(reminder.id) as Result.Error

        assertThat(result.message, `is`("Reminder not found!"))
    }

    @Test
    fun deleteAllReminders_addData_foundEmptyList()= runBlocking {
        remindersRepository.saveReminder(reminder)
        remindersRepository.deleteAllReminders()

        val result = remindersRepository.getReminders() as Result.Success

        assertThat(result.data, `is` (emptyList()))
    }


    @After
    fun cleanUp() {
        database.close()
    }
}

/*
// The “as?” also allows the cast to return null when the left-hand side is null.
        assertThat(result is Result.Success, `is`(true))
        // Then it's absolutely Result.Success
        result as Result.Success
        */