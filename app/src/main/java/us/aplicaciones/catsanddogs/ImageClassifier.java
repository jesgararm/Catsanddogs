package us.aplicaciones.catsanddogs;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Clase para clasificar imágenes con un modelo de TensorFlow Lite.
 */
public class ImageClassifier {

    /**
     * Interprete de TensorFlow Lite para ejecutar el modelo.
     */
    private final Interpreter tflite;

    /**
     * Constructor de la clase ImageClassifier.
     *
     * @param assetManager El AssetManager para acceder a los archivos del modelo.
     * @param modelPath La ruta del archivo del modelo.
     * @throws IOException Si ocurre un error al cargar el archivo del modelo.
     */
    public ImageClassifier(AssetManager assetManager, String modelPath) throws IOException {
        tflite = new Interpreter(loadModelFile(assetManager, modelPath));
    }

    /**
     * Verifica si el modelo se ha cargado correctamente.
     *
     * @return true si el modelo está cargado, false en caso contrario.
     */
    public boolean isModelLoaded() {
        return tflite != null;
    }

    /**
     * Carga el archivo del modelo en un MappedByteBuffer.
     *
     * @param assetManager El AssetManager para acceder a los archivos del modelo.
     * @param modelPath La ruta del archivo del modelo.
     * @return Un MappedByteBuffer que contiene el modelo.
     * @throws IOException Si ocurre un error al cargar el archivo del modelo.
     */
    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        try (FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
             FileChannel fileChannel = inputStream.getChannel()) {
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    /**
     * Clasifica una imagen dada y devuelve la etiqueta correspondiente.
     *
     * @param bitmap La imagen a clasificar.
     * @param labels Las etiquetas posibles.
     * @return La etiqueta de la clasificación.
     */
    public String classifyImage(Bitmap bitmap, String[] labels) {
        try {
            ByteBuffer inputBuffer = preprocessImage(bitmap);
            float[][] output = runInference(inputBuffer);
            return getClassificationResult(output, labels);
        } catch (Exception e) {
            Log.e("ImageClassifier", "Error during inference", e);
            return "Error during inference: " + e.getMessage();
        }
    }

    /**
     * Preprocesa la imagen para que sea compatible con el modelo.
     *
     * @param bitmap La imagen a preprocesar.
     * @return Un ByteBuffer que contiene la imagen preprocesada.
     */
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

    /**
     * Ejecuta la inferencia en el modelo con la imagen preprocesada.
     *
     * @param inputBuffer El ByteBuffer que contiene la imagen preprocesada.
     * @return Un array de floats con los resultados de la inferencia.
     */
    private float[][] runInference(ByteBuffer inputBuffer) {
        float[][] output = new float[1][1];
        tflite.run(inputBuffer, output);
        Log.d("TFLite", "Resultado de salida: " + output[0][0]);
        return output;
    }

    /**
     * Obtiene el resultado de la clasificación basado en la salida del modelo.
     *
     * @param output La salida del modelo.
     * @param labels Las etiquetas posibles.
     * @return La etiqueta correspondiente a la clasificación.
     */
    private String getClassificationResult(float[][] output, String[] labels) {
        float probability = output[0][0];
        if (probability > 0.2) {
            return labels[1];
        } else {
            return labels[0];
        }
    }
}