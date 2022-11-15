package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    private val TAG = SelectLocationFragment::class.java.simpleName

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var dialog: Dialog
    private lateinit var myPoi: PointOfInterest
    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    private val REQUEST_FOREGROUND_LOCATION_PERMISSION = 1
    private val REQUEST_TURN_DEVICE_LOCATION_ON = 29
    private var marker: Marker? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this
        dialog = Dialog(requireContext())

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        TODO Done: add the map setup implementation
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        /**Menu Setup**/
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.map_options, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.normal_map -> {
                        map.mapType = GoogleMap.MAP_TYPE_NORMAL
                        true
                    }
                    R.id.hybrid_map -> {
                        map.mapType = GoogleMap.MAP_TYPE_HYBRID
                        true
                    }
                    R.id.satellite_map -> {
                        map.mapType = GoogleMap.MAP_TYPE_SATELLITE
                        true
                    }
                    R.id.terrain_map -> {
                        map.mapType = GoogleMap.MAP_TYPE_TERRAIN
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        // Create the dialog message
        dialog.apply {
            setContentView(R.layout.custom_dialog)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                dialog.window?.setBackgroundDrawable(
                    AppCompatResources.getDrawable(
                        context,
                        R.drawable.dialog_design
                    )
                )
            }
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            window?.attributes?.windowAnimations = R.style.DialogAnimation
            setCancelable(false)
        }

        val okay: Button = dialog.findViewById(R.id.btn_okay)
        val cancel: Button = dialog.findViewById(R.id.btn_cancel)
        val num: EditText = dialog.findViewById(R.id.editTextNum)

        okay.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "The radius of the geofence => ${num.text} mile",
                Toast.LENGTH_LONG
            ).show()
            _viewModel.selectedPOI.value = myPoi
            _viewModel.circularRadius.value = num.text.toString().toFloat()
            dialog.dismiss()
            NavHostFragment.findNavController(this).popBackStack()
        }

        cancel.setOnClickListener {
            Toast.makeText(requireContext(), "Canceled", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

    }

    // TODO Done: call this function after the user confirms on the selected location
    private fun onLocationSelected(poi: PointOfInterest) {
        myPoi = poi
        dialog.show()
//        TODO Done: When the user confirms on the selected location,
//         send back the selected location details to the view model
//         and navigate back to the previous fragment to save the reminder and add the geofence
    }

    /**Map accessories**/
    override fun onMapReady(googleMap: GoogleMap) {
//        TODO Done: zoom to the user location after taking his permission
//        TODO Done: add style to the map
//        TODO Done: put a marker to location that the user selected
        val myHome = LatLng(29.975507526586643, 31.40644697381402)

        map = googleMap.apply {
            addMarker(MarkerOptions().position(myHome).title("Marker at there :)"))
            moveCamera(CameraUpdateFactory.newLatLngZoom(myHome, 16f))
        }

        setPoiClick(map)
        setMapStyle(map)
        setMapLongClick(map)
        checkPermissionsThenStartGeofence()
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style_1
                )
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            map.clear()     // To avoid showing many unused markers

            marker = map.addMarker(
                MarkerOptions().position(poi.latLng).title(poi.name).rotation(1f).alpha(0.5f)
            )
            marker?.showInfoWindow()
//            map.animateCamera(CameraUpdateFactory.newLatLng(poi.latLng))

            onLocationSelected(poi)
        }
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.clear()     // To avoid showing many unused markers

        map.setOnMapLongClickListener { latLng ->
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )
            marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    // when you click on the marked location, show these details.
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
            )
//             map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
            marker?.showInfoWindow()

            onLocationSelected(
                PointOfInterest(latLng, "${latLng.latitude},${latLng.longitude}", "Unnamed place")
            )
        }
    }

    /**Ask location permission**/
    private fun getPermissionsArray(): Array<String> {
        // Foreground => ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION permissions
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        return permissions.toTypedArray()   // for a certain usage later
    }

    private fun isLocationPermissionsGranted(): Boolean {
        // Check the 2 permissions
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

    private fun checkPermissionsThenStartGeofence() {
        if (isLocationPermissionsGranted()) {
            checkDeviceLocationSettingsIsEnabled()
        } else {
            Toast.makeText(context, R.string.permission_denied_explanation, Toast.LENGTH_SHORT)
                .show()
            resolutionForResult.launch(getPermissionsArray())
        }
    }


    private val resolutionForResult = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // After the user choose from the Permission-Dialog ↴
        if (permissions.values.all { it }) {
            // Now all permissions are granted
            checkDeviceLocationSettingsIsEnabled()
        } else {
            _viewModel.showSnackBar.value = getString(R.string.permission_denied_explanation)
        }
    }

    /**Check Device Location Settings**/
    // Uses the Location Client to check the current state of location settings, and gives the user the opportunity to turn on location services within our app.
    private fun checkDeviceLocationSettingsIsEnabled(resolve: Boolean = true) {
        /**  1- Get some info about the device-location */
        val locationRequest = LocationRequest.create().apply {
            priority =
                LocationRequest.PRIORITY_LOW_POWER   // RenderScript.Priority.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        /**  2- Check the device-location => If it is disabled, do a loop (dialog ⇄ snakebar) */
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        REQUEST_TURN_DEVICE_LOCATION_ON,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else
                Snackbar.make(
                    binding.root,
                    R.string.location_required_error,
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.settings) {
                        startActivity(
                            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        )
                    }
                    .show()
        }

        /**  3- Check the device-location => If it is enabled, do ✅  */
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(requireContext(), R.string.location_enabled, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    /**Activity response**/
    // When we get the result from asking the user to turn on device location,
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            // We don't rely on the result code, but just check the location setting again
            checkDeviceLocationSettingsIsEnabled(false)
        }
    }

}
