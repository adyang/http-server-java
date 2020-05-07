package server.handlers;

import server.data.Request;
import server.data.Response;
import server.data.Status;

public class TeapotHandler {
    public static Response handleCoffee(Request ignore) {
        return new Response(Status.I_AM_A_TEAPOT, "I'm a teapot");
    }

    public static Response handleTea(Request ignore) {
        return new Response(Status.OK, "Have a cup of tea!");
    }
}
