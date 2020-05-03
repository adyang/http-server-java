package server.data;

import java.nio.channels.ReadableByteChannel;
import java.util.Collections;
import java.util.Map;

public class Request {
    public final Method method;
    public final String uri;
    public Map<String, String> headers;
    public ReadableByteChannel body;
    public String user;

    public Request(Method method, String uri) {
        this(method, uri, Collections.emptyMap());
    }

    public Request(Method method, String uri, Map<String, String> headers) {
        this.method = method;
        this.uri = uri;
        this.headers = headers;
    }
}
