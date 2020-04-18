package server;

import java.io.PrintWriter;
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

    static void compose(PrintWriter out, Response response) {
        writeStatusLine(out, response);
        writeHeaders(out, response);
        out.print("\r\n");
        writeBody(out, response);
        out.flush();
    }

    private static void writeStatusLine(PrintWriter out, Response response) {
        out.printf("HTTP/1.1 %d %s\r\n", response.statusCode, REASON_PHRASES.get(response.statusCode));
    }

    private static void writeHeaders(PrintWriter out, Response response) {
        for (Map.Entry<String, String> header : response.headers.entrySet()) {
            out.printf("%s: %s\r\n", header.getKey(), header.getValue());
        }
    }

    private static void writeBody(PrintWriter out, Response response) {
        out.printf("%s", response.body);
    }
}
