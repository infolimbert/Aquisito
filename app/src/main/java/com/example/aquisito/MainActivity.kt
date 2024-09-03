package com.example.aquisito

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.aquisito.databinding.ActivityMainBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority


class MainActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityMainBinding

    private val locationFragment = LocationFragment()
    private val routeFragment = RouteFragment()
    private val configFragment = ConfigFragment()


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkAndEnableGPS()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        mBinding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.toolbar)
        //gestor para la barra de navegacion
        bottomNav()
        checkLocationPermission()

    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.hostFragment)
            when (currentFragment) {
                is LocationFragment -> currentFragment.enableLocationFeatures(true)
                //is RouteFragment -> currentFragment.updateRouteDisplay()
                // Agrega más casos si es necesario para otros fragmentosel
            }
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                checkAndEnableGPS()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                showInContextUI()
            }
            else -> {
                requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun checkAndEnableGPS() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this)

        client.checkLocationSettings(builder.build())
            .addOnSuccessListener {
                locationFragment.enableLocationFeatures(true)
            }
            .addOnFailureListener { exception ->
                Log.d("LocationFragment","El GPS esta listo par ausarse desde el CheckAndEnabledGPS")
                if (exception is ResolvableApiException) {
                    try {
                        exception.startResolutionForResult(this, 1001)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        Log.d("MainActivity", "Error al intentar habilitar el GPS: ${sendEx.message}")
                    }
                }
            }
    }

    //Funcion para habilitar el gps si esque lo desactivaron cuando usaban la aplicacion
    private fun showAlertGpsDisabled() {
        AlertDialog.Builder(this)
            .setTitle("GPS Desactivado")
            .setMessage("Por favor, activa el GPS para continuar usando esta característica.")
            .setPositiveButton("Configuración") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (!isGPSEnabled(this
        )) {
            // Mostrar un diálogo o una notificación pidiendo al usuario que habilite el GPS
            showAlertGpsDisabled()
        }
    }

    fun isGPSEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun showInContextUI() {
        AlertDialog.Builder(this)
            .setTitle("Permiso de ubicación necesario")
            .setMessage("Esta aplicación necesita acceso a la ubicación para funcionar correctamente. Por favor, permite el acceso para continuar.")
            .setPositiveButton("OK") { _, _ ->
                requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun bottomNav(){

        mBinding.bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_location -> {
                    replaceFragment(locationFragment, getString(R.string.location_title))
                    true
                }
                R.id.action_route -> {
                    replaceFragment(routeFragment, getString(R.string.route_title))
                    true
                }
                R.id.action_config -> {
                    replaceFragment(configFragment, getString(R.string.config_title))
                    true
                }
                else -> false
            }
        }

        // Establecer la pestaña inicial
        mBinding.bottomNav.selectedItemId = R.id.action_location

    }

    private fun replaceFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.hostFragment, fragment)
            .commit()

        mBinding.toolbar.title = title
        supportActionBar?.title = title
    }

}
