package com.example.aquisito

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Looper.getMainLooper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.aquisito.databinding.FragmentLocationBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

class LocationFragment : Fragment() {

    private lateinit var locationBinding: FragmentLocationBinding

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                checkAndEnableGPS()
            } else {
                locationBinding.tvLocation.text = getString(R.string.permiso_denegado)
            }

        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        locationBinding = FragmentLocationBinding.inflate(inflater, container, false)
        return locationBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Verificar permisos de ubicación
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permiso ya concedido, habilitar GPS
                checkAndEnableGPS()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                // Mostrar una UI educativa para explicar por qué se necesita el permiso
                showInContextUI()
            }

            else -> {
                //solicitar permiso
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

    }


    private fun checkAndEnableGPS() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        val client = LocationServices.getSettingsClient(requireContext())
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        client.checkLocationSettings(builder.build()).addOnSuccessListener {

            showLocationEnabledMessage()

        }.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(requireActivity(), REQUEST_ENABLE_GPS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Manejar el error
                }
            }

        }

    }

    private fun showLocationEnabledMessage() {
        locationBinding.tvLocation.text = "GPS habitado"
    }


    private fun showInContextUI() {
        // Aquí puedes mostrar un diálogo o una UI que explique por qué se necesita el permiso
        // Por ejemplo, un AlertDialog explicativo
        AlertDialog.Builder(requireContext())
            .setTitle("Permiso de ubicacion Necesario")
            .setMessage("Esta aplicacion ncesita acceso a la ubicacion para funcionar")
            .setPositiveButton("OK") { _, _ ->
                // Volver a solicitar el permiso
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    companion object {
        private const val REQUEST_ENABLE_GPS = 2
    }


}