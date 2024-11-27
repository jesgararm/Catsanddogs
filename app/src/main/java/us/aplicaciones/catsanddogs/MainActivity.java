package us.aplicaciones.catsanddogs;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
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

        try {
            classifier = new ImageClassifier(getAssets(), "cats_vs_dogs_model.tflite");
        } catch (IOException e) {
            e.printStackTrace();
        }

        captureButton.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(imageBitmap);

            ByteBuffer inputBuffer = ImageProcessor.preprocessImage(imageBitmap, 224);
            String[] labels = {"Cat", "Dog"};
            String result = classifier.classifyImage(inputBuffer, labels);
            resultTextView.setText(result);
        }
    }
}