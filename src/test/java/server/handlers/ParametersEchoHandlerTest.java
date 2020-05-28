package server.handlers;

import org.junit.jupiter.api.Test;
import server.data.Method;
import server.data.Request;
import server.data.Response;
import server.data.Status;
import server.util.Maps;

import static org.assertj.core.api.Assertions.assertThat;

public class ParametersEchoHandlerTest {
    @Test
    void parametersPresent() {
        Request request = new Request(Method.GET, "/parameters");
        request.parameters = Maps.of("keyOne", "valueOne", "keyTwo", "valueTwo");

        Response response = ParametersEchoHandler.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.body).asString().contains("keyOne = valueOne", "keyTwo = valueTwo");
    }

    @Test
    void parametersAbsent() {
        Request request = new Request(Method.GET, "/parameters");

        Response response = ParametersEchoHandler.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.body).asString().isEmpty();
    }
}
