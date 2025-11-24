package com.aplicaciones_android.aplicacionrocola_projectofinal

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.aplicaciones_android.aplicacionrocola_projectofinal.ui.home.FragmentInicio
import com.aplicaciones_android.aplicacionrocola_projectofinal.ui.menu.MenuFragment
import com.aplicaciones_android.aplicacionrocola_projectofinal.ui.playlist.PlaylistFragment
import com.aplicaciones_android.aplicacionrocola_projectofinal.ui.search.BuscarFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        PlaylistManager.initialize(this)

        // Cargar fragmento inicio
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FragmentInicio())
                .commit()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.bringToFront()
        bottomNav.selectedItemId = R.id.nav_inicio
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> openFragment(FragmentInicio())
                R.id.nav_menu -> openFragment(MenuFragment())
                R.id.nav_buscar -> openFragment(BuscarFragment())
                R.id.nav_playlist -> openFragment(PlaylistFragment())
                else -> false
            }
        }
    }

    private fun openFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, fragment)
            .commit()
        return true
    }
}