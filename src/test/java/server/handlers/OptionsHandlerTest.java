package server.handlers;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.Handler;
import server.data.Header;
import server.data.Method;
import server.data.Request;
import server.data.Response;
import server.data.Status;
import server.util.TestHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

class OptionsHandlerTest {
    private static final Map<String, List<Method>> ALLOWED_METHODS = singletonMap("/onlyGet", singletonList(Method.GET));
    private static final List<Method> DEFAULT_METHODS = asList(Method.GET, Method.HEAD, Method.OPTIONS);

    private TestHandler testHandler;
    private Handler handler;

    @BeforeEach
    void setUp() {
        testHandler = new TestHandler();
        handler = new OptionsHandler(testHandler, ALLOWED_METHODS, DEFAULT_METHODS);
    }

    @Test
    void options_configuredPath() throws IOException {
        Request request = new Request(Method.OPTIONS, "/onlyGet");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.headers).containsEntry(Header.ALLOW, "GET");
        assertThat(response.body).isEqualTo("");
    }

    @Test
    void options_nonConfiguredPath() throws IOException {
        Request request = new Request(Method.OPTIONS, "/others");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.headers).containsEntry(Header.ALLOW, "GET, HEAD, OPTIONS");
        assertThat(response.body).isEqualTo("");
    }

    @Test
    void otherMethods() throws IOException {
        Request request = new Request(Method.GET, "/others");

        Response response = handler.handle(request);

        assertThat(response).isEqualTo(testHandler.handledResponse);
    }
}