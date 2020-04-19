package server;

import java.util.Collections;
import java.util.Map;

public class Response {
    public final Status status;
    public final Object body;
    public final Map<String, Object> headers;

    public Response(Status status, Object body) {
        this(status, Collections.emptyMap(), body);
    }

    public Response(Status status, Map<String, Object> headers, Object body) {
        this.status = status;
        this.body = body;
        this.headers = headers;
    }
}
