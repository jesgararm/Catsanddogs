package us.aplicaciones.catsanddogs;

import android.Manifest;
import android.annotation.SuppressLint;
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
import java.util.Objects;

/**
 * Actividad principal de la aplicación para clasificar imágenes de gatos y perros.
 * @noinspection deprecation
 */
public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;

    private ImageView imageView;
    private TextView resultTextView;
    private ImageClassifier classifier;

    /**
     * Método llamado cuando se crea la actividad.
     *
     * @param savedInstanceState Estado guardado de la instancia anterior.
     */
    @SuppressLint("QueryPermissionsNeeded")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        resultTextView = findViewById(R.id.resultTextView);
        Button captureButton = findViewById(R.id.captureButton);
        Button selectButton = findViewById(R.id.selectButton);

        // Verificar si el archivo del modelo es accesible
        try {
            getAssets().open("cats_vs_dogs_model.tflite").close();
            Toast.makeText(this, "Model file is accessible", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Model file not found: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        // Inicializar el clasificador de imágenes
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

    /**
     * Método llamado cuando se recibe el resultado de una actividad iniciada con startActivityForResult.
     *
     * @param requestCode Código de solicitud.
     * @param resultCode Código de resultado.
     * @param data Datos devueltos por la actividad.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Bitmap imageBitmap = null;

            try {
                if (requestCode == REQUEST_IMAGE_CAPTURE) {
                    imageBitmap = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
                } else if (requestCode == REQUEST_IMAGE_PICK) {
                    if (data.getData() == null) {
                        throw new IOException("No image selected");
                    }
                    imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                }

                if (imageBitmap != null) {
                    imageView.setImageBitmap(imageBitmap);
                    String[] etiquetas = {"Gato", "Perro"};
                    String result = classifier.classifyImage(imageBitmap, etiquetas);
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

    /**
     * Solicita permisos de almacenamiento si es necesario.
     */
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q &&
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }
}