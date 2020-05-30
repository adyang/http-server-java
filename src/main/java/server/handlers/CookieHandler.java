package server.handlers;

import server.data.Header;
import server.data.Request;
import server.data.Response;
import server.data.Status;
import server.util.Maps;

import java.util.Map;

public class CookieHandler {
    private static final String TYPE_KEY = "type";

    public static Response cookie(Request request) {
        if (request.parameters != null && request.parameters.containsKey(TYPE_KEY)) {
            String type = request.parameters.get(TYPE_KEY);
            return new Response(Status.OK, Maps.of(Header.SET_COOKIE, "type=" + type), "Eat " + type);
        } else {
            return new Response(Status.BAD_REQUEST, "Missing type parameter.");
        }
    }

    public static Response eatCookie(Request request) {
        String type = parseTypeCookie(request.headers);
        if (type == null)
            return new Response(Status.BAD_REQUEST, "Missing cookie type.");
        else
            return new Response(Status.OK, "mmmm " + type);
    }

    private static String parseTypeCookie(Map<String, String> headers) {
        if (!headers.containsKey(Header.COOKIE)) return null;
        String cookie = headers.get(Header.COOKIE);
        String[] keyVal = cookie.split("=", 2);
        if (keyVal[0].equals(TYPE_KEY))
            return keyVal[1];
        else
            return null;
    }
}
