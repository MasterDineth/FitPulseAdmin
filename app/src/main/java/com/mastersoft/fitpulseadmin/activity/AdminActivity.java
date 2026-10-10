package com.mastersoft.fitpulseadmin.activity;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.transition.MaterialFadeThrough;
import com.mastersoft.fitpulseadmin.R;
import com.mastersoft.fitpulseadmin.fragment.AdminDashboardFragment;
import com.mastersoft.fitpulseadmin.fragment.AdminFinanceFragment;
import com.mastersoft.fitpulseadmin.fragment.AdminUsersFragment;

public class AdminActivity extends AppCompatActivity {

    private NavigationBarView adminBottomNav;
    private final AdminDashboardFragment dashboardFragment = new AdminDashboardFragment();
    private final AdminUsersFragment usersFragment     = new AdminUsersFragment();
    private final AdminFinanceFragment financeFragment   = new AdminFinanceFragment();

    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        adminBottomNav = findViewById(R.id.adminBottomNavigation);
        activeFragment = dashboardFragment;

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.adminFragmentContainer, financeFragment,  "admin_finance").hide(financeFragment)
                    .add(R.id.adminFragmentContainer, usersFragment,    "admin_users").hide(usersFragment)
                    .add(R.id.adminFragmentContainer, dashboardFragment, "admin_dashboard")
                    .commit();
        }

        adminBottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment target;

            if      (id == R.id.admin_nav_dashboard) target = dashboardFragment;
            else if (id == R.id.admin_nav_users)     target = usersFragment;
            else if (id == R.id.admin_nav_finance)   target = financeFragment;
            else return false;

            if (target != activeFragment) switchRootFragment(target);
            return true;
        });

        // Show / hide bottom nav
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            boolean hasBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
            adminBottomNav.setVisibility(hasBack ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) fm.popBackStack();
        else super.onBackPressed();
    }

    //tab switch

    private void switchRootFragment(Fragment target) {
        activeFragment.setExitTransition(new MaterialFadeThrough());
        target.setEnterTransition(new MaterialFadeThrough());
        getSupportFragmentManager()
                .beginTransaction()
                .hide(activeFragment)
                .show(target)
                .commit();
        activeFragment = target;
    }

    //helpers for child fragments

    public void navigateAdminTo(int navItemId) {
        adminBottomNav.setSelectedItemId(navItemId);
    }


    public void pushDetailFragment(Fragment fragment, String tag) {
        fragment.setEnterTransition(new MaterialFadeThrough());
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.adminFragmentContainer, fragment, tag)
                .addToBackStack(tag)
                .commit();
    }

    public void hideBottomNav() {
        adminBottomNav.setVisibility(View.GONE);
    }

    public void showBottomNav() {
        adminBottomNav.setVisibility(View.VISIBLE);
    }
}
