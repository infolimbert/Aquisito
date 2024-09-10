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
import android.os.Handler
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

    private lateinit var locationUpdateReceiver: BroadcastReceiver
    // Variable privada para el binding, inicializada como nula
    private lateinit var locationBinding: FragmentLocationBinding
    // uso del broadcast
    private lateinit var gpsStatusReceiver: BroadcastReceiver


    // Variable para controlar si TalkBack ya ha leído la ubicación una vez
    private var hasAnnouncedLocation = false

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

        // Iniciar el servicio de ubicación
        val serviceIntent = Intent(requireContext(), LocationService::class.java)
        requireContext().startService(serviceIntent)

        registerGPSStatusReceiver()
        // Registrar el BroadcastReceiver para escuchar las actualizaciones de ubicación
        registerLocationReceiver()


        // Iniciar el proceso de obtener la ubicación y hacer que TalkBack lo lea
        announceLocationToTalkBackWithDelay()

    }

    // Método para anunciar la ubicación solo una vez al iniciar
    private fun announceLocationToTalkBackWithDelay() {
        if (!hasAnnouncedLocation) {  // Solo lo ejecutamos si no ha sido anunciado antes
            Handler(Looper.getMainLooper()).postDelayed({
                locationBinding.tvLocation?.let {
                    if (it.text.isNotEmpty() && it.text!= "Cerca de:") {
                        it.announceForAccessibility(it.text)  // TalkBack lee el contenido
                        hasAnnouncedLocation = true  // Marcamos que ya se ha leído una vez
                    }
                }
            }, 3000)  // Ajusta el retraso si es necesario (2 segundos en este caso)
        }
    }
    private fun registerLocationReceiver(){
        locationUpdateReceiver = object : BroadcastReceiver(){

            override  fun onReceive(context: Context?, intent: Intent){
                val lat = intent?.getDoubleExtra("latitud", 0.0)
                val lgt = intent?.getDoubleExtra("longitud", 0.0)
                val address = intent?.getStringExtra("address")

                locationBinding.tvLocation.text = "Cerca de:\n$address"
            }
        }
        val intentFilter = IntentFilter("LocationUpdate")
        requireContext().registerReceiver(locationUpdateReceiver, intentFilter)
    }


    private fun registerGPSStatusReceiver() {
            gpsStatusReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                        updateGPSStatus()
                    }
                }
            }
            val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
            requireContext().registerReceiver(gpsStatusReceiver, filter)
    }



    //recibe la respuesta y cambie el tvlocation
    private fun updateGPSStatus() {
        if (!isGPSEnabled(requireContext())) {
            locationBinding.tvLocation.text = "GPS deshabilitado. Actívelo para usar esta función."
        } else {
            locationBinding.tvLocation.text = "GPS habilitado. Obteniendo ubicación..."
            Log.d("LocationFragment", "GPS habilitado. Esperando actualizaciones de ubicación...")
        }
    }
    // funcion que envia una respuesta bollean de false o true para ver si el gps esta hablitado
    fun isGPSEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }


    fun enableLocationFeatures(enable: Boolean) {
        if (enable)
            registerLocationReceiver()

    }


    override fun onDestroy() {
        super.onDestroy()
        // Asegúrate de desregistrar el BroadcastReceiver para evitar fugas de memoria
        requireContext().unregisterReceiver(gpsStatusReceiver)
        // Desregistrar el BroadcastReceiver cuando el fragmento se destruye
        requireContext().unregisterReceiver(locationUpdateReceiver)

    }


}