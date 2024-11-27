package us.aplicaciones.catsanddogs;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;

    private ImageView imageView;
    private TextView resultTextView;
    private ImageClassifier classifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        resultTextView = findViewById(R.id.resultTextView);
        Button captureButton = findViewById(R.id.captureButton);
        Button selectButton = findViewById(R.id.selectButton);
        try {
            getAssets().open("cats_vs_dogs_model.tflite").close();
            Toast.makeText(this, "Model file is accessible", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Model file not found: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        try {
            classifier = new ImageClassifier(getAssets(), "cats_vs_dogs_model.tflite");
            if (classifier.isModelLoaded()) {
                Toast.makeText(this, "Model loaded successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to load model", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error initializing model: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        // Botón para capturar imagen
        captureButton.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        });

        // Botón para seleccionar imagen de la galería
        selectButton.setOnClickListener(v -> {
            Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK);
        });

        // Solicitar permisos si es necesario
        requestStoragePermission();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Bitmap imageBitmap = null;

            try {
                if (requestCode == REQUEST_IMAGE_CAPTURE) {
                    imageBitmap = (Bitmap) data.getExtras().get("data");
                } else if (requestCode == REQUEST_IMAGE_PICK) {
                    if (data.getData() == null) {
                        throw new IOException("No image selected");
                    }
                    imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                }

                if (imageBitmap != null) {
                    imageView.setImageBitmap(imageBitmap);
                    ByteBuffer inputBuffer = ImageProcessor.preprocessImage(imageBitmap, 224);
                    String[] labels = {"Cat", "Dog"};
                    String result = classifier.classifyImage(inputBuffer, labels);
                    resultTextView.setText(result);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to process image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Unexpected error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q &&
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }
}