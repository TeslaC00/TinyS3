package dev.teslac00;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaHandler implements HttpHandler {

    final static int CR = 13; // (D) /r -> carriage return
    final static int LF = 10; // (A) /n -> carriage return
    final static String CONTENT_TYPE_TEXT = "text/plain";
    final static String CONTENT_TYPE_JSON = "application/json";

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
        Logger.debug("Content-Length: ", contentLength);

        String contentType = requestHeaders.getFirst("Content-Type");
        if (contentType == null || !contentType.startsWith("multipart/form-data")) {
            return ResponseData.badRequest("Wrong Content-Type use 'multipart/form-data'");
        }

        Logger.debug("Content-Type: ", contentType);

        // multipart/form-data; boundary=PieBoundary123456789012345678901234567
        String boundary;
        try {
            boundary = contentType.split("=")[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            return ResponseData.badRequest("Missing multipart boundary");
        }

        Logger.debug("Boundary: ", boundary);

        InputStream requestBody = exchange.getRequestBody();
        if (requestBody == null) {
            return ResponseData.badRequest("Missing request body");
        }

        Logger.debug("Request Body: ", requestBody);
        byte[] rawBodyBytes;
        try {
            rawBodyBytes = requestBody.readNBytes(contentLength);
        } catch (OutOfMemoryError oe) {
            return ResponseData.badRequest("Too Big request Body");
        } catch (IOException e) {
            return ResponseData.badRequest("Failed to read  the post method ending");
        }

        Logger.debug("Raw body size: ", rawBodyBytes.length);

        // split by --boundary
        byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
        // trim field which is surrounded by CRLF
        // trim field headers-data by CRLF 2 times
        // ends with --CRLF
        final int DASH = 45; // - -> minus or dash
        final byte[] TRAIL_BYTES = new byte[]{DASH, DASH, CR, LF};
        List<byte[]> splitBytes = splitBytes(rawBodyBytes, boundaryBytes);
//            Logger.debug("SplitBytes length: ", splitBytes.size());
//            for (byte[] b : splitBytes) {
//                Logger.debug("Next split length: ", b.length);
//                Logger.debug("As String: [", new String(b, StandardCharsets.UTF_8), "]");
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
        Logger.debug("Multipart field length size: ", fieldBytesList.size());

        // field headers
        // Content-Disposition: form-data; name="image"; filename="rat.jpg"
        // Content-Type: image/jpeg
        // Each field header field separated by CRLF (Content-Disposition-CRLF-Content-Type)
        String fieldHeaders = new String(fieldBytesList.getFirst(), StandardCharsets.UTF_8);
        Logger.trace("Field headers: [", fieldHeaders, "]");

        // Could extract Content-Type using split then default it and use smaller pattern for different field header
        // Not sure if we get different Header inplace of Content-Type
        Pattern fieldHeaderPattern = Pattern.compile(
                "Content-Disposition:\\s([^;]*);\\sname=\"([^\"]*)\";\\sfilename=\"([^\"]*)\"(?:\\r\\nContent-Type:\\s(.*))?"
        );

        Matcher fieldHeaderMatcher = fieldHeaderPattern.matcher(fieldHeaders);
        boolean matches = fieldHeaderMatcher.matches();
        if (!matches) {
            return ResponseData.badRequest("Invalid field headers");
        }

        String fileName = fieldHeaderMatcher.group(3);
        String fieldDataType = fieldHeaderMatcher.group(4);
        if (fieldDataType == null) {
            fieldDataType = CONTENT_TYPE_TEXT;
        }

        Logger.debug("Content-Disposition: [", fieldHeaderMatcher.group(1), "]");
        Logger.debug("name: [", fieldHeaderMatcher.group(2), "]");
        Logger.debug("filename: [", fileName, "]");
        Logger.debug("Content-Type: [", fieldDataType, "]");

        File saveFile = new File(fileName);

        try (FileOutputStream fos = new FileOutputStream(saveFile)) {
            fos.write(fieldBytesList.getLast());
        } catch (FileNotFoundException fe) {
            Logger.error("File cannot be created/open with name: ", fileName, fe.getMessage());
            return ResponseData.fromString(HttpStatus.INTERNAL_SERVER_ERROR, "Filename conflicts, Failed to save file");
        } catch (IOException e) {
            Logger.error("Cannot write data in file: ", fileName, e.getMessage());
            return ResponseData.fromString(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save file");
        }

        Logger.info("Saved the file to disk: ", fileName);
        return ResponseData.ok("Saved the file SUCCESSFULLY!");
    }

    private void byteDebug(byte[] bytes) {
        System.out.print("{");
        for (byte b : bytes) {
            if (b == CR) System.out.print("CR ");
            else if (b == LF) System.out.print("LF ");
            else System.out.printf("%d ", b);
        }
        System.out.print("}");
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
