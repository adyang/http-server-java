package server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.data.Header;
import server.data.Method;
import server.data.Request;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RequestParser {
    private static final Logger logger = LoggerFactory.getLogger(RequestParser.class);

    static Request parse(BufferedReader in) throws IOException {
        Request request = parseRequestLine(in.readLine());
        request.headers = parseHeaders(in);
        int contentLength = parseContentLength(request.headers);
        request.body = parseBody(in, contentLength);
        return request;
    }

    private static Request parseRequestLine(String line) {
        logger.info("[Request] '{}'", line);
        if (line == null) throw new ParseException("Malformed request: missing request line");
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

    private static Map<String, String> parseHeaders(BufferedReader in) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            String[] header = line.split(":", 2);
            if (header.length != 2) throw new ParseException("Invalid header: " + line);
            headers.put(header[0], header[1].trim());
        }
        if (line == null) throw new ParseException("Malformed request: missing blank line after header(s)");
        return headers;
    }

    private static int parseContentLength(Map<String, String> headers) {
        String contentLength = headers.getOrDefault(Header.CONTENT_LENGTH, "0");
        try {
            return Integer.parseInt(contentLength);
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid Content-Length: " + contentLength);
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
