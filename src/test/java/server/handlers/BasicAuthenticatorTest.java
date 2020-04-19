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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

public class BasicAuthenticatorTest {
    private static final List<String> PROTECTED_PATHS = singletonList("/protected");
    public static final Map<String, String> CREDENTIALS = singletonMap("admin", "password");
    private TestHandler handler;
    private Handler authenticator;

    @BeforeEach
    void setUp() {
        handler = new TestHandler();
        authenticator = new BasicAuthenticator(handler, "default", PROTECTED_PATHS, CREDENTIALS);
    }

    @Test
    void protectedPath_noAuthorizationHeader() throws IOException {
        Request request = new Request(Method.GET, "/protected");

        Response response = authenticator.handle(request);

        assertThat(response.status).isEqualTo(Status.UNAUTHORIZED);
        assertThat(response.headers).containsEntry("WWW-Authenticate", "Basic realm=\"default\"");
    }

    @Test
    void protectedPath_authorizationHeader_withValidCredentials() throws IOException {
        String basicCookie = Base64.getEncoder().encodeToString("admin:password".getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = singletonMap("Authorization", "Basic " + basicCookie);
        Request request = new Request(Method.GET, "/protected", headers, "");

        Response response = authenticator.handle(request);

        assertThat(response).isEqualTo(handler.handledResponse);
        assertThat(handler.receivedRequest.user).isEqualTo("admin");
    }

    @Test
    void protectedPath_authorizationHeader_withInvalidCredentials() throws IOException {
        String basicCookie = Base64.getEncoder().encodeToString("admin:wrong".getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = singletonMap("Authorization", "Basic " + basicCookie);
        Request request = new Request(Method.GET, "/protected", headers, "");

        Response response = authenticator.handle(request);

        assertThat(response.status).isEqualTo(Status.UNAUTHORIZED);
        assertThat(response.headers).containsEntry("WWW-Authenticate", "Basic realm=\"default\"");
        assertThat(response.body).isEqualTo("Invalid credentials" + System.lineSeparator());
    }

    @Test
    void protectedPath_malformedAuthorizationHeader() throws IOException {
        Map<String, String> headers = singletonMap("Authorization", "BasicNoSpaceOnlyOneToken");
        Request request = new Request(Method.GET, "/protected", headers, "");

        Response response = authenticator.handle(request);

        assertThat(response.status).isEqualTo(Status.BAD_REQUEST);
        assertThat(response.body).isEqualTo("Malformed Authorization header: BasicNoSpaceOnlyOneToken" + System.lineSeparator());
    }

    @Test
    void protectedPath_malformedCredentials() throws IOException {
        String basicCookie = Base64.getEncoder().encodeToString("userNoColon".getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = singletonMap("Authorization", "Basic " + basicCookie);
        Request request = new Request(Method.GET, "/protected", headers, "");

        Response response = authenticator.handle(request);

        assertThat(response.status).isEqualTo(Status.BAD_REQUEST);
        assertThat(response.body).isEqualTo("Malformed Authorization header: credentials format invalid" + System.lineSeparator());
    }

    @Test
    void unprotectedPath() throws IOException {
        Request request = new Request(Method.GET, "/unprotected", "");

        Response response = authenticator.handle(request);

        assertThat(response).isEqualTo(handler.handledResponse);
        assertThat(handler.receivedRequest.user).isEqualTo("anonymous");
    }
}
