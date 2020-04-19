package server.data;

import java.util.Collections;
import java.util.Map;

public class Request {
    public final Method method;
    public final String uri;
    public Map<String, String> headers;
    public String body;
    public String user;

    public Request(Method method, String uri) {
        this(method, uri, "");
    }

    public Request(Method method, String uri, String body) {
        this(method, uri, Collections.emptyMap(), body);
    }

    public Request(Method method, String uri, Map<String, String> headers, String body) {
        this.method = method;
        this.uri = uri;
        this.headers = headers;
        this.body = body;
    }
}
