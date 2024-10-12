package com.example.aquisito


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.aquisito.databinding.FragmentLocationBinding
import java.util.Locale


class LocationFragment : Fragment(), TextToSpeech.OnInitListener {

    private lateinit var locationUpdateReceiver: BroadcastReceiver
    // Variable privada para el binding, inicializada como nula
    private lateinit var locationBinding: FragmentLocationBinding
    // uso del broadcast
    private lateinit var gpsStatusReceiver: BroadcastReceiver

    // inicializacion del texttospeach
    private lateinit var tts: TextToSpeech


    // Variable para controlar si TalkBack ya ha leído la ubicación una vez
    private var hasAnnouncedLocation = false

    // Se infla el layout del fragmento y se inicializa el binding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        locationBinding = FragmentLocationBinding.inflate(inflater, container, false)
        return locationBinding.root


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Iniciar el servicio de ubicación
        val serviceIntent = Intent(requireContext(), LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent)  // Para Android 8.0 y superior
        } else {
            requireContext().startService(serviceIntent)  // Para versiones anteriores
        }
        registerGPSStatusReceiver()
        // Registrar el BroadcastReceiver para escuchar las actualizaciones de ubicación
        registerLocationReceiver()

        // Configura el listener para tocar la pantalla
        tts = TextToSpeech(requireContext(), this)  // Inicializa TTS

        // Configura el listener para tocar la pantalla
        locationBinding.tvLocation.setOnClickListener {
            val address = locationBinding.tvLocation.text.toString()
            if (address.isNotEmpty()) {
                speakLocation()
            }
        }

        // Iniciar el proceso de obtener la ubicación y hacer que TalkBack lo lea
        announceLocationToTalkBackWithDelay()


    }


    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("es", "ES"))  // Configura el idioma español
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "El idioma no está soportado.")
            }
        }
    }

   fun speakLocation() {
       val text = locationBinding.tvLocation.text
       if (text.isNotEmpty()) {
           tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
       }
    }

    // Método para anunciar la ubicación solo una vez al iniciar
    private fun announceLocationToTalkBackWithDelay() {
        if (!hasAnnouncedLocation) {  // Solo lo ejecutamos si no ha sido anunciado antes
            Handler(Looper.getMainLooper()).postDelayed({
                locationBinding.tvLocation.let {
                    if (it.text.isNotEmpty() && it.text!= "Cerca de:") {
                        it.announceForAccessibility(it.text)  // TalkBack lee el contenido
                        hasAnnouncedLocation = true  // Marcamos que ya se ha leído una vez
                    }
                }
            }, 6000)  // Ajusta el retraso si es necesario (2 segundos en este caso)
        }
    }
    private fun registerLocationReceiver(){
        locationUpdateReceiver = object : BroadcastReceiver(){

            override  fun onReceive(context: Context?, intent: Intent) {

                Handler(Looper.getMainLooper()).post {
                    if (isAdded && context != null) { // Verificar si el fragmento está adjunto a la actividad
                        val address = intent.getStringExtra("address")
                        val namePOI = intent.getStringExtra("namePOI")

                        if (namePOI == "No se encontraron POIs cercanos." || namePOI == "Error al procesar la respuesta.") {
                            locationBinding.tvLocation.text =
                                getString(R.string.texto_estas_en, address)
                        } else {
                            locationBinding.tvLocation.text =
                                getString(R.string.location_message_with_poi, address, namePOI)

                        }

                    } else {
                        Log.w(
                            "LocationFragment",
                            "El fragmento no está adjunto a la actividad. BROADCAST CALL UBICACION"
                        )
                    }
                }

            }
}
        val intentFilter = IntentFilter("LocationUpdate")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
           requireContext().registerReceiver(locationUpdateReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            //LocalBroadcastManager.getInstance(requireContext()).registerReceiver(locationUpdateReceiver, intentFilter)
        }
    }


    private fun registerGPSStatusReceiver() {
            gpsStatusReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {

                    if (isAdded && context != null) {
                        if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                            updateGPSStatus()
                        }
                    } else {
                        Log.w("LocationFragment", "El fragmento no está adjunto a la actividad. GPS STATUS")
                    }
                }
            }
            val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
            requireContext().registerReceiver(gpsStatusReceiver, filter)
    }



    //recibe la respuesta y cambie el tvlocation
    private fun updateGPSStatus() {
        if (isAdded) {
        if (!isGPSEnabled(requireContext())) {
            locationBinding.tvLocation.text = getString(R.string.message_GPS_deshabilitado)
        } else {
            locationBinding.tvLocation.text = getString(R.string.message_GPS_habilitado)
            Log.d("LocationFragment", "GPS habilitado. Esperando actualizaciones de ubicación...")
        }
        } else {
            Log.w("LocationFragment", "El fragmento no está adjunto a la actividad.")
        }
    }
    // funcion que envia una respuesta bollean de false o true para ver si el gps esta hablitado
    private fun isGPSEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }


    fun enableLocationFeatures(enable: Boolean) {
        if (enable)
            registerLocationReceiver()

    }
    override fun onDestroy() {
        super.onDestroy()
        Log.d("LocationFragment", "onDestroy() llamado. Fragmento ya no está visible.")
        requireContext().unregisterReceiver(gpsStatusReceiver)
        Log.d("LocationFragment", "gpsStatusReceiver desregistrado")

        //LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(locationUpdateReceiver)
        requireContext().unregisterReceiver(locationUpdateReceiver)
        Log.d("LocationFragment", "locationUpdateReceiver desregistrado")
        //detenemos el motor tts
        tts.shutdown()
        Log.d("LocationFragment", "TextToSpeech apagado")

    }


}


