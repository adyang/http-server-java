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

public class DeleteHandlerTest {
    @TempDir
    Path directory;
    private DeleteHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DeleteHandler(directory);
    }

    @Test
    void delete_existingResource() throws IOException {
        Files.write(directory.resolve("existing-file"), "Hello World!".getBytes(StandardCharsets.UTF_8));
        Request request = new Request(Method.DELETE, "/existing-file");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.body).isEqualTo("");
        assertThat(Files.exists(directory.resolve("existing-file"))).isFalse();
    }

    @Test
    void delete_absentResource() throws IOException {
        Request request = new Request(Method.DELETE, "/missing-file");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.NOT_FOUND);
    }

    @Test
    void delete_existingDirectory() throws IOException {
        Files.createDirectory(directory.resolve("directory"));
        Request request = new Request(Method.DELETE, "/directory");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.CONFLICT);
        assertThat(response.body).isEqualTo("Unable to delete: directory is a directory.");
    }
}
