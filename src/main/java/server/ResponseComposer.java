package server;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResponseComposer {
    private static final Map<Integer, String> REASON_PHRASES = Stream.of(
            new AbstractMap.SimpleImmutableEntry<>(200, "OK"),
            new AbstractMap.SimpleImmutableEntry<>(400, "Bad Request"),
            new AbstractMap.SimpleImmutableEntry<>(404, "Not Found")
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    static void compose(PrintStream out, Response response) {
        writeStatusLine(out, response);
        writeHeaders(out, response);
        out.print("\r\n");
        writeBody(out, response);
        out.flush();
    }

    private static void writeStatusLine(PrintStream out, Response response) {
        out.printf("HTTP/1.1 %d %s\r\n", response.statusCode, REASON_PHRASES.get(response.statusCode));
    }

    private static void writeHeaders(PrintStream out, Response response) {
        for (Map.Entry<String, Object> header : response.headers.entrySet()) {
            out.printf("%s: %s\r\n", header.getKey(), header.getValue());
        }
    }

    private static void writeBody(PrintStream out, Response response) {
        if (response.body instanceof String) {
            writeStringBody(out, response.body);
        } else if (response.body instanceof byte[]) {
            writeByteArrayBody(out, (byte[]) response.body);
        }
    }

    private static void writeStringBody(PrintStream out, Object body) {
        out.printf("%s", body);
    }

    private static void writeByteArrayBody(PrintStream out, byte[] body) {
        try {
            out.write(body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
