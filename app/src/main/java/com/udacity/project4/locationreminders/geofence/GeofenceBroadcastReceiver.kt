package com.udacity.project4.locationreminders.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R

/**
 * Triggered by the Geofence.  Since we can have many Geofences at once, we pull the request
 * ID from the first Geofence, and locate it within the cached data in our Room DB
 *
 * Or users can add the reminders and then close the app, So our app has to run in the background
 * and handle the geofencing in the background.
 * To do that you can use https://developer.android.com/reference/android/support/v4/app/JobIntentService to do that.
 *
 */

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event : GeofencingEvent? = GeofencingEvent.fromIntent(intent)
        if (event != null) {
            if (!( event.hasError() && event.errorCode == GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE )) {
                //TODO Done: implement the onReceive method to receive the geofencing events at the background
                GeofenceTransitionsJobIntentService.enqueueWork(context, intent)
            }
            else
                Toast.makeText(context, R.string.geofences_not_added, Toast.LENGTH_LONG).show()
        }


    }
}