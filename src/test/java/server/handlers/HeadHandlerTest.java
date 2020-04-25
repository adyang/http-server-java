package server.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import server.data.Header;
import server.data.Method;
import server.data.Request;
import server.data.Response;
import server.data.Status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class HeadHandlerTest {
    @TempDir
    Path directory;
    private HeadHandler handler;

    @BeforeEach
    void setUp() {
        handler = new HeadHandler(directory);
    }

    @Test
    void head_absentResource() throws IOException {
        Request request = new Request(Method.HEAD, "/does-not-exists");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.NOT_FOUND);
        assertThat(response.body).isEqualTo("");
    }

    @Test
    void head_existingResource() throws IOException {
        Files.write(directory.resolve("existing-file"), "Hello World!".getBytes(StandardCharsets.UTF_8));
        Request request = new Request(Method.HEAD, "/existing-file");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.headers).containsOnly(entry(Header.CONTENT_LENGTH, 12L));
        assertThat(response.body).isEqualTo("");
    }
}
