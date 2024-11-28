package us.aplicaciones.catsanddogs;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.DataType;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class ImageClassifier {
    private Interpreter tflite;
    private final int IMAGE_SIZE = 224; // Ajustar al tamaño del modelo

    public ImageClassifier(AssetManager assetManager, String modelPath) throws IOException {
        tflite = new Interpreter(loadModelFile(assetManager, modelPath));
    }

    public boolean isModelLoaded() {
        return tflite != null;
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        try (FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
             FileChannel fileChannel = inputStream.getChannel()) {
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }
    public String classifyImage(Bitmap bitmap, String[] labels) {
        try {
            ByteBuffer inputBuffer = preprocessImage(bitmap);
            float[][] output = runInference(inputBuffer);
            return getClassificationResult(output, labels);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error during inference: " + e.getMessage();
        }
    }

    private ByteBuffer preprocessImage(Bitmap bitmap) {
        Tensor inputTensor = tflite.getInputTensor(0);
        int[] inputShape = inputTensor.shape();
        int inputHeight = inputShape[1];
        int inputWidth = inputShape[2];
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true);

        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * inputShape[3]);
        inputBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputWidth * inputHeight];
        resizedBitmap.getPixels(intValues, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        for (int pixel : intValues) {
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;
            inputBuffer.putFloat(r / 255.0f);
            inputBuffer.putFloat(g / 255.0f);
            inputBuffer.putFloat(b / 255.0f);
        }
        return inputBuffer;
    }

    private float[][] runInference(ByteBuffer inputBuffer) {
        float[][] output = new float[1][1];
        tflite.run(inputBuffer, output);
        Log.d("TFLite", "Resultado de salida: " + output[0][0]);
        return output;
    }

    private String getClassificationResult(float[][] output, String[] labels) {
        float probability = output[0][0];
        if (probability > 0.2) {
            return labels[1];
        } else {
            return labels[0];
        }
    }
}
