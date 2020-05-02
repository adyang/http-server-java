package server.handlers;

import server.Handler;
import server.data.Header;
import server.data.Request;
import server.data.Response;

import java.util.HashMap;

public class DefaultResponseHeaderWrapper implements Handler {
    private final Handler handler;

    public DefaultResponseHeaderWrapper(Handler handler) {
        this.handler = handler;
    }

    @Override
    public Response handle(Request request) {
        Response response = handler.handle(request);
        HashMap<String, Object> headers = new HashMap<>(response.headers);
        headers.put(Header.CONNECTION, "close");
        return new Response(response.status, headers, response.body);
    }
}
