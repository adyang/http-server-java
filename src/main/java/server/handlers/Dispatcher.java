package server.handlers;

import server.Handler;
import server.data.Method;
import server.data.PatternHandler;
import server.data.Request;
import server.data.Response;
import server.data.Status;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Dispatcher implements Handler {
    private static final String WILDCARD = "*";

    private final Map<Method, List<PatternHandler>> routes;

    public Dispatcher(Map<Method, List<PatternHandler>> routes) {
        this.routes = routes;
    }

    @Override
    public Response handle(Request request) {
        for (PatternHandler ph : routes.getOrDefault(request.method, Collections.emptyList()))
            if (matches(ph.pattern, request))
                return ph.handler.handle(request);
        return new Response(Status.INTERNAL_SERVER_ERROR, "");
    }

    private boolean matches(String pattern, Request request) {
        return pattern.equals(request.path) || pattern.equals(WILDCARD);
    }

}
