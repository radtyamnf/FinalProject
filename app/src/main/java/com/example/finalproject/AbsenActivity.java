package com.example.finalproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.finalproject.Model.Attendance;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AbsenActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_LOCATION_PERMISSION = 2;
    private static final int REQUEST_CAMERA_PERMISSION = 3;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FusedLocationProviderClient fusedLocationClient;

    private EditText locationInput;
    private ImageView photoPreview;
    private Bitmap photoBitmap;
    private Button btnCapturePhoto, btnSubmitAttendance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_absen);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        fusedLocationClient = locationService.getFusedLocationProviderClient(this);

        locationInput = findViewById(R.id.LocationInput);
        photoPreview = findViewById(R.id.photoPreview);
        btnCapturePhoto = findViewById(R.id.btnCapturePhoto);
        btnSubmitAttendance = findViewById(R.id.btnSubmitAttendance);

        getLocation();

        btnCapturePhoto.setOnClickListener(v ->); {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.Camera) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            } else {
                capturePhoto();
            }
        }
        ;

        btnSubmitAttendance.setOnClickListener(v -> submitAttendance);
    }

    private void capturePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap photoBitmap = (Bitmap) extras.get("data");
            photoPreview.setImageBitmap(photoBitmap);
        }
    }

    private void getLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        locationInput.setText(location.getLatitude() + ", " + location.getLongitude());
                    } else {
                        Toast.makeText(this, "Lokasi tidak ditemukan.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Gagal mendapatkan lokasi.", Toast.LENGTH_SHORT).show());
    }

    private void submitAttendance() {
        if (photoBitmap == null) {
            Toast.makeText(this, "Silakan ambil foto sebelum mengirim absensi.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name") != null ? documentSnapshot.getString("name") : "Tidak Diketahui";
                            String lokasi = locationInput.getText().toString();
                            String waktuKehadiran = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                            String fotoBase64 = convertBitmapToBase64(photoBitmap);

                            String status = "Hadir";

                            Attendance attendance = new Attendance(userId, name, status, lokasi, waktuKehadiran, fotoBase64);

                            db.collection("Attendance").add(attendance)
                                    .addOnSuccessListener(documentReference -> Toast.makeText(this, "Absensi berhasil.", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(this, "Gagal menyimpan absensi.", Toast.LENGTH_SHORT).show());
                        } else {
                            Toast.makeText(this, "Data pengguna tidak ditemukan.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Gagal mengambil data pengguna.", Toast.LENGTH_SHORT).show());
        }
    }


    private String convertBitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);
        byte[] byteArray = baos.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Izin lokasi tidak diberikan", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                capturePhoto();
            } else {
                Toast.makeText(this, "Izin kamera tidak diberikan", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
