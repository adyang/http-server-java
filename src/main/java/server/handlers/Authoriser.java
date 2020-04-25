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

public class Authoriser implements Handler {
    private final Handler handler;
    private final Map<String, Map<String, List<Method>>> accessControlList;
    private final List<Method> defaultAccess;

    public Authoriser(Handler handler, Map<String, Map<String, List<Method>>> accessControlList, List<Method> defaultAccess) {
        this.handler = handler;
        this.accessControlList = accessControlList;
        this.defaultAccess = defaultAccess;
    }

    @Override
    public Response handle(Request request) throws IOException {
        if (accessControlList.containsKey(request.user))
            return authorise(request);
        else
            return new Response(Status.METHOD_NOT_ALLOWED,
                    Collections.singletonMap(Header.ALLOW, ""),
                    "");
    }

    private Response authorise(Request request) throws IOException {
        Map<String, List<Method>> accessControl = accessControlList.get(request.user);
        List<Method> allowedMethods = accessControl.getOrDefault(request.uri, defaultAccess);
        if (allowedMethods.contains(request.method))
            return handler.handle(request);
        else {
            return new Response(Status.METHOD_NOT_ALLOWED,
                    Collections.singletonMap(Header.ALLOW, commaDelimited(allowedMethods)),
                    "");
        }
    }

    private String commaDelimited(List<Method> methods) {
        return methods.stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
