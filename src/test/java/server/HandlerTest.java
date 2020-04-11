package server;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class HandlerTest {
    @TempDir
    static Path directory;

    @BeforeAll
    static void beforeAll() throws IOException {
        Files.write(directory.resolve("existing-file"), "Hello World!".getBytes(StandardCharsets.UTF_8));
        Files.createFile(directory.resolve("another-file"));
        Files.createFile(directory.resolve(".hidden-file"));
        Files.createDirectory(directory.resolve("directory"));
    }

    @Test
    void get_absentResource() throws IOException {
        Request request = new Request("GET", "/does-not-exists");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(404);
        assertThat(response.body).isNull();
    }

    @Test
    void get_existingResource() throws IOException {
        Request request = new Request("GET", "/existing-file");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.body).isEqualTo("Hello World!");
    }

    @Test
    void get_directory() throws IOException {
        Request request = new Request("GET", "/");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.body.split(System.lineSeparator()))
                .containsExactlyInAnyOrder("existing-file", "another-file", ".hidden-file", "directory");
    }

    @Test
    void head_absentResource() throws IOException {
        Request request = new Request("HEAD", "/does-not-exists");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(404);
        assertThat(response.body).isNull();
    }

    @Test
    void head_existingResource() throws IOException {
        Request request = new Request("HEAD", "/existing-file");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.body).isNull();
    }
}