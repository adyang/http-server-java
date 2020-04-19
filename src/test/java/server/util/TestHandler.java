package server.util;

import server.Handler;
import server.data.Request;
import server.data.Response;
import server.data.Status;

public class TestHandler implements Handler {
    public Response handledResponse = new Response(Status.OK, "");
    public Request receivedRequest;

    @Override
    public Response handle(Request request) {
        receivedRequest = request;
        return handledResponse;
    }
}
