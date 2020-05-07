package server.handlers;

import org.junit.jupiter.api.Test;
import server.data.Method;
import server.data.Request;
import server.data.Response;
import server.data.Status;

import static org.assertj.core.api.Assertions.assertThat;

public class TeapotHandlerTest {
    @Test
    void coffee() {
        Request request = new Request(Method.GET, "/coffee");

        Response response = TeapotHandler.handleCoffee(request);

        assertThat(response.status).isEqualTo(Status.I_AM_A_TEAPOT);
        assertThat(response.body).isEqualTo("I'm a teapot");
    }

    @Test
    void tea() {
        Request request = new Request(Method.GET, "/tea");

        Response response = TeapotHandler.handleTea(request);

        assertThat(response.status).isEqualTo(Status.OK);
    }
}
