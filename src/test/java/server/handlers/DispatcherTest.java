package server.handlers;

import org.junit.jupiter.api.Test;
import server.Handler;
import server.data.Method;
import server.data.PatternHandler;
import server.data.Request;
import server.data.Response;
import server.data.Status;
import server.util.Maps;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class DispatcherTest {
    public static final Map<Method, List<PatternHandler>> ROUTES = Maps.of(
            Method.GET, asList(
                    new PatternHandler("/pathOne", echoMethodBody(Method.GET, "/pathOne")),
                    new PatternHandler("*", echoMethodBody(Method.GET, "*"))),
            Method.PUT, asList(
                    new PatternHandler("*", echoMethodBody(Method.PUT, "*")),
                    new PatternHandler("/pathUnseen", echoMethodBody(Method.PUT, "pathUnseen")))
    );

    @Test
    void patternMatchesUriPath() {
        Dispatcher dispatcher = new Dispatcher(ROUTES);

        Response response = dispatcher.handle(new Request(Method.GET, "/pathOne"));

        assertThat(response.body).isEqualTo("GET /pathOne");
    }

    @Test
    void patternMatchesWildcard() {
        Dispatcher dispatcher = new Dispatcher(ROUTES);

        Response response = dispatcher.handle(new Request(Method.GET, "/path"));

        assertThat(response.body).isEqualTo("GET *");
    }

    @Test
    void patternMatchesInOrder() {
        Dispatcher dispatcher = new Dispatcher(ROUTES);

        Response response = dispatcher.handle(new Request(Method.PUT, "/anyPath"));

        assertThat(response.body).isEqualTo("PUT *");
    }

    @Test
    void noMatch() {
        Dispatcher dispatcher = new Dispatcher(ROUTES);

        Response response = dispatcher.handle(new Request(Method.DELETE, "/path"));

        assertThat(response.status).isEqualTo(Status.INTERNAL_SERVER_ERROR);
    }

    private static Handler echoMethodBody(Method method, String body) {
        return r -> new Response(Status.OK, method + " " + body);
    }
}
