package server;

import server.data.Request;
import server.data.Response;

public interface Handler {
    Response handle(Request request);
}
