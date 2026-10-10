package com.mastersoft.fitpulseadmin.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.mastersoft.fitpulseadmin.R;
import com.mastersoft.fitpulseadmin.activity.AdminActivity;
import com.mastersoft.fitpulseadmin.data.AdminRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.Timestamp;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AdminDashboardFragment extends Fragment {

    private TextView                 tvTotalMembers, tvTodayCheckins, tvMonthlyRevenue, tvNewMembers;
    private LinearProgressIndicator  dashboardLoader;
    private CircularProgressIndicator checkinLoader;
    private LinearLayout             layoutRecentCheckins;
    private TextView                 tvNoCheckins;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Corrected IDs based on fragment_admin_dashboard.xml
        tvTotalMembers      = view.findViewById(R.id.tvTotalMembers);
        tvTodayCheckins     = view.findViewById(R.id.tvTodayCheckins);
        tvMonthlyRevenue    = view.findViewById(R.id.tvMonthlyRevenue);
        tvNewMembers        = view.findViewById(R.id.tvNewThisMonth);
        dashboardLoader     = view.findViewById(R.id.dashboardLoadingIndicator);
        checkinLoader       = view.findViewById(R.id.checkinsLoadingIndicator);
        layoutRecentCheckins = view.findViewById(R.id.layoutRecentCheckins);
        tvNoCheckins        = view.findViewById(R.id.tvNoCheckinsToday);


        View tabUsers = view.findViewById(R.id.tabUsers);
        if (tabUsers != null) {
            tabUsers.setOnClickListener(v -> navigateTo(R.id.admin_nav_users));
        }

        View tabFinance = view.findViewById(R.id.tabFinance);
        if (tabFinance != null) {
            tabFinance.setOnClickListener(v -> navigateTo(R.id.admin_nav_finance));
        }

        View btnOpenScanner = view.findViewById(R.id.btnOpenScanner);
        if (btnOpenScanner != null) {
            btnOpenScanner.setOnClickListener(v -> openQrScanner());
        }

        View cardTodayCheckins = view.findViewById(R.id.cardTodayCheckins);
        if (cardTodayCheckins != null) {
            cardTodayCheckins.setOnClickListener(v -> openQrScanner());
        }

        loadDashboardData();
    }

    //Data loading

    private void loadDashboardData() {
        if (dashboardLoader != null) dashboardLoader.setVisibility(View.VISIBLE);
        AdminRepository repo = AdminRepository.getInstance();


        AtomicInteger remaining = new AtomicInteger(4);

        Runnable onComplete = () -> {
            if (remaining.decrementAndGet() == 0) {
                if (getView() != null && dashboardLoader != null) {
                    dashboardLoader.setVisibility(View.GONE);
                }
            }
        };

        //Total members
        repo.getTotalMemberCount(count -> {
            if (getView() == null || tvTotalMembers == null) return;
            tvTotalMembers.setText(String.valueOf(count));
            onComplete.run();
        });

        // checkin count
        repo.getTodayCheckinCount(count -> {
            if (getView() == null || tvTodayCheckins == null) return;
            tvTodayCheckins.setText(String.valueOf(count));
            onComplete.run();
        });

        //Monthly revenue
        repo.getMonthlyRevenue(total -> {
            if (getView() == null || tvMonthlyRevenue == null) return;
            tvMonthlyRevenue.setText(AdminRepository.formatRs(total));
            onComplete.run();
        });

        // New members
        repo.getNewMembersThisMonth(count -> {
            if (getView() == null || tvNewMembers == null) return;
            tvNewMembers.setText(String.valueOf(count));
            onComplete.run();
        });

        // Recent checkins
        loadRecentCheckIns(repo);
    }

    private void loadRecentCheckIns(AdminRepository repo) {
        if (checkinLoader != null) checkinLoader.setVisibility(View.VISIBLE);
        if (tvNoCheckins != null) tvNoCheckins.setVisibility(View.GONE);
        if (layoutRecentCheckins != null) layoutRecentCheckins.removeAllViews();

        repo.getTodayCheckIns(records -> {
            if (getView() == null) return;
            if (checkinLoader != null) checkinLoader.setVisibility(View.GONE);

            if (records.isEmpty()) {
                if (tvNoCheckins != null) tvNoCheckins.setVisibility(View.VISIBLE);
                return;
            }

            for (Map<String, Object> record : records) {
                addCheckinRow(record);
            }
        });
    }

    private void addCheckinRow(Map<String, Object> record) {
        if (getContext() == null || layoutRecentCheckins == null) return;

        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_checkin_card, layoutRecentCheckins, false);

        String name = record.containsKey("userName") ? (String) record.get("userName") : "—";
        Timestamp ts = record.containsKey("checkInTime") ? (Timestamp) record.get("checkInTime") : null;
        String time = AdminRepository.formatTime(ts);

        TextView tvName = row.findViewById(R.id.tvCheckinUserName);
        Chip chipTime = row.findViewById(R.id.chipCheckinTime);
        
        if (tvName != null) tvName.setText(name);
        if (chipTime != null) chipTime.setText(time);

        layoutRecentCheckins.addView(row);
    }


    private void navigateTo(int navItemId) {
        if (getActivity() instanceof AdminActivity) {
            ((AdminActivity) getActivity()).navigateAdminTo(navItemId);
        }
    }

    private void openQrScanner() {
        if (getActivity() instanceof AdminActivity) {
            ((AdminActivity) getActivity()).pushDetailFragment(
                    new QrScannerFragment(), "qr_scanner");
        }
    }
}
