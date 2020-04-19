package server;

public class Request {
    public final Method method;
    public final String uri;
    public String body;

    public Request(Method method, String uri) {
        this(method, uri, "");
    }

    public Request(Method method, String uri, String body) {
        this.method = method;
        this.uri = uri;
        this.body = body;
    }
}
