package server.handlers;

import org.junit.jupiter.api.Test;
import server.Handler;
import server.data.Method;
import server.data.Request;
import server.data.Response;
import server.data.Status;
import server.util.Maps;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DispatcherTest {
    public static final Map<Method, Handler> ROUTES = Maps.of(
            Method.GET, echoMethod(Method.GET),
            Method.PUT, echoMethod(Method.PUT)
    );

    @Test
    void methodPresentInRoutes() {
        Dispatcher dispatcher = new Dispatcher(ROUTES);

        Response response = dispatcher.handle(new Request(Method.GET, "/path"));

        assertThat(response.body).isEqualTo(Method.GET);
    }

    @Test
    void methodAbsentInRoutes() {
        Dispatcher dispatcher = new Dispatcher(ROUTES);

        Response response = dispatcher.handle(new Request(Method.DELETE, "/path"));

        assertThat(response.status).isEqualTo(Status.INTERNAL_SERVER_ERROR);
    }

    private static Handler echoMethod(Method method) {
        return r -> new Response(Status.OK, method);
    }
}
