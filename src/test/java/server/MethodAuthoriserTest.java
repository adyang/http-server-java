package server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class MethodAuthoriserTest {
    private static final Map<String, List<Method>> ALLOWED_METHODS = Stream.of(
            new AbstractMap.SimpleImmutableEntry<>("/onlyGet", singletonList(Method.GET)),
            new AbstractMap.SimpleImmutableEntry<>("/onlyGetAndPut", asList(Method.GET, Method.PUT))
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    private static final List<Method> DEFAULT_METHODS = asList(Method.GET, Method.HEAD, Method.OPTIONS);

    private TestHandler handler;
    private Handler authoriser;

    @BeforeEach
    void setUp() {
        handler = new TestHandler();
        authoriser = new MethodAuthoriser(handler, ALLOWED_METHODS, DEFAULT_METHODS);
    }

    @Test
    void configuredPath_allowedMethod() throws IOException {
        Request request = new Request(Method.GET, "/onlyGet");

        Response response = authoriser.handle(request);

        assertThat(response).isEqualTo(handler.handledResponse);
    }

    @Test
    void configuredPath_disallowedMethod_allowSingle() throws IOException {
        Request request = new Request(Method.PUT, "/onlyGet");

        Response response = authoriser.handle(request);

        assertThat(response.status).isEqualTo(Status.METHOD_NOT_ALLOWED);
        assertThat(response.headers).containsEntry("Allow", "GET");
    }

    @Test
    void configuredPath_disallowedMethod_allowMultiple() throws IOException {
        Request request = new Request(Method.DELETE, "/onlyGetAndPut");

        Response response = authoriser.handle(request);

        assertThat(response.status).isEqualTo(Status.METHOD_NOT_ALLOWED);
        assertThat(response.headers).containsEntry("Allow", "GET, PUT");
    }

    @Test
    void nonConfiguredPath_allowedMethod() throws IOException {
        Request request = new Request(Method.GET, "/others");

        Response response = authoriser.handle(request);

        assertThat(response).isEqualTo(handler.handledResponse);
    }

    @Test
    void nonConfiguredPath_disallowedMethod() throws IOException {
        Request request = new Request(Method.DELETE, "/others");

        Response response = authoriser.handle(request);

        assertThat(response.status).isEqualTo(Status.METHOD_NOT_ALLOWED);
        assertThat(response.headers).containsEntry("Allow", "GET, HEAD, OPTIONS");
    }

    @Test
    void configuredPath_optionsMethod() throws IOException {
        Request request = new Request(Method.OPTIONS, "/onlyGetAndPut");

        Response response = authoriser.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.headers).containsEntry("Allow", "GET, PUT");
        assertThat(response.body).isEqualTo("");
    }

    @Test
    void nonConfiguredPath_optionsMethod() throws IOException {
        Request request = new Request(Method.OPTIONS, "/others");

        Response response = authoriser.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.headers).containsEntry("Allow", "GET, HEAD, OPTIONS");
        assertThat(response.body).isEqualTo("");
    }

    private static class TestHandler implements Handler {
        Response handledResponse = new Response(Status.OK, "");

        @Override
        public Response handle(Request request) {
            return handledResponse;
        }
    }
}
