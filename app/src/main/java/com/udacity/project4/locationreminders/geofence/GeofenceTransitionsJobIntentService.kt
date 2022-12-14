package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.JobIntentService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext

class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope {

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    companion object {
        private const val JOB_ID = 573
        internal const val ACTION_GEOFENCE_EVENT = "ACTION_GEOFENCE_EVENT"     // manual action filter
        private val TAG = "GeofenceTransitionsJob"

        // TODO Done: call this to start the JobIntentService to handle the geofencing transition events
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                context,
                GeofenceTransitionsJobIntentService::class.java,
                JOB_ID,
                intent
            )
        }
    }

    override fun onHandleWork(intent: Intent) {
        //TODO Done: handle the geofencing transition events and send a notification to the user when he enters the geofence area
        //TODO Done: call @sendNotification
        Log.e("Here1", "First mission!")
        handleGeofenceThenSendNotification(intent)
        Log.e("Here2", "Last mission!")
    }

    private fun handleGeofenceThenSendNotification(intent: Intent){
        if (intent.action == ACTION_GEOFENCE_EVENT) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent != null) {
                if (geofencingEvent.hasError()) {
                    val errorMessage = errorMessage(this@GeofenceTransitionsJobIntentService, geofencingEvent.errorCode)
                    Log.e(TAG, errorMessage)
                    Toast.makeText(this@GeofenceTransitionsJobIntentService, "Error : \n $errorMessage", Toast.LENGTH_SHORT).show()
                    return
                }


            if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                /*Log.v(TAG, this@GeofenceTransitionsJobIntentService.getString(R.string.geofence_entered))

                val fenceId = when {
                    geofencingEvent.triggeringGeofences!!.isNotEmpty() ->
                        geofencingEvent.triggeringGeofences!![0].requestId
                    else -> {
                        Log.e(TAG, "No Geofence Trigger Found! Abort mission!")
                        return
                    }
                }
                // Unknown Geofences aren't helpful to us
                if ( fenceId.isNullOrEmpty() ) {
                    Log.e(TAG, "Unknown Geofence: Abort Mission")
                    return
                }
                sendNotification(fenceId)*/
                geofencingEvent.triggeringGeofences?.let {
                    sendNotification(triggeringGeofences = it)
                }
            }
            }

        }
    }

    //TODO Done: get the request id of the current geofence
    private fun sendNotification(triggeringGeofences: List<Geofence>) {
        if (triggeringGeofences.isEmpty()) return

        // As it can be more than one place
        for (triggeringGeofence in triggeringGeofences) {
            val requestId = triggeringGeofence.requestId
            //Get the local repository instance
            val remindersLocalRepository: ReminderDataSource by inject()
//        Interaction to the repository has to be through a coroutine scope
            CoroutineScope(coroutineContext).launch(SupervisorJob()) {
                //get the reminder with the request id
                val result = remindersLocalRepository.getReminder(requestId)
                if (result is Result.Success<ReminderDTO>) {
                    val reminderDTO = result.data
                    //send a notification to the user with the reminder details
                    sendNotification(
                        this@GeofenceTransitionsJobIntentService, ReminderDataItem(
                            reminderDTO.title,
                            reminderDTO.description,
                            reminderDTO.location,
                            reminderDTO.latitude,
                            reminderDTO.longitude,
                            reminderDTO.id
                        )
                    )
                }
            }
        }
    }

        /** Returns the error string for a geofencing error code. **/
        private fun errorMessage(context: Context, errorCode: Int): String {
            val resources = context.resources
            return when (errorCode) {
                GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> resources.getString(
                    R.string.geofence_not_available
                )
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> resources.getString(
                    R.string.geofence_too_many_geofences
                )
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> resources.getString(
                    R.string.geofence_too_many_pending_intents
                )
                else -> resources.getString(R.string.geofence_unknown_error)
            }
        }

}
