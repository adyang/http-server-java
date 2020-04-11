package server;

import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResponseComposer {
    private static final Map<Integer, String> REASON_PHRASES = Stream.of(
            new AbstractMap.SimpleImmutableEntry<>(200, "OK"),
            new AbstractMap.SimpleImmutableEntry<>(404, "Not Found")
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    static void compose(PrintWriter out, Response response) {
        out.printf("HTTP/1.1 %d %s\r\n", response.statusCode, REASON_PHRASES.get(response.statusCode));
        out.print("\r\n");
        if (response.body != null) out.printf("%s\r\n", response.body);
        out.flush();
    }
}
