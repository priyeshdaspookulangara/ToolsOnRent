package com.example.toolsonrent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController // Keep for when Toolbar is added
import androidx.navigation.ui.navigateUp // Keep for when Toolbar is added
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNavView: BottomNavigationView = findViewById(R.id.bottomNavView)

        // Define top-level destinations for AppBarConfiguration.
        // These are the IDs from your bottom_nav_menu.xml and should match IDs in nav_graph.xml.
        // R.id.nav_dashboard, R.id.nav_tools_inventory, etc. were set in nav_graph.xml
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_dashboard, R.id.nav_tools_inventory, R.id.nav_customers,
                R.id.nav_rentals, R.id.nav_settings
            )
        )

        // Setup ActionBar with NavController (if you have a Toolbar).
        // Assuming a Toolbar might be added later or is part of the theme being used.
        // If using Theme.Material3.DayNight.NoActionBar, a Toolbar must be manually added
        // to activity_main.xml and then setSupportActionBar(toolbar) must be called.
        // For now, this line might not have a visible effect without a Toolbar.
        // setupActionBarWithNavController(navController, appBarConfiguration)
        // Let's comment this out for now as toolbar is not explicitly in activity_main.xml

        // Link BottomNavigationView to NavController
        NavigationUI.setupWithNavController(bottomNavView, navController)
    }

    // Override onSupportNavigateUp if setupActionBarWithNavController is used.
    // This handles the Up button behavior in the ActionBar.
    // override fun onSupportNavigateUp(): Boolean {
    //     return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    // }
}
