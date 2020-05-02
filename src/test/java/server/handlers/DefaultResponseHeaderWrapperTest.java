package server.handlers;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.Handler;
import server.data.Header;
import server.data.Method;
import server.data.Request;
import server.data.Response;
import server.data.Status;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultResponseHeaderWrapperTest {
    private Handler handler;

    @BeforeEach
    void setUp() {
        handler = new DefaultResponseHeaderWrapper(r -> new Response(Status.OK, ""));
    }

    @Test
    void connectionClose() {
        Request request = new Request(Method.GET, "/any");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.headers).containsEntry(Header.CONNECTION, "close");
    }
}