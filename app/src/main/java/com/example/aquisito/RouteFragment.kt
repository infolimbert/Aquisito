package com.example.aquisito

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.example.aquisito.databinding.FragmentRouteBinding
import com.google.android.gms.maps.model.LatLng
import java.util.Locale


class RouteFragment : Fragment(), TextToSpeech.OnInitListener {
    // Variable privada para el binding, inicializada como nula
    private lateinit var routeBinding: FragmentRouteBinding

    private lateinit var locationUpdateReceiverRoute: BroadcastReceiver
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var textViewInstructions : TextView
    private lateinit var textViewHomeDescription : TextView
    private lateinit var imageViewArrow: ImageView
    private lateinit var buttonAddHome: Button
    private lateinit var buttonRemoveHome: Button
    private lateinit var buttonStartRoute: Button

    //private var routeGeometry: Geometry? = null // aqui se guarda la gemoetry de la ruta
    private var instruction: List<String>? = null //instrucciones de la ruta

    private var homeLocation: LatLng? = null // Aquí se almacenará la ubicación de casa


    private var lat: Double = 0.0
    private var lng: Double = 0.0
    private var address: String=""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        routeBinding = FragmentRouteBinding.inflate(inflater, container, false)
        return routeBinding.root
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationUpdateReceiverRoute = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                lat = intent?.getDoubleExtra("latitud", 0.0) ?: return
                lng = intent.getDoubleExtra("longitud", 0.0) ?: return
                address = intent.getStringExtra("address") ?: return
                // onLocationUpdate(lat, lng)
            }
        }
        // Registrar el receptor para actualizaciones de ubicación
        requireActivity().registerReceiver(locationUpdateReceiverRoute, IntentFilter("LocationUpdate"))
        Log.d("RouteFragment", "locationUpdateReceiver REGISTRADO")


        textViewInstructions = routeBinding.textViewInstructions
        textViewHomeDescription = routeBinding.tvHomeDescription
        imageViewArrow = routeBinding.ivArrow

        buttonAddHome = routeBinding.btnAddHome
        buttonRemoveHome = routeBinding.btnRemoveHome
        buttonStartRoute = routeBinding.btnStartReturn

        // inicializa el texttospeach
        textToSpeech = TextToSpeech(requireContext(), this)  // Inicializa TTS



        // Cargar la dirección de casa desde SharedPreferences
        loadHomeLocation()

        // Configurar listeners para los botones
        buttonAddHome.setOnClickListener { addHomeLocation() }
        buttonRemoveHome.setOnClickListener { removeHomeLocation() }
       // buttonStartRoute.setOnClickListener { startRoute() }

    }


        private  fun addHomeLocation() {
            // Aquí usamos las variables de latitud y longitud que obtenemos del BroadcastReceiver
                if (lat != 0.0 && lng != 0.0) {
                    homeLocation = LatLng(lat, lng)// Almacenar la ubicación de casa
                    Log.d("RouteFragment", "Casa agregada: Lat: $lat, Lng: $lng")

                    // Guardar en SharedPreferences
                    val sharedPreferences = requireActivity().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
                    with(sharedPreferences.edit()) {
                        putFloat("homeLat", lat.toFloat())
                        putFloat("homeLng", lng.toFloat())
                        apply() // Guardar los cambios
                    }


                    textViewHomeDescription.text = "Casa guardada en Lat: $lat, Lng: $lng"
                }else {
                    Log.e("RouteFragment", "No se pudo obtener la ubicación actual.")
                    textViewInstructions.text = "No se pudo obtener la ubicación actual."
                }

        }

    private fun loadHomeLocation() {
        val sharedPreferences = requireActivity().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        val lat = sharedPreferences.getFloat("homeLat", 0f)
        val lng = sharedPreferences.getFloat("homeLng", 0f)

        if (lat != 0f && lng != 0f) {
            homeLocation = LatLng(lat.toDouble(), lng.toDouble())
            Log.d("RouteFragment", "Casa cargada: Lat: $lat, Lng: $lng")
        }
    }

     fun removeHomeLocation() {
        homeLocation = null
        Log.d("RouteFragment", "Casa eliminada.")
        textViewHomeDescription.text = "No hay una casa guardada"

        // Eliminar de SharedPreferences
        val sharedPreferences = requireActivity().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove("homeLat")
            remove("homeLng")
            apply() // Guardar los cambios
        }

    }


    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale("es", "ES"))  // Configura el idioma español
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "El idioma no está soportado.")
            }
        }
    }



  /*  private fun onLocationUpdate(lat: Double, lng: Double) {
        // Comprobar proximidad a los giros de la ruta
        routeGeometry?.let { geometry ->
            for (step in geometry.coordinates) {
                val stepLat = step.latitude
                val stepLng = step.longitude
                val proximityThreshold = 0.0003 // Aproximadamente 30 metros en grados

                if (Math.abs(lat - stepLat) < proximityThreshold && Math.abs(lng - stepLng) < proximityThreshold) {
                    showInstructionsAndArrows(instructions, step)
                    break
                }
            }
        }
    }

    private fun showInstructionsAndArrows(instructions: List<String>?, step: Step) {
        instructions?.forEach { instruction ->
            textViewInstructions.text = instruction
            updateArrowDirection(instruction)
            textToSpeech.speak(instruction, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun updateArrowDirection(instruction: String) {
        when {
            instruction.contains("izquierda", ignoreCase = true) -> {
                imageViewArrow.setImageResource(R.drawable.left)
            }
            instruction.contains("derecha", ignoreCase = true) -> {
                imageViewArrow.setImageResource(R.drawable.right)
            }
            instruction.contains("recto", ignoreCase = true) -> {
                imageViewArrow.setImageResource(R.drawable.straight)
            }
            // Agregar más condiciones según sea necesario
        }
    }*/


    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(locationUpdateReceiverRoute) // Desregistrar el receptor
        Log.d("RouteFragment", "locationUpdateReceiver desregistrado")
        textToSpeech.shutdown() // Libera recursos de TTS
    }




}