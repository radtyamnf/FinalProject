package com.example.finalproject;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectabsen.Adapter.HistoryAdapter;
import com.example.projectabsen.Model.Attendance;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private RecyclerView historyRecyclerView;
    private HistoryAdapter historyAdapter;
    private List<Attendance> attendanceList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            Toast.makeText(this, "Pengguna belum login", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        historyRecyclerView = findViewById(R.id.historyRecyclerView);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(attendanceList);
        historyRecyclerView.setAdapter(historyAdapter);

        loadAttendanceHistory();
    }

    private void loadAttendanceHistory() {
        db.collection("Attendance")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        attendanceList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Attendance attendance = document.toObject(Attendance.class);
                                Log.d("HistoryActivity", "Loaded data: " + attendance);
                                attendanceList.add(attendance);
                            } catch (Exception e) {
                                Log.e("HistoryActivity", "Error parsing document to Attendance object", e);
                            }
                        }
                        historyAdapter.notifyDataSetChanged();
                    } else {
                        Log.e("HistoryActivity", "Error getting documents: ", task.getException());
                        Toast.makeText(this, "Gagal memuat data absensi", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}