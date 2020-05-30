package server.handlers;

import org.junit.jupiter.api.Test;
import server.data.Header;
import server.data.Method;
import server.data.Request;
import server.data.Response;
import server.data.Status;
import server.util.Maps;

import static org.assertj.core.api.Assertions.assertThat;

public class CookieHandlerTest {
    @Test
    void get_cookie() {
        Request request = new Request(Method.GET, "/cookie");
        request.parameters = Maps.of("type", "food");

        Response response = CookieHandler.cookie(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.headers).containsEntry(Header.SET_COOKIE, "type=food");
        assertThat(response.body).isEqualTo("Eat food");
    }

    @Test
    void get_cookie_typeAbsent() {
        Request request = new Request(Method.GET, "/cookie");

        Response response = CookieHandler.cookie(request);

        assertThat(response.status).isEqualTo(Status.BAD_REQUEST);
        assertThat(response.body).isEqualTo("Missing type parameter.");
    }

    @Test
    void eatCookie() {
        Request request = new Request(Method.GET, "/eat_cookie");
        request.headers = Maps.of(Header.COOKIE, "type=food");

        Response response = CookieHandler.eatCookie(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.body).isEqualTo("mmmm food");
    }

    @Test
    void eatCookie_typeAbsent() {
        Request request = new Request(Method.GET, "/eat_cookie");
        request.headers = Maps.of(Header.COOKIE, "notType=food");

        Response response = CookieHandler.eatCookie(request);

        assertThat(response.status).isEqualTo(Status.BAD_REQUEST);
        assertThat(response.body).isEqualTo("Missing cookie type.");
    }

    @Test
    void eatCookie_cookieAbsent() {
        Request request = new Request(Method.GET, "/eat_cookie");

        Response response = CookieHandler.eatCookie(request);

        assertThat(response.status).isEqualTo(Status.BAD_REQUEST);
        assertThat(response.body).isEqualTo("Missing cookie type.");
    }
}
