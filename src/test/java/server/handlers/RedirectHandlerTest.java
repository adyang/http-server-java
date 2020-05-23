package server.handlers;

import org.junit.jupiter.api.Test;
import server.data.Header;
import server.data.Method;
import server.data.Request;
import server.data.Response;
import server.data.Status;

import static org.assertj.core.api.Assertions.assertThat;

public class RedirectHandlerTest {
    @Test
    void redirectToUri() {
        Request request = new Request(Method.GET, "/any");

        Response response = new RedirectHandler("/redirectPath").handle(request);

        assertThat(response.status).isEqualTo(Status.FOUND);
        assertThat(response.headers).containsEntry(Header.LOCATION, "/redirectPath");
    }
}
