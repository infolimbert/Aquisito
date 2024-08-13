package com.example.aquisito

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.aquisito.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {


    // Variable para el binding de la actividad principal
    private lateinit var mBinding: ActivityMainBinding

    // Variables para manejar el fragmento activo y el administrador de fragmentos
    private lateinit var mFragmentManager: FragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)


        // Configuración de la barra de herramientas (Toolbar)
        setSupportActionBar(mBinding.toolbar)

        // Configurar la vista de navegación inferior
        val bottomNavigationView = mBinding.bottomNav

        // Configurar la vista de navegación inferior (BottomNavigationView)
        setupBottomNav()

        // Seleccionar y mostrar por defecto el fragmento de ubicación (LocationFragment)
        mBinding.bottomNav.selectedItemId = R.id.action_location
        mBinding.toolbar.title = getString(R.string.location_title)
        supportActionBar?.title = mBinding.toolbar.title


    }

    private fun setupBottomNav(){

        mFragmentManager = supportFragmentManager

        val locationFragment = LocationFragment()
        val routeFragment = RouteFragment()
        val configFragment = ConfigFragment()

        // Configuración del listener para gestionar los clics en los elementos del BottomNavigationView
        mBinding.bottomNav.setOnItemSelectedListener {
            when(it.itemId){
                R.id.action_location->{
                    // Reemplaza el fragmento actual por el LocationFragment y actualiza el título
                    replaceFragment(locationFragment, getString(R.string.location_title))
                    true
                }
                R.id.action_route ->{
                    replaceFragment(routeFragment,getString(R.string.route_title))
                    true
                }
                R.id.action_config->{
                    replaceFragment(configFragment,getString(R.string.config_title))
                    true
                }
                else-> false
            }

        }
    }

    // Método para reemplazar el fragmento actual y actualizar el título de la barra de herramientas
    private fun replaceFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.hostFragment, fragment) // Reemplaza el fragmento en el contenedor hostFragment
            .commit()
        mBinding.toolbar.title = title  // Actualiza el título del Toolbar
        supportActionBar?.title = title // Sincroniza el título de la ActionBar con el del Toolbar
    }
}