package dev.teslac00;

import java.util.ArrayList;
import java.util.List;

public class ByteHelper {

    public static int indexOf(byte[] bytes, byte[] delimiter, int offset) {
        for (int i = offset; i < bytes.length - delimiter.length; i++) {
            int start = i;
            for (byte b : delimiter) {
                if (bytes[start++] != b) break;
            }
            if (start == i + delimiter.length) return i;
        }

        return -1;
    }

    public static List<byte[]> splitBytes(byte[] bytes, byte[] delimiter) {
        List<byte[]> list = new ArrayList<>();
        int lastIndexEnd = 0;
        int index = indexOf(bytes, delimiter, lastIndexEnd);

        while (index >= 0) {
            int length = index - lastIndexEnd;
            if (length > 0) {
                byte[] array = new byte[length];
                System.arraycopy(bytes, lastIndexEnd, array, 0, length);
                list.add(array);
            }

            lastIndexEnd = index + delimiter.length;
            index = indexOf(bytes, delimiter, lastIndexEnd);
        }

        // add last left-over data
        if (lastIndexEnd < bytes.length) {
            int length = bytes.length - lastIndexEnd;
            byte[] array = new byte[length];
            System.arraycopy(bytes, lastIndexEnd, array, 0, length);
            list.add(array);
        }

        return list;
    }

    public static void byteDebug(byte[] bytes) {
        final int CR = 13; // (D) /r -> carriage return
        final int LF = 10; // (A) /n -> carriage return

        System.out.print("{");
        for (byte b : bytes) {
            if (b == CR) System.out.print("CR ");
            else if (b == LF) System.out.print("LF ");
            else System.out.printf("%d ", b);
        }
        System.out.print("}");
    }

}
