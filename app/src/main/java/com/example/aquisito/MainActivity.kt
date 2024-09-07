package com.example.aquisito

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
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


    // Código de solicitud de permisos
    private val REQUEST_BACKGROUND_LOCATION_PERMISSION = 101

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

        // Crear el canal de notificación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                LocationService.CHANNEL_ID,
                "Canal de Servicio de Ubicación",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }

        setSupportActionBar(mBinding.toolbar)
        //gestor para la barra de navegacion
        bottomNav()
        checkLocationPermission()

        // Verificar si es necesario solicitar el permiso de ubicación en segundo plano
        checkBackgroundLocationPermission()

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


    private fun checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {

                // Mostrar una explicación si es necesario
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    // Explicar al usuario por qué se necesita este permiso
                    showPermissionExplanationDialog()
                } else {
                        // Solicitar el permiso de ubicación en segundo plano
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                            REQUEST_BACKGROUND_LOCATION_PERMISSION
                        )
                    }
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


    private fun isGPSEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    // Método para mostrar un diálogo explicando por qué se necesita el permiso
    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permiso de ubicación en segundo plano")
            .setMessage("Este permiso es necesario para seguir rastreando la ubicación mientras la aplicación no está activa.")
            .setPositiveButton("OK") { _, _ ->
                // Solicitar el permiso de nuevo si el usuario lo entiende
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    REQUEST_BACKGROUND_LOCATION_PERMISSION
                )
            }
            .setNegativeButton("Cancelar", null)
            .create()
            .show()
    }

    private fun showInContextUI() {
        if (!isGPSEnabled(this)){
            AlertDialog.Builder(this)
                .setTitle("Permiso de ubicación necesario")
                .setMessage("Esta aplicación necesita acceso a la ubicación para funcionar correctamente. Por favor, permite el acceso para continuar.")
                .setPositiveButton("OK") { _, _ ->
                    requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                }
                .setNegativeButton("Cancelar", null)
                .show()

        }

    }
    private fun bottomNav(){

        mBinding.bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_location -> {
                    switchFragment(locationFragment, getString(R.string.location_title))
                    true
                }
                R.id.action_route -> {
                    switchFragment(routeFragment, getString(R.string.route_title))
                    true
                }
                R.id.action_config -> {
                    switchFragment(configFragment, getString(R.string.config_title))
                    true
                }
                else -> false
            }
        }

        // Establecer la pestaña inicial
        mBinding.bottomNav.selectedItemId = R.id.action_location

    }

    private fun switchFragment(fragment: Fragment, title: String) {
        val fragmentManager= supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()


        //ocultar todos los fragmetnos que ya estan aniadidos
        for (frag in fragmentManager.fragments){
            fragmentTransaction.hide(frag)
        }

        // Comprobar si el fragmento ya fue añadido anteriormente
        if (fragment.isAdded) {
            // Mostrar el fragmento si ya está añadido
            fragmentTransaction.show(fragment)
        } else {
            // Si no ha sido añadido, agregarlo al contenedor y luego mostrarlo
            fragmentTransaction.add(R.id.hostFragment, fragment)
        }

        // Actualizar el título de la toolbar
        mBinding.toolbar.title = title
        supportActionBar?.title = title

        // Confirmar la transacción
        fragmentTransaction.commit()
    }

    private fun stopLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        stopService(serviceIntent)  // Detener el servicio
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationService()  // Detener el servicio cuando la actividad se destruya
    }


}
