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
        ResponseData responseData;

        if ("GET".equalsIgnoreCase(method)) {
            responseData = ResponseData.ok("Media Endpoint LIVE");
        } else if ("POST".equalsIgnoreCase(method)) {
            responseData = post(exchange);
        } else {
            responseData = ResponseData.badRequest("Unsupported Method, use 'GET', 'POST'");
        }

        exchange.sendResponseHeaders(responseData.statusCode(), responseData.responseBytes().length);
        exchange.getResponseBody().write(responseData.responseBytes());
        exchange.close();
    }

    private ResponseData post(HttpExchange exchange) {
        Headers requestHeaders = exchange.getRequestHeaders();
        int contentLength;
        try {
            contentLength = Integer.parseUnsignedInt(requestHeaders.getFirst("Content-Length"));
        } catch (NumberFormatException e) {
            return ResponseData.fromString(HttpStatus.LENGTH_REQUIRED, "Wrong or Negative Content-Length");
        }
        Logger.log("Content-Length: ", contentLength);

        String contentType = requestHeaders.getFirst("Content-Type");
        if (contentType == null || !contentType.startsWith("multipart/form-data")) {
            return ResponseData.badRequest("Wrong Content-Type use 'multipart/form-data'");
        }

        Logger.log("Content-Type: ", contentType);

        // multipart/form-data; boundary=PieBoundary123456789012345678901234567
        String boundary;
        try {
            boundary = contentType.split("=")[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            return ResponseData.badRequest("Missing multipart boundary");
        }

        Logger.log("Boundary: ", boundary);

        InputStream requestBody = exchange.getRequestBody();
        if (requestBody == null) {
            return ResponseData.badRequest("Missing request body");
        }

        Logger.log("Request Body: ", requestBody);
        byte[] rawBodyBytes;
        try {
            rawBodyBytes = requestBody.readNBytes(contentLength);
        } catch (OutOfMemoryError oe) {
            return ResponseData.badRequest("Too Big request Body");
        } catch (IOException e) {
            return ResponseData.badRequest("Failed to read  the post method ending");
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
            return ResponseData.fromString(HttpStatus.CONTENT_TOO_LARGE, "Request multipart body does not ends(too large)");
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
            return ResponseData.fromString(HttpStatus.INTERNAL_SERVER_ERROR, "Filename conflicts, Failed to save file");
        } catch (IOException e) {
            Logger.log("Cannot write data in file: ", fileName, e.getMessage());
            return ResponseData.fromString(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save file");
        }

        Logger.log("Saved the file to disk: ", fileName);
        return ResponseData.ok("Saved the file SUCCESSFULLY!");
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
