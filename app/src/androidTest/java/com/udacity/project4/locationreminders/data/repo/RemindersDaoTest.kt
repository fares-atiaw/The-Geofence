package com.udacity.project4.locationreminders.data.repo

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertNull
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest {        /** Test DAO (Data Access Object) and Repository classes **/

    //Subjects under test
    private lateinit var database: RemindersDatabase

    // Set the main coroutines dispatcher for unit testing.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDB() {
        // { Room.inMemoryDatabaseBuilder } => This DB instance let the stored information disappears when the process is killed
        database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                RemindersDatabase::class.java
            )
            .allowMainThreadQueries().build()
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
    fun getReminders_addData_CheckItsNullity() = runBlockingTest {
        val reminder1 = ReminderDTO("Title1", "Description1", "location1", 29.975507526586643, 31.40644697381402)
        val reminder2 = ReminderDTO("Title2", "Description2", "location2", 37.819927, -122.478256)
        val reminder3 = ReminderDTO("Title3", "Description3", "location3", 37.795490, -122.394276)

        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)
        database.reminderDao().saveReminder(reminder3)

        val remindersList = database.reminderDao().getReminders()

        assertThat(remindersList, `is`(notNullValue()))
    }

    @Test
    fun saveReminder_getReminderById_checkItsExistence() = runBlockingTest {
        val reminder = ReminderDTO("Place", "I'm there", "The place", 29.975507526586643, 31.40644697381402)

        database.reminderDao().saveReminder(reminder)
        val result = database.reminderDao().getReminderById(reminder.id)

        assertThat(result, notNullValue())
        assertThat(result?.id, `is`(reminder.id))
        assertThat(result?.title, `is`(reminder.title))
        assertThat(result?.description, `is`(reminder.description))
        assertThat(result?.location, `is`(reminder.location))
        assertThat(result?.latitude, `is`(reminder.latitude))
        assertThat(result?.longitude, `is`(reminder.longitude))
    }

    @Test
    fun getReminderById_wrongId_NotFound() = runBlockingTest {
        val reminderId = "4g5df4g35"

        val data = database.reminderDao().getReminderById(reminderId)

        assertNull(data)
    }

    @Test
    fun deleteAllReminders_addData_foundEmptyList() = runBlockingTest {
        val reminder1 = ReminderDTO("Title1", "Description1", "location1", 29.975507526586643, 31.40644697381402)
        val reminder2 = ReminderDTO("Title2", "Description2", "location2", 37.819927, -122.478256)
        val reminder3 = ReminderDTO("Title3", "Description3", "location3", 37.795490, -122.394276)

        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)
        database.reminderDao().saveReminder(reminder3)

        database.reminderDao().deleteAllReminders()
        val remindersList = database.reminderDao().getReminders()

        assertThat(remindersList, `is`(emptyList()))
    }



    @After
    fun closeDb() = database.close()
}