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

    //usamos como variable global ya que necesitamos esta solicitud en varias funciones
    private val locationRequest: LocationRequest by lazy {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(2000)
            .build()
    }
    // Variable privada para el binding, inicializada como nula
    private var locationBinding: FragmentLocationBinding? =null
    // Propiedad pública para acceder al binding, asegura que no sea nula
    private val binding get() = locationBinding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var gpsStatusReceiver: BroadcastReceiver

    // Registrador del lanzador de permisos
        private val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted){
                //si el permiso es concedido, procede a obtener la ubicacion
                checkAndEnableGPS()
            }else  {
                // si el permiso es denegado, muestra un mensaje
                binding.tvLocation.text = "Permiso denegado. No se pueede obtener la ubicacion"
            }

    }

    // Se infla el layout del fragmento y se inicializa el binding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        locationBinding = FragmentLocationBinding.inflate(inflater, container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        //inicializa locationCallback aqui
        locationCallback = object : LocationCallback (){
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations){
                    updateLocationUI(location)
                    Log.d("LocationFragment", "Ubicación actualizada: Lat: ${location.latitude}, Lng: ${location.longitude}")
                }
            }
        }

        registerGPSStatusReceiver()
        checkLocationPermission() // Verifica permisos solo una vez
    }

    fun resumeLocationUpdates() {
        if (isLocationEnabled()) {
            requestLocationUpdates()
        }
    }

    // Verifica si el permiso de ubicación está concedido
    private fun checkLocationPermission() {
                if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )== PackageManager.PERMISSION_GRANTED
               ){
                    Log.d("LocationFragment","GPS habilitado desde el onCreated")
                    checkAndEnableGPS()
                    //getLastKnowLocation()
                }else{
                    // Permiso ya concedido, puedes iniciar las actualizaciones de ubicación
                    //Toast.makeText(requireContext(),"Permiso de ubicacion concedido",Toast.LENGTH_SHORT).show()
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
    }

    private fun checkAndEnableGPS(){
        //construye una solicitud de configuracion de ubicacion que invluye la solicitud anterior
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        //Obtiene el cliente de configuracion de ubicacion para verificar si el gps esta habilitado
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val task = settingsClient.checkLocationSettings(builder.build())

        //si el Gps esta habilitado, procede a obtener la ultima ubicacion conocida
        task.addOnSuccessListener {
            getLastKnowLocation()
            Log.d("LocationFragment","GPS listo par usar entro al CHECKANDENABLEDGPS")
        }.addOnFailureListener { exception ->
            //si el gps no esta hablitado, intenta resolver el problewma mostrando  un dialogo
            if (exception is ResolvableApiException) {
                // Redirige a la configuración del GPS si no está habilitado
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
    }


    // Implementación para obtener la última ubicación conocida
    private fun getLastKnowLocation() {
        try{
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null){
                    // si la ubicacion no es nula, actualiza la UI
                    updateLocationUI(location)
                }else   {
                    //si la ubicacion es nula, solicita actualizaciones de ubicacion
                    requestLocationUpdates()
                }

            }
        } catch (e: SecurityException){
            // Maneja la excepcion en caso de que los permisos no esten condedidos
            binding.tvLocation.text = "No se puede obtener la ubicacion. Permiso denegado."
        }
    }

    private fun requestLocationUpdates() {

     /*   locationCallback = object: LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations){
                    updateLocationUI(location)
                }
            }
        }
*/
        try{
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
        }catch(e: SecurityException ){
            binding.tvLocation.text="No se puede obtener la ubicacion. Permiso denegado."
        }
    }


    //Metodo para actualizar la interfaz de usuario con la ubicacion actual
    private fun updateLocationUI(location: Location){
        /*val locationText= "Lat: ${location.latitude}, Lng: ${location.longitude}"
        binding.tvLocation.text = locationText*/

        //actualiza la vista con la nueva direccion y guarda la ultima ubicacion conocida
        convertLocationAddress(location)
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
                    binding.tvLocation.text = addressText
                }else{
                    // si no se encuentra ninguna direecon, mostramos latitud longitud
                    //"Lat: ${location.latitude}, Lng: ${location.longitude}"
                    binding.tvLocation.text = "No hay direccion "
                     }

            }catch (e: IOException){
                // En caso de error, mostramos un mensaje
                binding.tvLocation.text = "Error al obtener la direccion"
        }

    }

    private fun registerGPSStatusReceiver() {
        gpsStatusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (LocationManager.PROVIDERS_CHANGED_ACTION == intent?.action) {
                    if (isLocationEnabled()) {
                        getLastKnowLocation()
                    } else {
                        binding.tvLocation.text = "GPS deshabilitado"
                    }
                }
            }
        }
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        requireContext().registerReceiver(gpsStatusReceiver, filter)
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }



    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getLastKnowLocation()  // Obtener la última ubicación conocida al volver a entrar al fragmento
            requestLocationUpdates()  // Solicitar actualizaciones continuas de la ubicación
        }

    }

    override fun onPause() {
        super.onPause()
        //Solo intentar eliminar las actualizaciones de ubicaion si locationCallback ha sido inicializado
        if (::locationCallback.isInitialized){
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }


    }

    // Limpia el binding para evitar fugas de memoria
    override fun onDestroyView() {
        super.onDestroyView()
        locationBinding=null
        //Detener las actualizaciones de ubicacion cuando se destruya la vista
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    //maneja el resultado de la oslicitud para hablitar el GPS
}