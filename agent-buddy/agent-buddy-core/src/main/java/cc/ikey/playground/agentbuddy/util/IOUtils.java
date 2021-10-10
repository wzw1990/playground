package cc.ikey.playground.agentbuddy.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {
    private static final int EOF = -1;
    private static final int BUFFER_SIZE = 4096;

    private IOUtils() {
    }

    public static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int n;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
    }

    public static byte[] readToBytes(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return new byte[0];
        } else {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            copy(inputStream, output);
            return output.toByteArray();
        }
    }
}