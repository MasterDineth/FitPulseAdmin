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
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminFinanceFragment extends Fragment {

    private LinearProgressIndicator financeLoader;
    private TextView tvRevenueMonthLabel, tvMonthRevenue;
    private TextView tvAmountPaid, tvPaidCount;
    private TextView tvAmountDue, tvDueCount;
    private LinearLayout layoutBarChart;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_finance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        financeLoader        = view.findViewById(R.id.financeLoadingIndicator);
        tvRevenueMonthLabel  = view.findViewById(R.id.tvRevenueMonthLabel);
        tvMonthRevenue       = view.findViewById(R.id.tvTotalRevenue);
        tvAmountPaid         = view.findViewById(R.id.tvPaidAmount);
        tvPaidCount          = view.findViewById(R.id.tvPaidCount);
        tvAmountDue          = view.findViewById(R.id.tvOutstandingAmount);
        tvDueCount           = view.findViewById(R.id.tvDueCount);
        layoutBarChart       = view.findViewById(R.id.layoutMonthlyBreakdown);

        // Tab navigation
        View tabDashboard = view.findViewById(R.id.tabDashboard);
        if (tabDashboard != null) {
            tabDashboard.setOnClickListener(v -> navigateTo(R.id.admin_nav_dashboard));
        }

        View tabUsers = view.findViewById(R.id.tabUsers);
        if (tabUsers != null) {
            tabUsers.setOnClickListener(v -> navigateTo(R.id.admin_nav_users));
        }

        // this month's label
        String monthLabel = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date());
        if (tvRevenueMonthLabel != null) {
            tvRevenueMonthLabel.setText(monthLabel + " Revenue");
        }

        loadFinanceData();
    }

    //Data loading
    private void loadFinanceData() {
        if (financeLoader != null) financeLoader.setVisibility(View.VISIBLE);

        AdminRepository.getInstance().getFinanceSummary(summary -> {
            if (getView() == null) return;
            if (financeLoader != null) financeLoader.setVisibility(View.GONE);

            // total revenue this month
            if (tvMonthRevenue != null) {
                tvMonthRevenue.setText(AdminRepository.formatRs(summary.amountPaidThisMonth));
            }

            // Amount paid
            if (tvAmountPaid != null) {
                tvAmountPaid.setText(AdminRepository.formatRs(summary.amountPaidThisMonth));
            }
            if (tvPaidCount != null) {
                tvPaidCount.setText(summary.paidCount + " payment" + (summary.paidCount != 1 ? "s" : ""));
            }

            // Amount due
            if (tvAmountDue != null) {
                tvAmountDue.setText(AdminRepository.formatRs(summary.amountDueThisMonth));
            }
            if (tvDueCount != null) {
                tvDueCount.setText(summary.dueCount + " member" + (summary.dueCount != 1 ? "s" : ""));
            }

            // bar chart
            if (layoutBarChart != null) {
                buildBarChart(summary.monthlyTotals);
            }
        });
    }

    //Bar chart

    private void buildBarChart(List<AdminRepository.MonthlyTotal> totals) {
        layoutBarChart.removeAllViews();
        if (totals == null || totals.isEmpty()) return;


        double maxAmount = 1;
        for (AdminRepository.MonthlyTotal t : totals) {
            if (t.amount > maxAmount) maxAmount = t.amount;
        }


        int lastIndex = totals.size() - 1;

        for (int i = 0; i < totals.size(); i++) {
            AdminRepository.MonthlyTotal t = totals.get(i);
            addBarRow(t.label, t.amount, maxAmount, i == lastIndex);
        }
    }

    private void addBarRow(String label, double amount, double maxAmount, boolean isCurrentMonth) {
        if (getContext() == null) return;


        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int vPad = dpToPx(8);
        row.setPadding(0, vPad, 0, vPad);

        // Month label
        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText(label);
        tvLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(dpToPx(80), ViewGroup.LayoutParams.WRAP_CONTENT);
        tvLabel.setLayoutParams(labelParams);

        // Progress bar
        com.google.android.material.progressindicator.LinearProgressIndicator bar =
                new com.google.android.material.progressindicator.LinearProgressIndicator(requireContext());
        int progress = maxAmount > 0 ? (int) ((amount / maxAmount) * 100) : 0;
        bar.setMax(100);
        bar.setIndeterminate(false);
        bar.setProgressCompat(progress, true);

        if (isCurrentMonth) {
            bar.setIndicatorColor(requireContext().getColor(
                    com.google.android.material.R.color.material_dynamic_primary40));
        } else {
            bar.setIndicatorColor(requireContext().getColor(
                    com.google.android.material.R.color.material_dynamic_secondary80));
        }
        bar.setTrackColor(requireContext().getColor(
                com.google.android.material.R.color.material_dynamic_neutral90));
        bar.setTrackThickness(dpToPx(8));
        bar.setTrackCornerRadius(dpToPx(4));

        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        barParams.setMarginStart(dpToPx(8));
        barParams.setMarginEnd(dpToPx(8));
        bar.setLayoutParams(barParams);

        // Amount label
        TextView tvAmount = new TextView(requireContext());
        tvAmount.setText(AdminRepository.formatRs(amount));
        tvAmount.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        if (isCurrentMonth) {
            tvAmount.setTextColor(requireContext().getColor(
                    com.google.android.material.R.color.material_dynamic_primary40));
            tvAmount.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }
        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvAmount.setLayoutParams(amountParams);

        row.addView(tvLabel);
        row.addView(bar);
        row.addView(tvAmount);


        com.google.android.material.divider.MaterialDivider divider =
                new com.google.android.material.divider.MaterialDivider(requireContext());
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
        divider.setLayoutParams(divParams);

        layoutBarChart.addView(row);
        layoutBarChart.addView(divider);
    }


    private int dpToPx(int dp) {
        return (int) (dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private void navigateTo(int navItemId) {
        if (getActivity() instanceof AdminActivity) {
            ((AdminActivity) getActivity()).navigateAdminTo(navItemId);
        }
    }
}
