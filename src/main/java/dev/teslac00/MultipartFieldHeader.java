package dev.teslac00;

public record MultipartFieldHeader(
        String contentDisposition,
        String name,
        String filename,
        String contentType
) {
}
