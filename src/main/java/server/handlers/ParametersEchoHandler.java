package server.handlers;

import server.data.Request;
import server.data.Response;
import server.data.Status;
import server.util.Maps;

import java.util.Map;
import java.util.stream.Collectors;

public class ParametersEchoHandler {
    public static Response handle(Request request) {
        Map<String, String> parameters = request.parameters == null ? Maps.of() : request.parameters;
        String body = parameters.entrySet().stream()
                .map(p -> p.getKey() + " = " + p.getValue())
                .collect(Collectors.joining(System.lineSeparator()));
        return new Response(Status.OK, body);
    }
}
