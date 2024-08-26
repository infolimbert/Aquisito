package com.example.aquisito

import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.aquisito.databinding.ActivityMainBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

class MainActivity : AppCompatActivity() {

    // Variable para el binding de la actividad principal
    private lateinit var mBinding: ActivityMainBinding


    private val locationFragment = LocationFragment()
    private val routeFragment = RouteFragment()
    private val configFragment = ConfigFragment()

    // Launcher para solicitar permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Permiso de ubicación concedido.")
            checkAndEnableGPS()
        } else {
            Log.d("MainActivity", "Permiso de ubicación denegado.")
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.toolbar)

        // Verificación inicial de permisos al iniciar la app
        checkLocationPermission()

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

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("MainActivity", "Permiso ya concedido, se procede a habilitar GPS.")
                checkAndEnableGPS()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                Log.d("MainActivity", "Se muestra la UI educativa para explicar el permiso.")
                showInContextUI()
            }
            else -> {
                Log.d("MainActivity", "Solicitando el permiso de ubicación.")
                requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun checkAndEnableGPS() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000
        ).build()
        val client = LocationServices.getSettingsClient(this)
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        client.checkLocationSettings(builder.build()).addOnSuccessListener {
            Log.d("MainActivity", "GPS habilitado correctamente.")
            // Aquí es donde podemos notificar al fragmento que el GPS está habilitado.
        }.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this, REQUEST_ENABLE_GPS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d("MainActivity", "Error al intentar habilitar el GPS: ${sendEx.message}")
                }
            }
        }
    }

    private fun showInContextUI() {
        AlertDialog.Builder(this)
            .setTitle("Permiso de ubicación necesario")
            .setMessage("Esta aplicación necesita acceso a la ubicación para funcionar.")
            .setPositiveButton("OK") { _, _ ->
                Log.d("MainActivity", "Volviendo a solicitar el permiso.")
                requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    companion object {
        private const val REQUEST_ENABLE_GPS = 2
    }

}