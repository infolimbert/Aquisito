package com.example.aquisito

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.intl.Locale
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat.isLocationEnabled
import com.example.aquisito.databinding.FragmentLocationBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import java.io.IOException

class LocationFragment : Fragment() {

    // Variable privada para el binding, inicializada como nula
    private lateinit var locationBinding: FragmentLocationBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback


    // Inicialización del request para ubicación con alta precisión
    private val locationRequest: LocationRequest by lazy {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(2000)
            .build()
    }

    // Se infla el layout del fragmento y se inicializa el binding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        locationBinding = FragmentLocationBinding.inflate(inflater, container, false)
        return locationBinding.root


    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        initializeLocationCallback()

    }

    fun enableLocationFeatures(enable: Boolean) {
        if (enable) {
            requestLocationUpdates()
        } else {
            stopLocationUpdates()
        }
    }


    private fun initializeLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    //updateLocationUI(location)
                    convertLocationAddress(location)
                    Log.d("LocationFragment","Lat: ${location.latitude}, Lng: ${location.longitude}")
                }
            }
        }
    }

    // Método para convertir la ubicación en una dirección legible

    private fun convertLocationAddress(location: Location){
        //geocoder nos permite convertir coordenas en una direccion
        val geocoder = Geocoder(requireContext(), java.util.Locale.getDefault())

        //intentamos obtener la direccion utilizando el geocoder
        try {
            val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)

            //si la lista de direccon no esta vacia
            if(!address.isNullOrEmpty()){
                //obtenemos la primera direccion de la lista
                val address = address[0]

                //construimos una cadena de texto con los componentes de la direccion
                val addressText= "${address.thoroughfare}, ${address.locality}, ${address.countryName}"

                //mostramos la direecon en un textView (tvlocation) en la interfaz de usuario
                locationBinding.tvLocation.text = addressText
            }else{
                // si no se encuentra ninguna direecon, mostramos latitud longitud
                //"Lat: ${location.latitude}, Lng: ${location.longitude}"
                locationBinding.tvLocation.text = "No hay direccion "
            }

        }catch (e: IOException){
            // En caso de error, mostramos un mensaje
            locationBinding.tvLocation.text = "Error al obtener la direccion"
        }

    }

    private fun requestLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Properly handle exception
            locationBinding.tvLocation.text = "No se puede obtener la ubicación. Permiso denegado."
        }
    }


    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateLocationUI(location: Location) {
        val locationText = "Lat: ${location.latitude}, Lng: ${location.longitude}"
        locationBinding.tvLocation.text = locationText
    }


    override fun onResume() {
        super.onResume()
        // Reanudar las actualizaciones de ubicación al volver al fragmento
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationUpdates()  // Solicitar actualizaciones continuas de la ubicación
        } else {
            // Opcional: Podrías mostrar una explicación o solicitar los permisos nuevamente aquí
        }
    }

    override fun onPause() {
        super.onPause()
        // Detener las actualizaciones de ubicación para evitar consumo innecesario de batería
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }


}