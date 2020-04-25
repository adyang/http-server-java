package server.handlers;

import server.Handler;
import server.data.Method;
import server.data.Request;
import server.data.Response;
import server.data.Status;

import java.util.Map;

public class Dispatcher implements Handler {
    private final Map<Method, Handler> routes;

    public Dispatcher(Map<Method, Handler> routes) {
        this.routes = routes;
    }

    @Override
    public Response handle(Request request) {
        if (routes.containsKey(request.method)) {
            Handler handler = routes.get(request.method);
            return handler.handle(request);
        } else {
            return new Response(Status.INTERNAL_SERVER_ERROR, "");
        }
    }
}
