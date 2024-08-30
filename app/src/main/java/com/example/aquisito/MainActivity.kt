package com.example.aquisito

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.aquisito.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityMainBinding

    private lateinit var mActiveFragment: Fragment
    private lateinit var mFragmentManager: FragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        mBinding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        // Configuración de la barra de herramientas
        setSupportActionBar(mBinding.toolbar)

        // Configurar la vista de navegación inferior
       // val bottomNavigationView = mBinding.bottomNav

        // Configurar el listener de navegación y los fragmentos
        setupBottomNav()

        // Establecer el elemento seleccionado inicial y el título
        /*bottomNavigationView.selectedItemId = R.id.action_location
        mBinding.toolbar.title = getString(R.string.location_title)
        supportActionBar?.title = mBinding.toolbar.title*/

        mBinding.bottomNav.selectedItemId = R.id.action_location
        mBinding.toolbar.title = getString(R.string.location_title)
        supportActionBar?.title = mBinding.toolbar.title


    }

    private fun setupBottomNav(){
       mFragmentManager = supportFragmentManager

        val locationFragment = LocationFragment()
        val routeFragment = RouteFragment()
        val configFragment = ConfigFragment()



        mBinding.bottomNav.setOnItemSelectedListener {
            when(it.itemId){
                R.id.action_location->{
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

    private fun replaceFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.hostFragment, fragment)
            .commit()
        mBinding.toolbar.title = title
        supportActionBar?.title = title
    }
}