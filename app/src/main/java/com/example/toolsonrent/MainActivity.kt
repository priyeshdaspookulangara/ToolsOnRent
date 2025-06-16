package com.example.toolsonrent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
// import androidx.navigation.ui.AppBarConfiguration
// import androidx.navigation.ui.setupActionBarWithNavController
// It's good practice to import R specifically if your project structure might cause ambiguity
// For example, if you had library modules with their own R files.
// However, for a single-module app, com.example.toolsonrent.R is implicitly available.

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // R will be resolved by the build system

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Optional: If you add a Toolbar to activity_main.xml and want to link it
        // val appBarConfiguration = AppBarConfiguration(navController.graph)
        // setupActionBarWithNavController(navController, appBarConfiguration)
        // Make sure you have a Toolbar with id, e.g., R.id.toolbar in activity_main.xml
        // and then call: setSupportActionBar(findViewById(R.id.toolbar)) before setupActionBarWithNavController
    }
}
