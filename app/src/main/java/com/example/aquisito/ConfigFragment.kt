package com.example.aquisito

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.aquisito.databinding.FragmentConfigBinding
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import kotlin.system.exitProcess


class ConfigFragment : Fragment() {
    private lateinit var configBinding:FragmentConfigBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        configBinding = FragmentConfigBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment

        //inicilizar firebase auth
        auth= FirebaseAuth.getInstance()

        //obtener el usuario actual
        val user=auth.currentUser
        if(user!=null){
            //mostrar nombre y correo en la interfaz
            configBinding.tvUserName.text=user.displayName
            configBinding.tvUserEmail.text=user.email
        }

        //configurar el boton de cerrar sesion
        configBinding.btnSignOut.setOnClickListener {
            //cerrar sesion
            auth.signOut()
            deleteSharePreference()
            // Además de signOut(), también limpia las credenciales de Firebase UI
            AuthUI.getInstance().signOut(requireContext()
            ).addOnCompleteListener {
                Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()
                // Finalizar todas las actividades y cerrar la aplicación
                requireActivity().finishAffinity()
                exitProcess(0)  // Garantiza que la aplicación se cierra completamente
            }

        }
        return configBinding.root
    }

    private fun deleteSharePreference(){
        // Obtener la instancia del RouteFragment
        val routeFragment = parentFragmentManager.findFragmentById(R.id.hostFragment) as? RouteFragment
        routeFragment?.removeHomeLocation() // Llama al método para eliminar la dirección de casa
    }



}

