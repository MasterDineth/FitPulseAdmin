package com.mastersoft.fitpulseadmin.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mastersoft.fitpulseadmin.R;
import com.mastersoft.fitpulseadmin.activity.AdminActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QrScannerFragment extends Fragment {

    private DecoratedBarcodeView barcodeView;
    private View cardScanResult;
    private TextView tvResultIcon, tvScanResultTitle, tvScanResultName, tvScanResultDetail;
    private boolean isScanPaused = false;

    private ActivityResultLauncher<String> cameraPermLauncher;
    private FirebaseFirestore db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        cameraPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        startScanning();
                    } else {
                        if (getView() != null) {
                            Snackbar.make(getView(), "Camera permission is required", Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qr_scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof AdminActivity) {
            ((AdminActivity) getActivity()).hideBottomNav();
        }

        barcodeView = view.findViewById(R.id.barcodeScanner);
        cardScanResult = view.findViewById(R.id.cardScanResult);
        tvResultIcon = view.findViewById(R.id.tvResultIcon);
        tvScanResultTitle = view.findViewById(R.id.tvScanResultTitle);
        tvScanResultName = view.findViewById(R.id.tvScanResultName);
        tvScanResultDetail = view.findViewById(R.id.tvScanResultDetail);

        MaterialToolbar toolbar = view.findViewById(R.id.scannerToolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        view.findViewById(R.id.btnScanAnother).setOnClickListener(v -> resumeScanning());
        view.findViewById(R.id.btnDoneScanning).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    //qr scanner
    private void startScanning() {
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (isScanPaused || result.getText() == null) return;
                isScanPaused = true;
                barcodeView.pause();
                processQrResult(result.getText());
            }

            @Override
            public void possibleResultPoints(List<com.google.zxing.ResultPoint> resultPoints) {
            }
        });
        barcodeView.resume();
    }

    private void resumeScanning() {
        cardScanResult.setVisibility(View.GONE);
        isScanPaused = false;
        barcodeView.resume();
    }

    private void processQrResult(String rawPayload) {
        // Parse data
        String[] parts = rawPayload.split("\\|");
        if (parts.length < 2) {
            showError("Invalid QR Code Format");
            return;
        }

        String userId = parts[0];
        String qrDate = parts[1];
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Qr Check
        if (!qrDate.equals(todayDate)) {
            showError("QR code is expired (from a different date).");
            return;
        }

        // Get User from Firestore
        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
            if (!userDoc.exists()) {
                showError("User not found in the system.");
                return;
            }

            // Get name
            String userName = userDoc.getString("fullName");
            if (userName == null || userName.isEmpty()) {
                userName = userDoc.getString("username");
            }
            if (userName == null) userName = "Member";

            final String finalUserName = userName;

            //Check if Attendance already exists for today
            String attendanceDocId = userId + "_" + todayDate;

            db.collection("attendance").document(attendanceDocId).get().addOnSuccessListener(attDoc -> {
                if (attDoc.exists()) {
                    showAlreadyCheckedIn(finalUserName);
                } else {
                    //Record new Attendance in Firestore
                    Map<String, Object> attendanceData = new HashMap<>();
                    attendanceData.put("userId", userId);
                    attendanceData.put("userName", finalUserName);
                    attendanceData.put("date", todayDate);
                    attendanceData.put("checkInTime", FieldValue.serverTimestamp());

                    db.collection("attendance").document(attendanceDocId)
                            .set(attendanceData)
                            .addOnSuccessListener(aVoid -> showSuccess(finalUserName))
                            .addOnFailureListener(e -> showError("Failed to save check-in: " + e.getMessage()));
                }
            }).addOnFailureListener(e -> showError("Error verifying attendance: " + e.getMessage()));

        }).addOnFailureListener(e -> showError("Network error fetching user: " + e.getMessage()));
    }

    //UI Result

    private void showSuccess(String userName) {
        if (getView() == null) return;
        tvResultIcon.setText("✅");
        tvScanResultTitle.setText("Check-in Successful!");
        tvScanResultName.setText(userName);

        String time = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
        tvScanResultDetail.setText("Checked in at " + time);
        showResultCard();
    }

    private void showAlreadyCheckedIn(String userName) {
        if (getView() == null) return;
        tvResultIcon.setText("ℹ️");
        tvScanResultTitle.setText("Already Checked In");
        tvScanResultName.setText(userName);
        tvScanResultDetail.setText("This member has already checked in today.");
        showResultCard();
    }

    private void showError(String message) {
        if (getView() == null) return;
        tvResultIcon.setText("❌");
        tvScanResultTitle.setText("Check-in Failed");
        tvScanResultName.setText("Scan Error");
        tvScanResultDetail.setText(message);
        showResultCard();
    }

    private void showResultCard() {
        cardScanResult.setAlpha(0f);
        cardScanResult.setVisibility(View.VISIBLE);
        cardScanResult.animate().alpha(1f).setDuration(300).start();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (!isScanPaused) barcodeView.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        barcodeView.pause();
        if (getActivity() instanceof AdminActivity) {
            ((AdminActivity) getActivity()).showBottomNav();
        }
    }
}