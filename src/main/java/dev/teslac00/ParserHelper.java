package dev.teslac00;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.teslac00.MediaHandler.CONTENT_TYPE_TEXT;

public class ParserHelper {

    private static final Pattern FIELD_HEADER_PATTERN = Pattern.compile(
            "Content-Disposition:\\s([^;]*);\\sname=\"([^\"]*)\";\\sfilename=\"([^\"]*)\"(?:\\r\\nContent-Type:\\s(.*))?"
    );

    public static MultipartFieldHeader parseFieldHeader(String rawFieldHeader) {
        // field headers
        // Content-Disposition: form-data; name="image"; filename="rat.jpg"
        // Content-Type: image/jpeg
        // Each field header field separated by CRLF (Content-Disposition-CRLF-Content-Type)
        Matcher fieldHeaderMatcher = FIELD_HEADER_PATTERN.matcher(rawFieldHeader);
        boolean matches = fieldHeaderMatcher.matches();
        if (!matches) return null;

        String fileName = fieldHeaderMatcher.group(3);
        String fieldDataType = fieldHeaderMatcher.group(4);
        if (fieldDataType == null) fieldDataType = CONTENT_TYPE_TEXT;

        Logger.debug("Content-Disposition: [", fieldHeaderMatcher.group(1), "]");
        Logger.debug("name: [", fieldHeaderMatcher.group(2), "]");
        Logger.debug("filename: [", fileName, "]");
        Logger.debug("Content-Type: [", fieldDataType, "]");

        return new MultipartFieldHeader(
                fieldHeaderMatcher.group(1),
                fieldHeaderMatcher.group(2),
                fileName,
                fieldDataType
        );
    }

    public static String parseFilenameURI(String uri) {
        // [/media/Blank.txt]
        Pattern fileNameUriPattern = Pattern.compile("/media(?:/([^/]*))?");
        Matcher matcher = fileNameUriPattern.matcher(uri);
        if (matcher.matches()) return matcher.group(1);
        return null;
    }

}
