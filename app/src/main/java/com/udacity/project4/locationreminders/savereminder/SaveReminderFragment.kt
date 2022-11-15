package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class SaveReminderFragment : BaseFragment() {   /**Using the view-model variables**/
    private val TAG = SaveReminderFragment::class.java.simpleName
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var binding: FragmentSaveReminderBinding
    lateinit var reminderData : ReminderDataItem
    private lateinit var geofencingClient: GeofencingClient
    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    private var PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    private val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
    private val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
    private val LOCATION_PERMISSION_INDEX_COARSE = 1
    private val BACKGROUND_LOCATION_PERMISSION_INDEX = 2
    private val REQUEST_TURN_DEVICE_LOCATION_ON = 29
    private var firstRequest = false

    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        try {
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        catch (_:Exception){
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        _viewModel.selectedPOI.observe(viewLifecycleOwner) {
            if (it != null) {
                _viewModel.reminderSelectedLocationStr.value = it.name
                _viewModel.latitude.value = it.latLng.latitude
                _viewModel.longitude.value = it.latLng.longitude
            }
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            reminderData = ReminderDataItem(
                title = title,
                id = _viewModel.selectedPOI.value?.placeId ?: "$latitude,$longitude",
                description = description,
                location = location,
                latitude = latitude,
                longitude = longitude
            )

//            TODO Done: use the user entered reminder details to:
//             Done 1) add a geofencing request
//             Done 2) save the reminder to the local db
            if (_viewModel.validateEnteredData(reminderData)){
                checkPermissionsThenStartGeofence()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(reminderData : ReminderDataItem, circularRegion : Float = 100f)
    {
        /** 1- You got the current GeofenceData from the parameter**/

        /** 2- Build the Geofence Object **/
        val geofence = Geofence.Builder()
            .setRequestId(reminderData.id)   // Set the request ID, string to identify the geofence.
            .setCircularRegion(reminderData.latitude!!, reminderData.longitude!!, circularRegion)  // Set the circular region of this geofence.
            .setExpirationDuration(Geofence.NEVER_EXPIRE)    // Set the expiration duration of the geofence. This geofence gets automatically removed after this period of time.
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)    // Set the transition types of interest. Alerts are only generated for these transition. We track entry and exit transitions in this sample.
            .build()

        /** 3- Build the geofence request **/
        val geofencingRequest = GeofencingRequest.Builder()
            // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device is already inside that geofence.
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)    // Add the geofence to be monitored by geofencing service.
            .build()

        /** 4- Add the new geofence request with the new geofence **/
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                // Geofences added.
                Toast.makeText(context, R.string.geofences_added, Toast.LENGTH_SHORT).show()
                Log.e("Add Geofence", geofence.requestId)

                _viewModel.validateAndSaveReminder(reminderData)      // learn Koin first
            }
            addOnFailureListener {
                // Failed to add geofence(s).
                _viewModel.showToast.value = getString(R.string.error_adding_geofence)
//                Toast.makeText(requireContext(), R.string.geofences_not_added, Toast.LENGTH_SHORT).show()
                if ((it.message != null)) {
                    Log.w("SaveReminderFragment", "${it.message}")
                }
            }
        }

    }

//   private val getPermissionsArray : Array<String>
    private fun getPermissionsArray(): Array<String> {
        // Foreground => ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION permissions
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Background => ACCESS_BACKGROUND_LOCATION(on Android 10+ (Q)) permission
        if (runningQOrLater)
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        return permissions.toTypedArray()   // for a certain usage later
    }

    private fun isLocationPermissionsGranted(): Boolean {
        // Check the 3 permissions
        getPermissionsArray().forEach {
            if (ContextCompat.checkSelfPermission(requireContext(), it) ==
                PackageManager.PERMISSION_DENIED
            ) {
                return false
            }
        }
        // Here all granted
        return true
    }


/**Starts the permission check**/
    private fun checkPermissionsThenStartGeofence() {
        if (isLocationPermissionsGranted()) {
            checkDeviceLocationSettingsThenStartGeofence()
        } else {
            Toast.makeText(context,R.string.permission_denied_explanation, Toast.LENGTH_SHORT).show()
            resolutionForResult.launch(getPermissionsArray())
        }
    }


    private val resolutionForResult = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // After the user choose from the Permission-Dialog ↴
            if (permissions.values.all { it }) {
                // Now all the 3 permissions are granted
                checkDeviceLocationSettingsThenStartGeofence()
            } else {
                _viewModel.showSnackBar.value = getString(R.string.permission_denied_explanation)
            }
        }

    // Uses the Location Client to check the current state of location settings, and gives the user the opportunity to turn on location services within our app.
/**Check Device Location Settings Then Start Geofence**/
    private fun checkDeviceLocationSettingsThenStartGeofence(resolve:Boolean = true) {
        /**  1- Get some info about the device-location */
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER   // RenderScript.Priority.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        /**  2- Check the device-location => If it is disabled, do a loop (dialog ⇄ snakebar) */
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                    startIntentSenderForResult(exception.resolution.intentSender, REQUEST_TURN_DEVICE_LOCATION_ON, null, 0,0,0, null)        // can work
                }
                catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else
                Snackbar.make(binding.root, R.string.location_required_error, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.settings) {
                        startActivity(
                            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        )
                    }.show()
        }

        /**  3- Check the device-location => If it is enabled, do ✅  */
        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {
                addGeofence(reminderData, _viewModel.circularRadius.value ?: 100f)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    // When we get the result from asking the user to turn on device location,
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            // We don't rely on the result code, but just check the location setting again
            checkDeviceLocationSettingsThenStartGeofence(false)
        }
    }


    companion object {
        internal const val ACTION_GEOFENCE_EVENT = "ACTION_GEOFENCE_EVENT"     // manual action filter
    }
}


// Determines whether the app has the appropriate permissions across Android 10+ and all other Android versions.
/** Check & ask for the 3 permissions **/
/*    private fun foregroundLocationPermissionApproved(): Boolean {
        return(PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val backgroundPermissionApproved =
            if (runningQOrLater)
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            else
                true

        return foregroundLocationPermissionApproved() && backgroundPermissionApproved
    }
    // Requests ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION and ACCESS_BACKGROUND_LOCATION(on Android 10+ (Q)).
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return

        firstRequest = true

        val resultCode = when {
            runningQOrLater -> {
                // this provides the result[BACKGROUND_LOCATION_PERMISSION_INDEX]
                PERMISSIONS += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        // You got the list of permissions and their request code. Now, show the request to the user!
        ActivityCompat.requestPermissions(requireActivity(), PERMISSIONS, resultCode)
    }*/

/*
// This function is being called after each fragment dialog of a permission finished.
@Deprecated("Deprecated in Java")
override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    Log.d(TAG, "onRequestPermissionResult")
    if (grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
        (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                PackageManager.PERMISSION_DENIED)
    ) {
        Snackbar.make(
            binding.root,
            R.string.permission_denied_explanation,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.settings) {
                startActivityForResult(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                }, REQUEST_TURN_DEVICE_LOCATION_ON)
            }.show()
    } else {
        checkDeviceLocationSettingsThenStartGeofence()
    }
}
*/
