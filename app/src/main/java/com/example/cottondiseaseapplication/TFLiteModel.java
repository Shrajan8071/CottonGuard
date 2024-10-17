package com.example.cottondiseaseapplication;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class TFLiteModel {
    public static final int INPUT_WIDTH = 224;  // Adjust according to your model
    public static final int INPUT_HEIGHT = 224; // Adjust according to your model
    private Interpreter tflite;
    private static final int NUM_CLASSES = 6; // 6 classes as per your model

    public TFLiteModel(Context context) throws IOException {
        tflite = new Interpreter(loadModelFile(context));
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd("inception_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // Adjusted for input shape [1, height, width, channels]
    public float[][] classify(float[][][][] input) {
        float[][] output = new float[1][NUM_CLASSES]; // Output for 6 classes
        tflite.run(input, output);
        return output;
    }

    public float[][][][] preprocessImage(Bitmap bitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_WIDTH, INPUT_HEIGHT, true);
        float[][][][] input = new float[1][INPUT_HEIGHT][INPUT_WIDTH][3]; // [1, height, width, channels]

        for (int i = 0; i < INPUT_HEIGHT; i++) {
            for (int j = 0; j < INPUT_WIDTH; j++) {
                int pixel = resizedBitmap.getPixel(j, i); // Note the change in order
                input[0][i][j][0] = Color.red(pixel) / 255.0f;     // Red
                input[0][i][j][1] = Color.green(pixel) / 255.0f;   // Green
                input[0][i][j][2] = Color.blue(pixel) / 255.0f;    // Blue
            }
        }
        return input;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
}
