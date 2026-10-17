package dev.teslac00;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MediaHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String method = exchange.getRequestMethod();
        Headers requestHeaders = exchange.getRequestHeaders();

        if ("GET".equalsIgnoreCase(method)) {
            String message = "Media Endpoint LIVE";
            exchange.sendResponseHeaders(HttpStatus.OK, message.getBytes().length);
            exchange.getResponseBody().write(message.getBytes());
            exchange.close();
        } else if ("POST".equalsIgnoreCase(method)) {
            int contentLength;
            try {
                contentLength = Integer.parseUnsignedInt(requestHeaders.getFirst("Content-Length"));
            } catch (NumberFormatException e) {
                String message = "Wrong or Negative Content-Length";
                exchange.sendResponseHeaders(HttpStatus.LENGTH_REQUIRED, message.getBytes().length);
                exchange.getResponseBody().write(message.getBytes());
                exchange.close();
                return;
            }
            Logger.log("Content-Length: ", contentLength);

            String contentType = requestHeaders.getFirst("Content-Type");
            if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                String message = "Wrong Content-Type use 'multipart/form-data'";
                exchange.sendResponseHeaders(HttpStatus.BAD_REQUEST, message.getBytes().length);
                exchange.getResponseBody().write(message.getBytes());
                exchange.close();
                return;
            }

            Logger.log("Content-Type: ", contentType);

            // multipart/form-data; boundary=PieBoundary123456789012345678901234567
            String boundary;
            try {
                boundary = contentType.split("=")[1];
            } catch (ArrayIndexOutOfBoundsException e) {
                String message = "Missing multipart boundary";
                exchange.sendResponseHeaders(HttpStatus.BAD_REQUEST, message.getBytes().length);
                exchange.getResponseBody().write(message.getBytes());
                exchange.close();
                return;
            }

            Logger.log("Boundary: ", boundary);

            InputStream requestBody = exchange.getRequestBody();
            if (requestBody == null) {
                String message = "Missing request body";
                exchange.sendResponseHeaders(HttpStatus.BAD_REQUEST, message.getBytes().length);
                exchange.getResponseBody().write(message.getBytes());
                exchange.close();
                return;
            }

            Logger.log("Request Body: ", requestBody);
            byte[] rawBodyBytes;
            try {
                rawBodyBytes = requestBody.readNBytes(contentLength);
            } catch (OutOfMemoryError oe) {
                String message = "Too Big request Body";
                exchange.sendResponseHeaders(HttpStatus.BAD_REQUEST, message.getBytes().length);
                exchange.getResponseBody().write(message.getBytes());
                exchange.close();
                return;
            } catch (IOException e) {
                String message = "Failed to read  the post method ending";
                exchange.sendResponseHeaders(HttpStatus.BAD_REQUEST, message.getBytes().length);
                exchange.getResponseBody().write(message.getBytes());
                exchange.close();
                return;
            }

            Logger.log("Raw body size: ", rawBodyBytes.length);

            // split by --boundary
            byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
            // trim field which is surrounded by CRLF
            // trim field headers-data by CRLF 2 times
            // ends with --CRLF
            final int CR = 13; // (D) /r -> carriage return
            final int LF = 10; // (A) /n -> carriage return
            final int DASH = 45; // - -> minus or dash
            final byte[] TRAIL_BYTES = new byte[]{DASH, DASH, CR, LF};
            List<byte[]> splitBytes = splitBytes(rawBodyBytes, boundaryBytes);
//            Logger.log("SplitBytes length: ", splitBytes.size());
//            for (byte[] b : splitBytes) {
//                Logger.log("Next split length: ", b.length);
//                Logger.log("As String: [", new String(b, StandardCharsets.UTF_8), "]");
//            }
            boolean ending = Arrays.equals(TRAIL_BYTES, splitBytes.getLast());
            if (!ending) {
                String message = "Request multipart body does not ends(too large)";
                exchange.sendResponseHeaders(HttpStatus.CONTENT_TOO_LARGE, message.getBytes().length);
                exchange.getResponseBody().write(message.getBytes());
                exchange.close();
                return;
            }

            // trim field which is surrounded by CRLF
            byte[] fieldRawBytes = splitBytes.getFirst();
            byte[] fieldTrimmedBytes = new byte[fieldRawBytes.length - 4];
            final byte[] FIELD_SPLIT_BYTES = new byte[]{CR, LF, CR, LF};
            System.arraycopy(fieldRawBytes, 2, fieldTrimmedBytes, 0, fieldTrimmedBytes.length);

            // trim field headers-data by CRLF 2 times
            List<byte[]> fieldBytesList = splitBytes(fieldTrimmedBytes, FIELD_SPLIT_BYTES);
            Logger.log("Multipart field length size: ", fieldBytesList.size());

            String fileName = LocalDate.now() + ".jpg";
            File saveFile = new File(fileName);

            try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                fos.write(fieldBytesList.getLast());
            } catch (FileNotFoundException fe) {
                Logger.log("File cannot be created/open with name: ", fileName, fe.getMessage());
                String message = "Filename conflicts, Failed to save file";
                exchange.sendResponseHeaders(HttpStatus.INTERNAL_SERVER_ERROR, message.getBytes().length);
                exchange.getResponseBody().write(message.getBytes());
                exchange.close();
                return;
            } catch (IOException e) {
                Logger.log("Cannot write data in file: ", fileName, e.getMessage());
                String message = "Failed to save file";
                exchange.sendResponseHeaders(HttpStatus.INTERNAL_SERVER_ERROR, message.getBytes().length);
                exchange.getResponseBody().write(message.getBytes());
                exchange.close();
                return;
            }

            Logger.log("Saved the file to disk: ", fileName);
            String message = "Saved the file SUCCESSFULLY!";
            exchange.sendResponseHeaders(HttpStatus.OK, message.getBytes().length);
            exchange.getResponseBody().write(message.getBytes());
            exchange.close();
        } else {
            String message = "Unsupported Method, use 'GET', 'POST'";
            exchange.sendResponseHeaders(HttpStatus.BAD_REQUEST, message.getBytes().length);
            exchange.getResponseBody().write(message.getBytes());
            exchange.close();
        }
    }

    private int indexOf(byte[] bytes, byte[] delimiter, int offset) {
        for (int i = offset; i < bytes.length - delimiter.length; i++) {
            int start = i;
            for (byte b : delimiter) {
                if (bytes[start++] != b) break;
            }
            if (start == i + delimiter.length) return i;
        }

        return -1;
    }

    private List<byte[]> splitBytes(byte[] bytes, byte[] delimiter) {
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
}
