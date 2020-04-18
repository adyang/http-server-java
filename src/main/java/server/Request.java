package server;

public class Request {
    public final String method;
    public final String uri;
    public String body;

    public Request(String method, String uri) {
        this(method, uri, "");
    }

    public Request(String method, String uri, String body) {
        this.method = method;
        this.uri = uri;
        this.body = body;
    }
}
