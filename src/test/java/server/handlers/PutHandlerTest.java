package server.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import server.data.Header;
import server.data.Method;
import server.data.Request;
import server.data.Response;
import server.data.Status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

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
        Request request = new Request(Method.PUT, "/new-file");
        String body = "lineOne\nlineTwo";
        request.headers = Collections.singletonMap(Header.CONTENT_LENGTH, String.valueOf(body.length()));
        request.body = readableChannelOf(body);

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.CREATED);
        assertThat(Files.readAllLines(directory.resolve("new-file")))
                .containsExactly("lineOne", "lineTwo");
    }

    @Test
    void put_existingResource() throws IOException {
        Files.write(directory.resolve("existing-file"), "Hello World!".getBytes(StandardCharsets.UTF_8));
        Request request = new Request(Method.PUT, "/existing-file");
        String body = "New Hello World!";
        request.headers = Collections.singletonMap(Header.CONTENT_LENGTH, String.valueOf(body.length()));
        request.body = readableChannelOf(body);

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(Files.readAllLines(directory.resolve("existing-file")))
                .containsExactly("New Hello World!");
    }

    @Test
    void put_emptyResource() throws IOException {
        Request request = new Request(Method.PUT, "/new-file");
        request.headers = Collections.singletonMap(Header.CONTENT_LENGTH, "0");
        request.body = readableChannelOf("");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.CREATED);
        assertThat(new String(Files.readAllBytes(directory.resolve("new-file")), StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void put_existingDirectory() throws IOException {
        Files.createDirectory(directory.resolve("directory"));
        Request request = new Request(Method.PUT, "/directory");
        String body = "New Hello World!";
        request.headers = Collections.singletonMap(Header.CONTENT_LENGTH, String.valueOf(body.length()));
        request.body = readableChannelOf(body);

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.CONFLICT);
        assertThat(response.body).isEqualTo("Unable to create/update: directory is a directory.");
    }

    @Test
    void put_invalidContentLength() {
        Request request = new Request(Method.PUT, "/new-file");
        request.headers = Collections.singletonMap(Header.CONTENT_LENGTH, "invalid");
        request.body = readableChannelOf("");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.BAD_REQUEST);
        assertThat(response.body).isEqualTo("Invalid Content-Length: invalid");
    }

    @Test
    void put_incompleteBody_newFile() {
        Request request = new Request(Method.PUT, "/new-file");
        request.headers = Collections.singletonMap(Header.CONTENT_LENGTH, "5");
        request.body = readableChannelOf("1234");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.BAD_REQUEST);
        assertThat(response.body).isEqualTo("Incomplete message: body is not 5 byte(s)");
        assertThat(Files.notExists(directory.resolve("new-file"))).isTrue();
    }

    @Test
    void put_incompleteBody_existingFile() throws IOException {
        Files.write(directory.resolve("existing-file"), "Hello World!".getBytes(StandardCharsets.UTF_8));
        Request request = new Request(Method.PUT, "/existing-file");
        request.headers = Collections.singletonMap(Header.CONTENT_LENGTH, "5");
        request.body = readableChannelOf("1234");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.BAD_REQUEST);
        assertThat(Files.readAllLines(directory.resolve("existing-file")))
                .containsExactly("Hello World!");
    }

    private ReadableByteChannel readableChannelOf(String body) {
        return Channels.newChannel(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
    }
}
