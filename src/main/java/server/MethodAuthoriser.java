package server;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MethodAuthoriser implements Handler {
    private final Handler handler;
    private final Map<String, List<Method>> allowedMethods;
    private final List<Method> defaultMethods;

    public MethodAuthoriser(Handler handler, Map<String, List<Method>> allowedMethods, List<Method> defaultMethods) {
        this.handler = handler;
        this.allowedMethods = allowedMethods;
        this.defaultMethods = defaultMethods;
    }

    @Override
    public Response handle(Request request) throws IOException {
        List<Method> methods = allowedMethods.getOrDefault(request.uri, defaultMethods);
        if (request.method == Method.OPTIONS)
            return options(methods);
        else
            return authorise(request, methods);
    }

    private Response options(List<Method> methods) {
        return new Response(Status.OK,
                Collections.singletonMap("Allow", commaDelimited(methods)),
                "");
    }

    private Response authorise(Request request, List<Method> methods) throws IOException {
        if (methods.contains(request.method))
            return handler.handle(request);
        else {
            return new Response(Status.METHOD_NOT_ALLOWED,
                    Collections.singletonMap("Allow", commaDelimited(methods)),
                    "");
        }
    }

    private String commaDelimited(List<Method> methods) {
        return methods.stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
