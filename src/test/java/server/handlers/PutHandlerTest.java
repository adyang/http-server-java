package server.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import server.data.Method;
import server.data.Request;
import server.data.Response;
import server.data.Status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class PutHandlerTest {
    @TempDir
    Path directory;
    private PutHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PutHandler(directory);
    }

    @Test
    void put_absentResource() throws IOException {
        Request request = new Request(Method.PUT, "/new-file", "lineOne\nlineTwo");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.CREATED);
        assertThat(Files.readAllLines(directory.resolve("new-file")))
                .containsExactly("lineOne", "lineTwo");
    }

    @Test
    void put_existingResource() throws IOException {
        Files.write(directory.resolve("existing-file"), "Hello World!".getBytes(StandardCharsets.UTF_8));
        Request request = new Request(Method.PUT, "/existing-file", "New Hello World!");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(Files.readAllLines(directory.resolve("existing-file")))
                .containsExactly("New Hello World!");
    }

    @Test
    void put_emptyResource() throws IOException {
        Request request = new Request(Method.PUT, "/new-file");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.CREATED);
        assertThat(new String(Files.readAllBytes(directory.resolve("new-file")), StandardCharsets.UTF_8))
                .isEmpty();
    }

    @Test
    void put_existingDirectory() throws IOException {
        Files.createDirectory(directory.resolve("directory"));
        Request request = new Request(Method.PUT, "/directory", "New Hello World!");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.CONFLICT);
        assertThat(response.body).isEqualTo("Unable to create/update: directory is a directory.");
    }
}
