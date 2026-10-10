package com.mastersoft.fitpulseadmin.fragment;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.mastersoft.fitpulseadmin.R;
import com.mastersoft.fitpulseadmin.activity.AdminActivity;
import com.mastersoft.fitpulseadmin.data.AdminRepository;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminUsersFragment extends Fragment {

    private LinearProgressIndicator usersLoader;
    private TextView                tvActiveCount, tvInactiveCount;
    private TextInputEditText       etSearch;
    private LinearLayout            layoutUsersList;
    private TextView                tvUsersEmpty;

    // Full unfiltered list
    private final List<Map<String, Object>> allUsers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        usersLoader    = view.findViewById(R.id.usersLoader);
        tvActiveCount  = view.findViewById(R.id.tvActiveCount);
        tvInactiveCount = view.findViewById(R.id.tvInactiveCount);
        etSearch       = view.findViewById(R.id.etUserSearch);
        layoutUsersList = view.findViewById(R.id.layoutUsersList);
        tvUsersEmpty   = view.findViewById(R.id.tvUsersEmpty);

        // Tab navigation
        if (view.findViewById(R.id.tabDashboard) != null) {
            view.findViewById(R.id.tabDashboard).setOnClickListener(v -> navigateTo(R.id.admin_nav_dashboard));
        }
        if (view.findViewById(R.id.tabFinance) != null) {
            view.findViewById(R.id.tabFinance).setOnClickListener(v   -> navigateTo(R.id.admin_nav_finance));
        }

        // search filter
        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(Editable s) {
                filterUsers(s.toString().trim().toLowerCase());
            }
        });

        loadUsers();
    }

    //Data loading

    private void loadUsers() {
        usersLoader.setVisibility(View.VISIBLE);
        tvUsersEmpty.setVisibility(View.GONE);
        layoutUsersList.removeAllViews();
        allUsers.clear();

        AdminRepository.getInstance().getAllUsersWithStatus(users -> {
            if (getView() == null) return;
            usersLoader.setVisibility(View.GONE);
            allUsers.addAll(users);

            int activeCount   = 0;
            int inactiveCount = 0;
            for (Map<String, Object> u : users) {
                Boolean isActive = (Boolean) u.get("isActive");
                if (Boolean.TRUE.equals(isActive)) activeCount++;
                else inactiveCount++;
            }

            tvActiveCount.setText(String.valueOf(activeCount));
            tvInactiveCount.setText(String.valueOf(inactiveCount));

            if (users.isEmpty()) {
                tvUsersEmpty.setVisibility(View.VISIBLE);
            } else {
                renderUsers(users);
            }
        });
    }

    private void renderUsers(List<Map<String, Object>> users) {
        layoutUsersList.removeAllViews();
        if (users.isEmpty()) {
            tvUsersEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvUsersEmpty.setVisibility(View.GONE);

        for (Map<String, Object> user : users) {
            addUserRow(user);
        }
    }

    private void filterUsers(String query) {
        if (query.isEmpty()) {
            renderUsers(allUsers);
            return;
        }
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> u : allUsers) {
            String fullName = getString(u, "fullName");
            String username = getString(u, "username");
            String uid      = getString(u, "uid");
            if (fullName.toLowerCase().contains(query)
                    || username.toLowerCase().contains(query)
                    || uid.toLowerCase().contains(query)) {
                filtered.add(u);
            }
        }
        renderUsers(filtered);
    }

    private void addUserRow(Map<String, Object> user) {
        if (getContext() == null) return;

        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_user_card, layoutUsersList, false);

        // Determine a short member ID from the Firestore UID
        String uid = getString(user, "uid");
        String memberId = "#" + (uid.length() >= 4 ? uid.substring(uid.length() - 4).toUpperCase() : uid);

        String fullName = getString(user, "fullName");
        if (fullName.isEmpty()) fullName = getString(user, "username");
        if (fullName.isEmpty()) fullName = "Unknown";

        Boolean isActive = (Boolean) user.get("isActive");
        boolean active = Boolean.TRUE.equals(isActive);

        ((TextView) row.findViewById(R.id.tvUserFullName)).setText(fullName);
        ((TextView) row.findViewById(R.id.tvUserMemberId)).setText(memberId);

        Chip chip = row.findViewById(R.id.chipUserStatus);
        chip.setText(active ? "Paid" : "Unpaid");

        if (active) {
            chip.setChipBackgroundColor(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_green_light)));
            chip.setTextColor(ContextCompat.getColor(requireContext(),
                    com.google.android.material.R.color.material_dynamic_primary10));
        } else {
            chip.setChipBackgroundColor(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)));
            chip.setTextColor(ContextCompat.getColor(requireContext(),
                    com.google.android.material.R.color.design_default_color_error));
        }

        // Card border tint
        MaterialCardView card = (MaterialCardView) row;
        card.setStrokeColor(active
                ? ContextCompat.getColor(requireContext(), android.R.color.holo_green_light)
                : ContextCompat.getColor(requireContext(), android.R.color.holo_red_light));
        card.setStrokeWidth(1);

        layoutUsersList.addView(row);
    }


    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof String ? (String) val : "";
    }

    private void navigateTo(int navItemId) {
        if (getActivity() instanceof AdminActivity) {
            ((AdminActivity) getActivity()).navigateAdminTo(navItemId);
        }
    }
}
