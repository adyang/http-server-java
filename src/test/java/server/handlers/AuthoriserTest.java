package server.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.Handler;
import server.data.Header;
import server.data.Method;
import server.data.Request;
import server.data.Response;
import server.data.Status;
import server.util.Maps;
import server.util.TestHandler;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class AuthoriserTest {
    private static final Map<String, Map<String, List<Method>>> ACCESS_CONTROL_LIST = Maps.of(
            "admin", Maps.of("/protected", asList(Method.GET, Method.PUT)),
            "anonymous", Maps.of()
    );
    private static final List<Method> DEFAULT_ACCESS = asList(Method.GET, Method.HEAD, Method.OPTIONS);

    private TestHandler handler;
    private Handler authoriser;

    @BeforeEach
    void setUp() {
        handler = new TestHandler();
        authoriser = new Authoriser(handler, ACCESS_CONTROL_LIST, DEFAULT_ACCESS);
    }

    @Test
    void userInAcl_configuredPath_allowedMethod() {
        Request request = new Request(Method.GET, "/protected");
        request.user = "admin";

        Response response = authoriser.handle(request);

        assertThat(response).isEqualTo(handler.handledResponse);
    }

    @Test
    void userInAcl_configuredPath_disallowedMethod() {
        Request request = new Request(Method.DELETE, "/protected");
        request.user = "admin";

        Response response = authoriser.handle(request);

        assertThat(response.status).isEqualTo(Status.METHOD_NOT_ALLOWED);
        assertThat(response.headers).containsEntry(Header.ALLOW, "GET, PUT");
    }

    @Test
    void userInAcl_nonConfiguredPath_allowedMethod() {
        Request request = new Request(Method.GET, "/others");
        request.user = "anonymous";

        Response response = authoriser.handle(request);

        assertThat(response).isEqualTo(handler.handledResponse);
    }

    @Test
    void userInAcl_nonConfiguredPath_disallowedMethod() {
        Request request = new Request(Method.DELETE, "/others");
        request.user = "anonymous";

        Response response = authoriser.handle(request);

        assertThat(response.status).isEqualTo(Status.METHOD_NOT_ALLOWED);
        assertThat(response.headers).containsEntry(Header.ALLOW, "GET, HEAD, OPTIONS");
    }

    @Test
    void userNotInAcl() {
        Request request = new Request(Method.GET, "/any");
        request.user = "notInAcl";

        Response response = authoriser.handle(request);

        assertThat(response.status).isEqualTo(Status.METHOD_NOT_ALLOWED);
        assertThat(response.headers).containsEntry(Header.ALLOW, "");
    }
}
