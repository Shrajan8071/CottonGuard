package com.example.cottondiseaseapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private static final int CAPTURE_IMAGE = 2;
    private static final int STORAGE_PERMISSION_CODE = 100;

    private ImageView imageView;
    private TextView textResult;
    private TFLiteModel tfliteModel;
    private Bitmap selectedBitmap;

    private FusedLocationProviderClient fusedLocationClient;
    private double latitude;
    private double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        imageView = findViewById(R.id.image_view);
        textResult = findViewById(R.id.text_result);
        Button buttonCaptureImage = findViewById(R.id.button_capture_image);
        Button buttonSelectImage = findViewById(R.id.button_select_image);
        Button buttonCheckResult = findViewById(R.id.button_check_result);

        // Check and request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }

        buttonCaptureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textResult.setText("");
                openCamera();
            }
        });

        buttonSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textResult.setText("Results:\n");
                openGallery();
            }
        });

        buttonCheckResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedBitmap != null) {
                    classifyImage(selectedBitmap);
                    uploadImageToFirebase(selectedBitmap); // Upload image to Firebase and send data after getting URL
                } else {
                    Toast.makeText(MainActivity.this, "Please select or capture an image first.", Toast.LENGTH_SHORT).show();
                }
                Toast.makeText(MainActivity.this, "hi check", Toast.LENGTH_SHORT).show();

                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

                getCurrentLocation();x`
//                sendDataToFirebase();
            }
        });

        try {
            tfliteModel = new TFLiteModel(this);
        }
        catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Model loading failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permissions if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, STORAGE_PERMISSION_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        // Now you can use latitude and longitude
//                        sendDataToFirebase();
                    } else {
                        Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private void sendDataToFirebaseOLD() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("plants");

        HashMap<String, Object> hm = new HashMap<>();
        // Use the disease identified by the model
        String disease = textResult.getText().toString(); // This should contain the disease result from classification
        hm.put("disease", disease);
        hm.put("location", latitude + ", " + longitude); // Storing latitude and longitude as a string

        String key = myRef.push().getKey();
        if (key != null) {
            Log.d("FirebaseKey", key);
        } else {
            Log.e("FirebaseKey", "Key is null!");
        }

        hm.put("key", key);
        myRef.child(key).setValue(hm).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Added", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("FirebaseError", "Error: ", task.getException());
                }
            }
        });
    }

    private void sendDataToFirebase(String imageUrl) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("plants");

        HashMap<String, Object> hm = new HashMap<>();
        String disease = textResult.getText().toString(); // This should contain the disease result from classification
        hm.put("disease", disease);
        hm.put("location", latitude + ", " + longitude); // Storing latitude and longitude as a string
        hm.put("imageUrl", imageUrl); // Store the image URL in the database

        String key = myRef.push().getKey();
        if (key != null) {
            hm.put("key", key);
            myRef.child(key).setValue(hm).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Data added to Firebase", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void uploadImageToFirebase(Bitmap bitmap) {
        if (bitmap == null) {
            Toast.makeText(this, "No image to upload", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert the Bitmap to a byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        // Create a reference to Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("images/" + System.currentTimeMillis() + ".jpg");

        // Upload the image to Firebase Storage
        storageRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL after a successful upload
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        sendDataToFirebase(imageUrl); // Call the method to save data with the image URL
                    }).addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Failed to get download URL", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAPTURE_IMAGE);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            try {
                if (requestCode == PICK_IMAGE && data != null) {
                    Uri imageUri = data.getData();
                    selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                } else if (requestCode == CAPTURE_IMAGE && data != null) {
                    selectedBitmap = (Bitmap) data.getExtras().get("data");
                }

                imageView.setImageBitmap(selectedBitmap); // Display the selected/captured image
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void classifyImage(Bitmap bitmap) {
        try {
            // Resize only for model input
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, TFLiteModel.INPUT_WIDTH, TFLiteModel.INPUT_HEIGHT, true);
            float[][][][] input = tfliteModel.preprocessImage(resizedBitmap);
            float[][] output = tfliteModel.classify(input);
            textResult.setText("Results:\n" + getResultClass(output[0]));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error during classification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getResultClass(float[] output) {
        String[] classes = {"Aphids", "Army Worm", "Bacterial Blight", "Healthy", "Powdery Mildew", "Target Spot"};

        StringBuilder result = new StringBuilder();
        float firstMax = output[0], secondMax = output[0];
        int firstIndex = 0, secondIndex = 0;
        for (int i = 0; i < output.length; i++) {
            if (output[i] > firstMax) {
                secondMax = firstMax;
                firstMax = output[i];
                secondIndex = firstIndex;
                firstIndex = i;
            } else if (output[i] > secondMax && output[i] != firstMax) {
                secondMax = output[i];
                secondIndex = i;
            }
        }

        DecimalFormat df = new DecimalFormat("0.4");
        result.append(classes[firstIndex]).append(": ").append(df.format(firstMax)).append("\n")
                .append(classes[secondIndex]).append(": ").append(df.format(secondMax));

        return result.toString();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied to read your external storage", Toast.LENGTH_SHORT).show();
            }
        }
    }
}