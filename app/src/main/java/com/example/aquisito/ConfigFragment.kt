package com.example.aquisito

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.aquisito.databinding.FragmentConfigBinding
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth


class ConfigFragment : Fragment() {
    private lateinit var configBinding:FragmentConfigBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
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
            // Además de signOut(), también limpia las credenciales de Firebase UI
            AuthUI.getInstance().signOut(requireContext()).addOnCompleteListener {
                Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()
                // Redirige al usuario al SplashActivity o pantalla de inicio
                val intent = Intent(requireContext(), SplashActivity::class.java)
                startActivity(intent)
                requireActivity().finish() // Finaliza la actividad actual
            }

        }
        return configBinding.root
    }



}

