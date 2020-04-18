package server;

import java.util.Collections;
import java.util.Map;

public class Response {
    public final int statusCode;
    public final Object body;
    public final Map<String, Object> headers;

    public Response(int statusCode, Object body) {
        this(statusCode, Collections.emptyMap(), body);
    }

    public Response(int statusCode, Map<String, Object> headers, Object body) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
    }
}
