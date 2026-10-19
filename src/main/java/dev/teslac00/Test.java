package dev.teslac00;

public class Test {

    static void main() {
        String uri = "/media/";
        String uriWithFilename = "/media/Blank.txt";
        String filename = ParserHelper.parseFilenameURI(uri);
        System.out.printf("[%s]\n", filename);
        filename = ParserHelper.parseFilenameURI(uriWithFilename);
        System.out.printf("[%s]\n", filename);
    }

}
