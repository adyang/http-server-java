package server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.data.Method;
import server.data.Request;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.HashMap;
import java.util.Map;

public class RequestParser {
    private static final Logger logger = LoggerFactory.getLogger(RequestParser.class);

    static Request parse(InputStream in) throws IOException {
        Request request = parseRequestLine(LineReader.readLine(in));
        request.headers = parseHeaders(in);
        request.body = Channels.newChannel(in);
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

    private static Map<String, String> parseHeaders(InputStream in) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String line;
        while ((line = LineReader.readLine(in)) != null && !line.isEmpty()) {
            String[] header = line.split(":", 2);
            if (header.length != 2) throw new ParseException("Invalid header: " + line);
            headers.put(header[0], header[1].trim());
        }
        if (line == null) throw new ParseException("Malformed request: missing blank line after header(s)");
        return headers;
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
