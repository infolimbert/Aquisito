package com.example.aquisito

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.aquisito.databinding.FragmentLocationBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationFragment : Fragment() {

    // Variable privada para el binding, inicializada como nula
    private var locationBinding: FragmentLocationBinding? =null
    // Propiedad pública para acceder al binding, asegura que no sea nula
    private val binding get() = locationBinding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Registrador del lanzador de permisos
        private val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted){
                //si el permiso es concedido, procede a obtener la ubicacion
                getLastKnowLocation()
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
        checkLocationPermission()
    }

    // Verifica si el permiso de ubicación está concedido
    private fun checkLocationPermission() {
                if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )== PackageManager.PERMISSION_GRANTED){
                    getLastKnowLocation()
                }else{
                    // Permiso ya concedido, puedes iniciar las actualizaciones de ubicación
                    //Toast.makeText(requireContext(),"Permiso de ubicacion concedido",Toast.LENGTH_SHORT).show()
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,1000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        locationCallback = object: LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations){
                    updateLocationUI(location)
                }
            }
        }

        try{
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }catch(e: SecurityException ){
            binding.tvLocation.text="No se puede obtener la ubicacion. Permiso denegado."
        }
    }


    //Metodo para actualizar la interfaz de usuario con la ubicacion actual
    private fun updateLocationUI(location: Location){
        val locationText= "Lat: ${location.latitude}, Lng: ${location.longitude}"
        binding.tvLocation.text = locationText
    }

    // Limpia el binding para evitar fugas de memoria
    override fun onDestroyView() {
        super.onDestroyView()
        locationBinding=null
        //Detener las actualizaciones de ubicacion cuando se destruya la vista
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}