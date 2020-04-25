package server.handlers;

import server.Handler;
import server.data.Header;
import server.data.Method;
import server.data.Request;
import server.data.Response;
import server.data.Status;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OptionsHandler implements Handler {
    private final Handler handler;
    private final Map<String, List<Method>> allowedMethods;
    private final List<Method> defaultMethods;

    public OptionsHandler(Handler handler, Map<String, List<Method>> allowedMethods, List<Method> defaultMethods) {
        this.handler = handler;
        this.allowedMethods = allowedMethods;
        this.defaultMethods = defaultMethods;
    }

    @Override
    public Response handle(Request request) throws IOException {
        if (request.method != Method.OPTIONS) return handler.handle(request);

        List<Method> methods = allowedMethods.getOrDefault(request.uri, defaultMethods);
        return new Response(Status.OK,
                Collections.singletonMap(Header.ALLOW, commaDelimited(methods)),
                "");
    }

    private String commaDelimited(List<Method> methods) {
        return methods.stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
