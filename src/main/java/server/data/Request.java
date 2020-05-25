package server.data;

import java.nio.channels.ReadableByteChannel;
import java.util.Collections;
import java.util.Map;

public class Request {
    public final Method method;
    public final String path;
    public final String query;
    public Map<String, String> headers;
    public ReadableByteChannel body;
    public String user;
    public Map<String, String> parameters;

    public Request(Method method, String path) {
        this(method, path, null);
    }

    public Request(Method method, String path, String query) {
        this(method, path, query, Collections.emptyMap());
    }

    public Request(Method method, String path, String query, Map<String, String> headers) {
        this.method = method;
        this.path = path;
        this.query = query;
        this.headers = headers;
    }
}
