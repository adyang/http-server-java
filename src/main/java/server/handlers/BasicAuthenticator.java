package server.handlers;

import server.Handler;
import server.data.Request;
import server.data.Response;
import server.data.Status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BasicAuthenticator implements Handler {
    private final Handler handler;
    private final String challenge;
    private final List<String> protectedPaths;
    private final Map<String, String> credentialsStore;

    public BasicAuthenticator(Handler handler, String realm, List<String> protectedPaths, Map<String, String> credentialsStore) {
        this.handler = handler;
        this.challenge = String.format("Basic realm=\"%s\"", realm);
        this.protectedPaths = protectedPaths;
        this.credentialsStore = credentialsStore;
    }

    @Override
    public Response handle(Request request) throws IOException {
        if (!protectedPaths.contains(request.uri)) {
            request.user = "anonymous";
            return handler.handle(request);
        }

        if (request.headers.containsKey("Authorization")) {
            return attemptAuthenticationOf(request);
        } else {
            return new Response(Status.UNAUTHORIZED, Collections.singletonMap("WWW-Authenticate", challenge), "");
        }
    }

    private Response attemptAuthenticationOf(Request request) throws IOException {
        try {
            String[] credentials = parseCredentials(request.headers.get("Authorization"));
            return authenticate(request, credentials[0], credentials[1]);
        } catch (ParseException e) {
            return new Response(Status.BAD_REQUEST, "Malformed Authorization header: " + e.getMessage() + System.lineSeparator());
        }
    }

    private String[] parseCredentials(String authorization) {
        String[] tokens = authorization.split(" ", 2);
        if (tokens.length != 2) throw new ParseException(authorization);
        String decodedCookie = new String(Base64.getDecoder().decode(tokens[1]), StandardCharsets.UTF_8);
        String[] credentials = decodedCookie.split(":", 2);
        if (credentials.length != 2) throw new ParseException("credentials format invalid");
        return credentials;
    }

    private Response authenticate(Request request, String username, String password) throws IOException {
        if (password.equals(credentialsStore.get(username))) {
            request.user = username;
            return handler.handle(request);
        } else {
            return new Response(Status.UNAUTHORIZED,
                    Collections.singletonMap("WWW-Authenticate", challenge),
                    "Invalid credentials" + System.lineSeparator());
        }
    }

    private static class ParseException extends RuntimeException {
        public ParseException(String message) {
            super(message);
        }
    }
}
