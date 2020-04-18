package server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class HandlerTest {
    @TempDir
    Path directory;

    @BeforeEach
    void setUp() throws IOException {
        Files.write(directory.resolve("existing-file"), "Hello World!".getBytes(StandardCharsets.UTF_8));
        Files.createFile(directory.resolve("another-file"));
        Files.createFile(directory.resolve(".hidden-file"));
        Files.createDirectory(directory.resolve("directory"));
        Files.createFile(directory.resolve("directory").resolve("inner-file"));
    }

    @Test
    void get_absentResource() throws IOException {
        Request request = new Request("GET", "/does-not-exists");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(404);
        assertThat(response.body).isEmpty();
    }

    @Test
    void get_existingResource() throws IOException {
        Request request = new Request("GET", "/existing-file");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.body).isEqualTo("Hello World!");
    }

    @Test
    void get_rootDirectory() throws IOException {
        Request request = new Request("GET", "/");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.body).contains("<title>Directory: /</title>");
        assertThat(response.body).contains("<h1>Directory: /</h1>");
        assertThat(response.body).contains(
                "<li><a href=\"/existing-file\">existing-file</a></li>",
                "<li><a href=\"/another-file\">another-file</a></li>",
                "<li><a href=\"/.hidden-file\">.hidden-file</a></li>",
                "<li><a href=\"/directory\">directory</a></li>");
    }

    @Test
    void get_nonRootDirectory() throws IOException {
        Request request = new Request("GET", "/directory");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.body).contains("<title>Directory: /directory</title>");
        assertThat(response.body).contains("<h1>Directory: /directory</h1>");
        assertThat(response.body).contains("<li><a href=\"/directory/inner-file\">inner-file</a></li>");
    }

    @Test
    void head_absentResource() throws IOException {
        Request request = new Request("HEAD", "/does-not-exists");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(404);
        assertThat(response.body).isEmpty();
    }

    @Test
    void head_existingResource() throws IOException {
        Request request = new Request("HEAD", "/existing-file");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.body).isEmpty();
    }

    @Test
    void options_anyResource() throws IOException {
        Request request = new Request("OPTIONS", "/any-path");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.headers)
                .containsOnly(entry("Allow", "GET, HEAD, OPTIONS, PUT, DELETE"));
    }

    @Test
    void options_logsResource() throws IOException {
        Request request = new Request("OPTIONS", "/logs");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.headers)
                .containsOnly(entry("Allow", "GET, HEAD, OPTIONS"));
    }

    @Test
    void put_absentResource() throws IOException {
        Request request = new Request("PUT", "/new-file", "lineOne\nlineTwo");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(201);
        assertThat(Files.readAllLines(directory.resolve("new-file")))
                .containsExactly("lineOne", "lineTwo");
    }

    @Test
    void put_existingResource() throws IOException {
        Request request = new Request("PUT", "/existing-file", "New Hello World!");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(Files.readAllLines(directory.resolve("existing-file")))
                .containsExactly("New Hello World!");
    }

    @Test
    void put_emptyResource() throws IOException {
        Request request = new Request("PUT", "/new-file");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(201);
        assertThat(new String(Files.readAllBytes(directory.resolve("new-file")), StandardCharsets.UTF_8))
                .isEmpty();
    }

    @Test
    void put_existingDirectory() throws IOException {
        Request request = new Request("PUT", "/directory", "New Hello World!");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(409);
        assertThat(response.body).isEqualTo("Unable to create/update: directory is a directory.");
    }

    @Test
    void delete_existingResource() throws IOException {
        Request request = new Request("DELETE", "/existing-file");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(Files.exists(directory.resolve("existing-file"))).isFalse();
    }

    @Test
    void delete_absentResource() throws IOException {
        Request request = new Request("DELETE", "/missing-file");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(404);
    }

    @Test
    void delete_existingDirectory() throws IOException {
        Request request = new Request("DELETE", "/directory");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(409);
        assertThat(response.body).isEqualTo("Unable to delete: directory is a directory.");
    }
}