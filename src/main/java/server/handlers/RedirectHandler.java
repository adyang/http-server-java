package server.handlers;

import server.Handler;
import server.data.Header;
import server.data.Request;
import server.data.Response;
import server.data.Status;
import server.util.Maps;

public class RedirectHandler implements Handler {
    private final String redirectPath;

    public RedirectHandler(String redirectPath) {
        this.redirectPath = redirectPath;
    }

    public Response handle(Request ignore) {
        return new Response(Status.FOUND, Maps.of(Header.LOCATION, redirectPath), "");
    }
}
