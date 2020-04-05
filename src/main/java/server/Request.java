package server;

public class Request {
    public final String method;
    public final String uri;

    public Request(String method, String uri) {
        this.method = method;
        this.uri = uri;
    }
}
