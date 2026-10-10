package com.mastersoft.fitpulseadmin;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class FitPulseAdminApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}
