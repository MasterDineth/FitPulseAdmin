package com.mastersoft.fitpulseadmin.data;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminRepository {

    private static final String TAG = "AdminRepository";

    // ── Collection names ──────────────────────────────────────────────────────
    private static final String COL_USERS      = "users";
    private static final String COL_STATUS     = "membershipStatus";
    private static final String COL_PAYMENTS   = "paymentHistory";
    private static final String COL_ATTENDANCE = "attendance";

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static AdminRepository instance;

    public static synchronized AdminRepository getInstance() {
        if (instance == null) {
            instance = new AdminRepository();
        }
        return instance;
    }

    private final FirebaseFirestore db;

    private AdminRepository() {
        db = FirebaseFirestore.getInstance();
    }

    //Dashboadrd


    public void getTotalMemberCount(final CountCallback callback) {
        db.collection(COL_USERS)
                .get()
                .addOnSuccessListener(snapshot -> {
                    callback.onResult(snapshot.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getTotalMemberCount failed", e);
                    callback.onResult(0);
                });
    }

    public void getNewMembersThisMonth(final CountCallback callback) {
        Timestamp startOfMonth = buildStartOfMonthTimestamp();
        db.collection(COL_USERS)
                .whereGreaterThanOrEqualTo("dateRegistered", startOfMonth)
                .get()
                .addOnSuccessListener(snapshot -> {
                    callback.onResult(snapshot.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getNewMembersThisMonth failed", e);
                    callback.onResult(0);
                });
    }

    public void getTodayCheckinCount(final CountCallback callback) {
        String today = buildTodayString();
        db.collection(COL_ATTENDANCE)
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(snapshot -> {
                    callback.onResult(snapshot.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getTodayCheckinCount failed", e);
                    callback.onResult(0);
                });
    }

    public void getMonthlyRevenue(final RevenueCallback callback) {
        Timestamp startOfMonth = buildStartOfMonthTimestamp();
        db.collection(COL_PAYMENTS)
                .whereEqualTo("status", "SUCCESS")
                .whereGreaterThanOrEqualTo("paymentDate", startOfMonth)
                .get()
                .addOnSuccessListener(snapshot -> {
                    double total = 0.0;
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Double amount = doc.getDouble("amount");
                        if (amount != null) {
                            total += amount;
                        }
                    }
                    callback.onResult(total);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getMonthlyRevenue failed", e);
                    callback.onResult(0.0);
                });
    }


    public void getTodayCheckIns(final CheckInsCallback callback) {
        String today = buildTodayString();
        db.collection(COL_ATTENDANCE)
                .whereEqualTo("date", today)
                .orderBy("checkInTime", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        list.add(doc.getData());
                    }
                    callback.onResult(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getTodayCheckIns failed", e);
                    callback.onResult(new ArrayList<>());
                });
    }

    // Users

    public void getAllUsersWithStatus(final UsersCallback callback) {
        db.collection(COL_USERS)
                .orderBy("dateRegistered", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(usersSnapshot -> {

                    if (usersSnapshot.isEmpty()) {
                        callback.onResult(new ArrayList<>());
                        return;
                    }

                    final List<DocumentSnapshot> userDocs = usersSnapshot.getDocuments();

                    db.collection(COL_STATUS)
                            .get()
                            .addOnSuccessListener(statusSnapshot -> {

                                Map<String, Timestamp> renewalMap = new HashMap<>();
                                for (DocumentSnapshot statusDoc : statusSnapshot.getDocuments()) {
                                    Timestamp renewal = statusDoc.getTimestamp("renewalDate");
                                    if (renewal != null) {
                                        renewalMap.put(statusDoc.getId(), renewal);
                                    }
                                }

                                Date now = new Date();
                                List<Map<String, Object>> result = new ArrayList<>();

                                for (DocumentSnapshot userDoc : userDocs) {
                                    Map<String, Object> userMap = new HashMap<>();

                                    if (userDoc.getData() != null) {
                                        userMap.putAll(userDoc.getData());
                                    }

                                    userMap.put("uid", userDoc.getId());
                                    Timestamp renewal = renewalMap.get(userDoc.getId());
                                    boolean isActive = (renewal != null && renewal.toDate().after(now));
                                    userMap.put("isActive", isActive);

                                    result.add(userMap);
                                }

                                callback.onResult(result);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "getAllUsersWithStatus — status fetch failed", e);
                                List<Map<String, Object>> result = new ArrayList<>();
                                for (DocumentSnapshot userDoc : userDocs) {
                                    Map<String, Object> userMap = new HashMap<>();
                                    if (userDoc.getData() != null) {
                                        userMap.putAll(userDoc.getData());
                                    }
                                    userMap.put("uid", userDoc.getId());
                                    userMap.put("isActive", false);
                                    result.add(userMap);
                                }
                                callback.onResult(result);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getAllUsersWithStatus — users fetch failed", e);
                    callback.onResult(new ArrayList<>());
                });
    }

    // Finanace
    public void getFinanceSummary(final FinanceCallback callback) {
        Timestamp startOfMonth = buildStartOfMonthTimestamp();

        db.collection(COL_PAYMENTS)
                .whereEqualTo("status", "SUCCESS")
                .whereGreaterThanOrEqualTo("paymentDate", startOfMonth)
                .get()
                .addOnSuccessListener(paidSnapshot -> {

                    double totalPaid = 0.0;
                    int paidCount = paidSnapshot.size();
                    Set<String> paidUserIds = new HashSet<>();

                    for (QueryDocumentSnapshot doc : paidSnapshot) {
                        Double amount = doc.getDouble("amount");
                        if (amount != null) {
                            totalPaid += amount;
                        }
                        String uid = doc.getString("userId");
                        if (uid != null && !uid.isEmpty()) {
                            paidUserIds.add(uid);
                        }
                    }

                    final double finalTotalPaid = totalPaid;
                    final int finalPaidCount = paidCount;
                    final Set<String> finalPaidUserIds = paidUserIds;

                    db.collection(COL_STATUS)
                            .get()
                            .addOnSuccessListener(statusSnapshot -> {

                                double totalDue = 0.0;
                                int dueCount = 0;

                                for (QueryDocumentSnapshot statusDoc : statusSnapshot) {
                                    String uid = statusDoc.getString("userId");
                                    if (uid == null || uid.isEmpty()) {
                                        uid = statusDoc.getId();
                                    }

                                    if (finalPaidUserIds.contains(uid)) continue;

                                    Double planAmount = statusDoc.getDouble("amount");
                                    if (planAmount != null && planAmount > 0) {
                                        totalDue += planAmount;
                                        dueCount++;
                                    }
                                }

                                final double finalTotalDue = totalDue;
                                final int finalDueCount = dueCount;

                                buildMonthlyTotals(monthlyTotals -> {
                                    FinanceSummary summary = new FinanceSummary();
                                    summary.amountPaidThisMonth  = finalTotalPaid;
                                    summary.paidCount            = finalPaidCount;
                                    summary.amountDueThisMonth   = finalTotalDue;
                                    summary.dueCount             = finalDueCount;
                                    summary.monthlyTotals        = monthlyTotals;
                                    callback.onResult(summary);
                                });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "getFinanceSummary — status fetch failed", e);
                                buildMonthlyTotals(monthlyTotals -> {
                                    FinanceSummary summary = new FinanceSummary();
                                    summary.amountPaidThisMonth = finalTotalPaid;
                                    summary.paidCount           = finalPaidCount;
                                    summary.monthlyTotals       = monthlyTotals;
                                    callback.onResult(summary);
                                });
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getFinanceSummary — payments fetch failed", e);
                    callback.onResult(new FinanceSummary());
                });
    }

    private void buildMonthlyTotals(final MonthlyTotalsCallback callback) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.MONTH, -5);
        Timestamp sixMonthsAgo = new Timestamp(cal.getTime());

        db.collection(COL_PAYMENTS)
                .whereEqualTo("status", "SUCCESS")
                .whereGreaterThanOrEqualTo("paymentDate", sixMonthsAgo)
                .get()
                .addOnSuccessListener(snapshot -> {

                    SimpleDateFormat monthFmt = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                    Map<String, Double> totalsMap = new HashMap<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Timestamp ts = doc.getTimestamp("paymentDate");
                        Double amount = doc.getDouble("amount");
                        if (ts != null && amount != null) {
                            String key = monthFmt.format(ts.toDate());
                            Double existing = totalsMap.get(key);
                            totalsMap.put(key, (existing != null ? existing : 0.0) + amount);
                        }
                    }

                    Calendar cursor = Calendar.getInstance();
                    cursor.set(Calendar.DAY_OF_MONTH, 1);
                    cursor.set(Calendar.HOUR_OF_DAY, 0);
                    cursor.set(Calendar.MINUTE, 0);
                    cursor.set(Calendar.SECOND, 0);
                    cursor.set(Calendar.MILLISECOND, 0);
                    cursor.add(Calendar.MONTH, -5);

                    List<MonthlyTotal> result = new ArrayList<>();
                    for (int i = 0; i < 6; i++) {
                        String label = monthFmt.format(cursor.getTime());
                        double amount = totalsMap.containsKey(label) ? totalsMap.get(label) : 0.0;
                        result.add(new MonthlyTotal(label, amount));
                        cursor.add(Calendar.MONTH, 1);
                    }

                    callback.onResult(result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "buildMonthlyTotals failed", e);
                    callback.onResult(new ArrayList<>());
                });
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  DATA CLASSES
    // ════════════════════════════════════════════════════════════════════════════

    public static class FinanceSummary {
        public double              amountPaidThisMonth = 0.0;
        public int                 paidCount           = 0;
        public double              amountDueThisMonth  = 0.0;
        public int                 dueCount            = 0;
        public List<MonthlyTotal>  monthlyTotals       = new ArrayList<>();
    }

    public static class MonthlyTotal {
        public final String label;
        public final double amount;

        public MonthlyTotal(String label, double amount) {
            this.label  = label;
            this.amount = amount;
        }
    }

    public interface CountCallback { void onResult(int count); }
    public interface RevenueCallback { void onResult(double total); }
    public interface CheckInsCallback { void onResult(List<Map<String, Object>> records); }
    public interface UsersCallback { void onResult(List<Map<String, Object>> users); }
    public interface FinanceCallback { void onResult(FinanceSummary summary); }
    private interface MonthlyTotalsCallback { void onResult(List<MonthlyTotal> totals); }


    public static Timestamp buildStartOfMonthTimestamp() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Timestamp(cal.getTime());
    }

    public static String buildTodayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    public static String formatTime(Timestamp ts) {
        if (ts == null) return "—";
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(ts.toDate());
    }

    public static String formatDate(Timestamp ts) {
        if (ts == null) return "—";
        return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(ts.toDate());
    }

    public static String formatRs(double amount) {
        return String.format(Locale.getDefault(), "Rs %,.0f", amount);
    }
}