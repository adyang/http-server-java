package server;

import java.io.BufferedReader;
import java.io.IOException;

public class RequestParser {
    static Request parse(BufferedReader in) throws IOException {
        Request request = parseRequestLine(in.readLine());
        int contentLength = parseContentLength(in);
        request.body = parseBody(in, contentLength);
        return request;
    }

    private static Request parseRequestLine(String line) {
        String[] tokens = line.split(" ");
        return new Request(parseMethod(tokens[0]), tokens[1]);

    }

    private static Method parseMethod(String methodToken) {
        try {
            return Method.valueOf(methodToken);
        } catch (IllegalArgumentException e) {
            throw new InvalidMethodException("Invalid method: " + methodToken);
        }
    }

    private static int parseContentLength(BufferedReader in) throws IOException {
        int contentLength = 0;
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            String[] header = line.split(":", 2);
            if (header.length != 2) throw new ParseException("Invalid header: " + line);
            if ("Content-Length".equals(header[0]))
                contentLength = parseContentLengthInt(header[1]);
        }
        if (line == null) throw new ParseException("Malformed request: missing blank line after header(s)");
        return contentLength;
    }

    private static int parseContentLengthInt(String contentLength) {
        try {
            return Integer.parseInt(contentLength.trim());
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid Content-Length: " + contentLength.trim());
        }
    }

    private static String parseBody(BufferedReader in, int contentLength) throws IOException {
        if (contentLength == 0) return "";
        char[] buffer = new char[contentLength];
        int lengthRead = in.read(buffer, 0, contentLength);
        if (lengthRead != contentLength)
            throw new ParseException("Content-Length mismatched: body is not " + contentLength + " byte(s)");
        return String.valueOf(buffer);
    }

    public static class ParseException extends RuntimeException {
        public ParseException(String message) {
            super(message);
        }
    }

    public static class InvalidMethodException extends RuntimeException {
        public InvalidMethodException(String message) {
            super(message);
        }
    }
}
