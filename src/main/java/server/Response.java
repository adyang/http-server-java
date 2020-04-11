package server;

import java.util.Collections;
import java.util.Map;

public class Response {
    public final int statusCode;
    public final String body;
    public final Map<String, String> headers;

    public Response(int statusCode, String body) {
        this(statusCode, Collections.emptyMap(), body);
    }

    public Response(int statusCode, Map<String, String> headers, String body) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
    }
}
