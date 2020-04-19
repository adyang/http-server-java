package server;

import server.data.Request;
import server.data.Response;

import java.io.IOException;

public interface Handler {
    Response handle(Request request) throws IOException;
}
