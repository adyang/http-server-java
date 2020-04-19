package server.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.Handler;
import server.data.Method;
import server.data.Request;
import server.data.Response;
import server.data.Status;
import server.util.TestHandler;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

public class AuthoriserTest {
    private static final Map<String, Map<String, List<Method>>> ACCESS_CONTROL_LIST = Stream.of(
            new AbstractMap.SimpleImmutableEntry<>("admin", singletonMap("/protected", asList(Method.GET, Method.PUT))),
            new AbstractMap.SimpleImmutableEntry<String, Map<String, List<Method>>>("anonymous", emptyMap())
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    private static final List<Method> DEFAULT_ACCESS = asList(Method.GET, Method.HEAD, Method.OPTIONS);

    private TestHandler handler;
    private Handler authoriser;

    @BeforeEach
    void setUp() {
        handler = new TestHandler();
        authoriser = new Authoriser(handler, ACCESS_CONTROL_LIST, DEFAULT_ACCESS);
    }

    @Test
    void userInAcl_configuredPath_allowedMethod() throws IOException {
        Request request = new Request(Method.GET, "/protected");
        request.user = "admin";

        Response response = authoriser.handle(request);

        assertThat(response).isEqualTo(handler.handledResponse);
    }

    @Test
    void userInAcl_configuredPath_disallowedMethod() throws IOException {
        Request request = new Request(Method.DELETE, "/protected");
        request.user = "admin";

        Response response = authoriser.handle(request);

        assertThat(response.status).isEqualTo(Status.METHOD_NOT_ALLOWED);
        assertThat(response.headers).containsEntry("Allow", "GET, PUT");
    }

    @Test
    void userInAcl_nonConfiguredPath_allowedMethod() throws IOException {
        Request request = new Request(Method.GET, "/others");
        request.user = "anonymous";

        Response response = authoriser.handle(request);

        assertThat(response).isEqualTo(handler.handledResponse);
    }

    @Test
    void userInAcl_nonConfiguredPath_disallowedMethod() throws IOException {
        Request request = new Request(Method.DELETE, "/others");
        request.user = "anonymous";

        Response response = authoriser.handle(request);

        assertThat(response.status).isEqualTo(Status.METHOD_NOT_ALLOWED);
        assertThat(response.headers).containsEntry("Allow", "GET, HEAD, OPTIONS");
    }

    @Test
    void userNotInAcl() throws IOException {
        Request request = new Request(Method.GET, "/any");
        request.user = "notInAcl";

        Response response = authoriser.handle(request);

        assertThat(response.status).isEqualTo(Status.METHOD_NOT_ALLOWED);
        assertThat(response.headers).containsEntry("Allow", "");
    }
}
