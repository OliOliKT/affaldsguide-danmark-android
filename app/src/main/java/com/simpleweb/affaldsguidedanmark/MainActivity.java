package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.gms.ads.MobileAds;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private Button begynd;
    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;
    private NavigationView navigationView;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageManager.applySavedLanguage(this);
        super.onCreate(savedInstanceState);

        MobileAds.initialize(this);

        sharedPref = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE);
        boolean hasSeenGreetingPage = sharedPref.getBoolean("hasSeenGreetingPage", false);

        if (!hasSeenGreetingPage) {
            setContentView(R.layout.activity_greeting);
            setUpGreetingLayout();
        } else {
            setContentView(R.layout.activity_main);
            setUpMainLayout();
        }
    }

    private void setUpGreetingLayout() {
        RadioGroup languageRadioGroup = findViewById(R.id.languageRadioGroup);
        RadioButton danishLanguageOption = findViewById(R.id.danishLanguageOption);
        RadioButton englishLanguageOption = findViewById(R.id.englishLanguageOption);

        if (LanguageManager.isEnglish(this)) {
            englishLanguageOption.setChecked(true);
        } else {
            danishLanguageOption.setChecked(true);
        }

        languageRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.englishLanguageOption) {
                LanguageManager.saveLanguage(this, LanguageManager.ENGLISH);
            } else if (checkedId == R.id.danishLanguageOption) {
                LanguageManager.saveLanguage(this, LanguageManager.DANISH);
            }
        });

        begynd = findViewById(R.id.begynd);
        begynd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharedPref.edit().putBoolean("hasSeenGreetingPage", true).apply();
                setContentView(R.layout.activity_main);
                setUpMainLayout();
            }
        });
    }

    private void setUpMainLayout() {

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        drawerLayout = findViewById(R.id.main_layout);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                switch (itemId) {
                    case R.id.search_trash_button:
                        navController.navigate(R.id.action_to_searchTrash);
                        break;
                    case R.id.trash_types_button:
                        navController.navigate(R.id.fragment_trash_types);
                        break;
                    case R.id.municipalities_button:
                        navController.navigate(R.id.fragment_municipalities);
                        break;
                    case R.id.navigation_view_button:
                        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                            drawerLayout.closeDrawer(GravityCompat.END);
                        } else {
                            drawerLayout.openDrawer(GravityCompat.END);
                        }
                        break;
                }
                return true;
            }
        });

        navigationView = findViewById(R.id.navigation_view);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                switch (itemId) {
                    case R.id.om:
                        navController.navigate(R.id.fragment_about);
                        drawerLayout.closeDrawer(GravityCompat.END);
                        return true;
                    case R.id.kontaktinformation:
                        navController.navigate(R.id.fragment_contact_information);
                        drawerLayout.closeDrawer(GravityCompat.END);
                        return true;
                    case R.id.spørgsmålogsvar:
                        navController.navigate(R.id.fragment_qa);
                        drawerLayout.closeDrawer(GravityCompat.END);
                        return true;
                    case R.id.seneste_soegninger:
                        navController.navigate(R.id.fragment_recent_searches);
                        drawerLayout.closeDrawer(GravityCompat.END);
                        return true;
                    case R.id.sprog:
                        navController.navigate(R.id.fragment_language);
                        drawerLayout.closeDrawer(GravityCompat.END);
                        return true;
                    case R.id.privatlivspolitik:
                        navController.navigate(R.id.fragment_privacy_policy);
                        drawerLayout.closeDrawer(GravityCompat.END);
                        return true;
                    case R.id.vilkårogbetingelser:
                        navController.navigate(R.id.fragment_terms_of_service);
                        drawerLayout.closeDrawer(GravityCompat.END);
                        return true;
                }
                return false;
            }
        });
    }
}
