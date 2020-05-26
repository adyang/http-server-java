package server.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.data.Header;
import server.data.Method;
import server.data.Request;
import server.data.Response;
import server.data.Status;
import server.util.Maps;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class CatFormHandlerTest {
    private AtomicReference<String> cat;
    private CatFormHandler handler;

    @BeforeEach
    void setUp() {
        cat = new AtomicReference<>();
        handler = new CatFormHandler(cat);
    }

    @Test
    void get_catAbsent() {
        Request request = new Request(Method.GET, "/cat-form/data");

        Response response = handler.get(request);

        assertThat(response.status).isEqualTo(Status.NOT_FOUND);
    }

    @Test
    void get_catPresent() {
        cat.set("hicat");
        Request request = new Request(Method.GET, "/cat-form/data");

        Response response = handler.get(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.body).isEqualTo("data=hicat");
    }

    @Test
    void post_dataPresent() {
        Request request = new Request(Method.POST, "/cat-form");
        request.parameters = Maps.of("data", "testcat");

        Response response = handler.post(request);

        assertThat(response.status).isEqualTo(Status.CREATED);
        assertThat(response.headers).containsEntry(Header.LOCATION, "/cat-form/data");
        assertThat(cat.get()).isEqualTo("testcat");
    }

    @Test
    void post_dataAbsent() {
        Request request = new Request(Method.POST, "/cat-form");
        request.parameters = Maps.of();

        Response response = handler.post(request);

        assertThat(response.status).isEqualTo(Status.BAD_REQUEST);
    }

    @Test
    void post_catPresent() {
        cat.set("herecat");
        Request request = new Request(Method.POST, "/cat-form");
        request.parameters = Maps.of("data", "testcat");

        Response response = handler.post(request);

        assertThat(response.status).isEqualTo(Status.CONFLICT);
        assertThat(cat.get()).isEqualTo("herecat");
    }

    @Test
    void put_dataPresent() {
        Request request = new Request(Method.PUT, "/cat-form/data");
        request.parameters = Maps.of("data", "testcat");

        Response response = handler.put(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(cat.get()).isEqualTo("testcat");
    }

    @Test
    void put_dataAbsent() {
        Request request = new Request(Method.PUT, "/cat-form/data");
        request.parameters = Maps.of();

        Response response = handler.put(request);

        assertThat(response.status).isEqualTo(Status.BAD_REQUEST);
    }

    @Test
    void delete_catPresent() {
        cat.set("herecat");
        Request request = new Request(Method.DELETE, "/cat-form/data");

        Response response = handler.delete(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(cat.get()).isNull();
    }

    @Test
    void delete_catAbsent() {
        Request request = new Request(Method.DELETE, "/cat-form/data");

        Response response = handler.delete(request);

        assertThat(response.status).isEqualTo(Status.NOT_FOUND);
    }
}
