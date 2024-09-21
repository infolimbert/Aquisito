package com.example.aquisito

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
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

    private lateinit var mBinding: ActivityMainBinding

    private val locationFragment = LocationFragment()
    private val routeFragment = RouteFragment()
    private val configFragment = ConfigFragment()

    private var mediaSession: MediaSession? = null



    // Código de solicitud de permisos
    private val REQUEST_BACKGROUND_LOCATION_PERMISSION = 101

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkBackgroundLocationPermission()
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

        // Verificar si el permiso de segundo plano ya ha sido concedido
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            restartLocationUpdates()
        }


        // Iniciar MediaSession automáticamente al abrir la aplicación
        mediaSession = MediaSession()
        mediaSession?.start(this)
        // Pasamos el fragmento al Model
        Model.setLocationFragment(locationFragment)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_BACKGROUND_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Permiso de ubicación en segundo plano concedido")
                // Reiniciar las actualizaciones de ubicación
                checkAndEnableGPS()
                restartLocationUpdates()
            } else {
                Log.d("MainActivity", "Permiso de ubicación en segundo plano denegado")
            }
        }
    }

    private fun restartLocationUpdates() {
        val serviceIntent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)  // Usar startForegroundService para Android O y superior
        } else {
            startService(serviceIntent)
        }
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
                checkBackgroundLocationPermission()
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
            }else {
                // Si el permiso ya fue concedido, habilitamos el GPS
                checkAndEnableGPS()
            }
        } else {
            // Si estamos en una versión anterior a Android Q, habilitamos el GPS directamente
            checkAndEnableGPS()
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
                    // Verificar si el permiso ha sido rechazado permanentemente (No volver a preguntar)
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                        // Si el usuario NO ha seleccionado "No volver a preguntar", vuelve a solicitar el permiso
                        requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        // Si el usuario ha seleccionado "No volver a preguntar", llevarlo a la configuración de la app
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }
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

        // Liberar la MediaSession cuando se destruye la actividad
        mediaSession?.stop(this)


    }


}
