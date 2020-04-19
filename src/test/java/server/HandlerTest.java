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
        Request request = new Request(Method.GET, "/does-not-exists");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(404);
        assertThat(response.body).isEqualTo("");
    }

    @Test
    void get_existingResource() throws IOException {
        Request request = new Request(Method.GET, "/existing-file");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.headers).containsOnly(entry("Content-Length", 12L));
        assertThat(response.body).isEqualTo("Hello World!".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void get_jpegImage() throws IOException {
        Files.createFile(directory.resolve("image.jpeg"));
        Request request = new Request(Method.GET, "/image.jpeg");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.headers).containsEntry("Content-Type", "image/jpeg");
    }

    @Test
    void get_pngImage() throws IOException {
        Files.createFile(directory.resolve("image.png"));
        Request request = new Request(Method.GET, "/image.png");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.headers).containsEntry("Content-Type", "image/png");
    }

    @Test
    void get_gifImage() throws IOException {
        Files.createFile(directory.resolve("image.gif"));
        Request request = new Request(Method.GET, "/image.gif");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.headers).containsEntry("Content-Type", "image/gif");
    }

    @Test
    void get_textFile() throws IOException {
        Files.createFile(directory.resolve("file.txt"));
        Request request = new Request(Method.GET, "/file.txt");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.headers).containsEntry("Content-Type", "text/plain");
    }

    @Test
    void get_rootDirectory() throws IOException {
        Request request = new Request(Method.GET, "/");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.headers).containsKeys("Content-Length");
        assertThat(response.headers).containsEntry("Content-Type", "text/html");
        assertThat((String) response.body)
                .contains("<title>Directory: /</title>")
                .contains("<h1>Directory: /</h1>")
                .contains(
                        "<li><a href=\"/existing-file\">existing-file</a></li>",
                        "<li><a href=\"/another-file\">another-file</a></li>",
                        "<li><a href=\"/.hidden-file\">.hidden-file</a></li>",
                        "<li><a href=\"/directory\">directory</a></li>");
    }

    @Test
    void get_nonRootDirectory() throws IOException {
        Request request = new Request(Method.GET, "/directory");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat((String) response.body)
                .contains("<title>Directory: /directory</title>")
                .contains("<h1>Directory: /directory</h1>")
                .contains("<li><a href=\"/directory/inner-file\">inner-file</a></li>");
    }

    @Test
    void head_absentResource() throws IOException {
        Request request = new Request(Method.HEAD, "/does-not-exists");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(404);
        assertThat(response.body).isEqualTo("");
    }

    @Test
    void head_existingResource() throws IOException {
        Request request = new Request(Method.HEAD, "/existing-file");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.headers).containsOnly(entry("Content-Length", 12L));
        assertThat(response.body).isEqualTo("");
    }

    @Test
    void options_anyResource() throws IOException {
        Request request = new Request(Method.OPTIONS, "/any-path");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.headers)
                .containsOnly(entry("Allow", "GET, HEAD, OPTIONS, PUT, DELETE"));
        assertThat(response.body).isEqualTo("");
    }

    @Test
    void options_logsResource() throws IOException {
        Request request = new Request(Method.OPTIONS, "/logs");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.headers)
                .containsOnly(entry("Allow", "GET, HEAD, OPTIONS"));
        assertThat(response.body).isEqualTo("");
    }

    @Test
    void put_absentResource() throws IOException {
        Request request = new Request(Method.PUT, "/new-file", "lineOne\nlineTwo");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(201);
        assertThat(Files.readAllLines(directory.resolve("new-file")))
                .containsExactly("lineOne", "lineTwo");
    }

    @Test
    void put_existingResource() throws IOException {
        Request request = new Request(Method.PUT, "/existing-file", "New Hello World!");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(Files.readAllLines(directory.resolve("existing-file")))
                .containsExactly("New Hello World!");
    }

    @Test
    void put_emptyResource() throws IOException {
        Request request = new Request(Method.PUT, "/new-file");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(201);
        assertThat(new String(Files.readAllBytes(directory.resolve("new-file")), StandardCharsets.UTF_8))
                .isEmpty();
    }

    @Test
    void put_existingDirectory() throws IOException {
        Request request = new Request(Method.PUT, "/directory", "New Hello World!");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(409);
        assertThat(response.body).isEqualTo("Unable to create/update: directory is a directory.");
    }

    @Test
    void delete_existingResource() throws IOException {
        Request request = new Request(Method.DELETE, "/existing-file");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.body).isEqualTo("");
        assertThat(Files.exists(directory.resolve("existing-file"))).isFalse();
    }

    @Test
    void delete_absentResource() throws IOException {
        Request request = new Request(Method.DELETE, "/missing-file");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(404);
    }

    @Test
    void delete_existingDirectory() throws IOException {
        Request request = new Request(Method.DELETE, "/directory");

        Response response = Handler.handle(request, directory);

        assertThat(response.statusCode).isEqualTo(409);
        assertThat(response.body).isEqualTo("Unable to delete: directory is a directory.");
    }
}