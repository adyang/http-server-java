package server.handlers;

import org.junit.jupiter.api.Test;
import server.Handler;
import server.data.Method;
import server.data.Request;
import server.data.Response;
import server.data.Status;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class DispatcherTest {
    public static final Map<Method, Handler> ROUTES = Stream.of(
            new AbstractMap.SimpleImmutableEntry<>(Method.GET, echoMethod(Method.GET)),
            new AbstractMap.SimpleImmutableEntry<>(Method.PUT, echoMethod(Method.PUT))
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    @Test
    void methodPresentInRoutes() throws IOException {
        Dispatcher dispatcher = new Dispatcher(ROUTES);

        Response response = dispatcher.handle(new Request(Method.GET, "/path"));

        assertThat(response.body).isEqualTo(Method.GET);
    }

    @Test
    void methodAbsentInRoutes() throws IOException {
        Dispatcher dispatcher = new Dispatcher(ROUTES);

        Response response = dispatcher.handle(new Request(Method.DELETE, "/path"));

        assertThat(response.status).isEqualTo(Status.INTERNAL_SERVER_ERROR);
    }

    private static Handler echoMethod(Method method) {
        return r -> new Response(Status.OK, method);
    }
}
