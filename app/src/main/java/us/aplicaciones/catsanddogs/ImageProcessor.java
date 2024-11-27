package us.aplicaciones.catsanddogs;
import android.graphics.Bitmap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ImageProcessor {
    public static ByteBuffer preprocessImage(Bitmap bitmap, int imageSize) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[imageSize * imageSize];
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());

        for (int pixel : intValues) {
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;
            byteBuffer.putFloat(r / 255.0f);
            byteBuffer.putFloat(g / 255.0f);
            byteBuffer.putFloat(b / 255.0f);
        }

        return byteBuffer;
    }
}
