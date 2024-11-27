package us.aplicaciones.catsanddogs;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.DataType;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
public class ImageClassifier {
    private Interpreter tflite;
    private final int IMAGE_SIZE = 224; // Ajustar al tamaño del modelo

    public ImageClassifier(AssetManager assetManager, String modelPath) throws IOException {
        tflite = new Interpreter(loadModelFile(assetManager, modelPath));
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileChannel fileChannel;
        try (FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {
            fileChannel = inputStream.getChannel();
        }
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        MappedByteBuffer map = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        return map;
    }

    public String classifyImage(ByteBuffer inputImage, String[] labels) {
        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(new int[]{1, labels.length}, DataType.UINT8);
        tflite.run(inputImage, outputBuffer.getBuffer().rewind());
        float[] probabilities = outputBuffer.getFloatArray();
        int maxIndex = 0;
        for (int i = 1; i < probabilities.length; i++) {
            if (probabilities[i] > probabilities[maxIndex]) maxIndex = i;
        }
        return labels[maxIndex];
    }
}
