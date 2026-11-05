package com.fox.emu

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.fox.emu.ui.StoreFragment
import com.fox.emu.ui.GamesFragment
import com.fox.emu.ui.DownloadsFragment
import com.fox.emu.ui.SettingsFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val nav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        nav.setOnItemSelectedListener { item ->
            val frag: Fragment = when (item.itemId) {
                R.id.nav_store -> StoreFragment()
                R.id.nav_games -> GamesFragment()
                R.id.nav_downloads -> DownloadsFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> StoreFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, frag)
                .commit()
            true
        }
        if (savedInstanceState == null) nav.selectedItemId = R.id.nav_store
    }
}
