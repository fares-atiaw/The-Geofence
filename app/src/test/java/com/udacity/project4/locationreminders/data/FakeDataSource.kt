package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import java.util.LinkedHashMap

/** Using FakeDataSource with static access to the data for easy testing. **/
class FakeDataSource : ReminderDataSource {

    var remindersList: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()

    private var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Error happened")
        }
        remindersList.let {
            return Result.Success(it.values.toList())
        }
//        return Result.Error("reminders not found")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersList[reminder.id] = reminder
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error("Error happened")
        }
        remindersList[id]?.let {
            return Result.Success(it)
        }
        return Result.Error("Error happened")
    }

    override suspend fun deleteAllReminders() {
        remindersList.clear()
    }

}