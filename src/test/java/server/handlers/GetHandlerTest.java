package server.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import server.data.Header;
import server.data.Method;
import server.data.Request;
import server.data.Response;
import server.data.Status;
import server.util.ByteChannels;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class GetHandlerTest {
    @TempDir
    Path directory;
    private GetHandler handler;

    @BeforeEach
    void setUp() throws IOException {
        Files.write(directory.resolve("existing-file"), "Hello World!".getBytes(StandardCharsets.UTF_8));
        Files.createFile(directory.resolve("another-file"));
        Files.createFile(directory.resolve(".hidden-file"));
        Files.createDirectory(directory.resolve("directory"));
        Files.createFile(directory.resolve("directory").resolve("inner-file"));

        handler = new GetHandler(directory);
    }

    @Test
    void get_absentResource() {
        Request request = new Request(Method.GET, "/does-not-exists");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.NOT_FOUND);
        assertThat(response.body).isEqualTo("");
    }

    @Test
    void get_existingResource() throws IOException {
        Request request = new Request(Method.GET, "/existing-file");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.headers).containsOnly(entry(Header.CONTENT_LENGTH, 12L));
        assertThat(slurpReadableByteChannel(response.body)).isEqualTo("Hello World!");
    }

    @Test
    void get_jpegImage() throws IOException {
        Files.createFile(directory.resolve("image.jpeg"));
        Request request = new Request(Method.GET, "/image.jpeg");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.headers).containsEntry(Header.CONTENT_TYPE, "image/jpeg");
    }

    @Test
    void get_pngImage() throws IOException {
        Files.createFile(directory.resolve("image.png"));
        Request request = new Request(Method.GET, "/image.png");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.headers).containsEntry(Header.CONTENT_TYPE, "image/png");
    }

    @Test
    void get_gifImage() throws IOException {
        Files.createFile(directory.resolve("image.gif"));
        Request request = new Request(Method.GET, "/image.gif");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.headers).containsEntry(Header.CONTENT_TYPE, "image/gif");
    }

    @Test
    void get_textFile() throws IOException {
        Files.createFile(directory.resolve("file.txt"));
        Request request = new Request(Method.GET, "/file.txt");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.headers).containsEntry(Header.CONTENT_TYPE, "text/plain");
    }

    @Test
    void get_rootDirectory() {
        Request request = new Request(Method.GET, "/");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.headers).containsKeys(Header.CONTENT_LENGTH);
        assertThat(response.headers).containsEntry(Header.CONTENT_TYPE, "text/html");
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
    void get_nonRootDirectory() {
        Request request = new Request(Method.GET, "/directory");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat((String) response.body)
                .contains("<title>Directory: /directory</title>")
                .contains("<h1>Directory: /directory</h1>")
                .contains("<li><a href=\"/directory/inner-file\">inner-file</a></li>");
    }

    @Test
    void get_partialContent_startAndEndPresent() throws IOException {
        Request request = new Request(Method.GET, "/existing-file");
        request.headers = Collections.singletonMap(Header.RANGE, "bytes=6-10");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.PARTIAL_CONTENT);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_RANGE, "bytes 6-10/12"),
                entry(Header.CONTENT_LENGTH, 5L)
        );
        assertThat(slurpReadableByteChannel(response.body)).isEqualTo("World");
    }

    @Test
    void get_partialContent_startAndEndPresent_endLargerThanLengthOfResource() throws IOException {
        Request request = new Request(Method.GET, "/existing-file");
        request.headers = Collections.singletonMap(Header.RANGE, "bytes=6-18");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.PARTIAL_CONTENT);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_RANGE, "bytes 6-11/12"),
                entry(Header.CONTENT_LENGTH, 6L)
        );
        assertThat(slurpReadableByteChannel(response.body)).isEqualTo("World!");
    }

    @Test
    void get_partialContent_startAndEndPresent_largeNumbers() {
        String byteRange = String.format("bytes=%d-%d", (Integer.MAX_VALUE + 1L), (Integer.MAX_VALUE + 2L));
        Request request = new Request(Method.GET, "/existing-file");
        request.headers = Collections.singletonMap(Header.RANGE, byteRange);

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.REQUESTED_RANGE_NOT_SATISFIABLE);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_RANGE, "bytes */12")
        );
    }

    @Test
    void get_partialContent_startPresentOnly() throws IOException {
        Request request = new Request(Method.GET, "/existing-file");
        request.headers = Collections.singletonMap(Header.RANGE, "bytes=6-");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.PARTIAL_CONTENT);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_RANGE, "bytes 6-11/12"),
                entry(Header.CONTENT_LENGTH, 6L)
        );
        assertThat(slurpReadableByteChannel(response.body)).isEqualTo("World!");
    }

    @Test
    void get_partialContent_endPresentOnly() throws IOException {
        Request request = new Request(Method.GET, "/existing-file");
        request.headers = Collections.singletonMap(Header.RANGE, "bytes=-3");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.PARTIAL_CONTENT);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_RANGE, "bytes 9-11/12"),
                entry(Header.CONTENT_LENGTH, 3L)
        );
        assertThat(slurpReadableByteChannel(response.body)).isEqualTo("ld!");
    }

    @Test
    void get_partialContent_startEqualToLengthOfResource() {
        Request request = new Request(Method.GET, "/existing-file");
        request.headers = Collections.singletonMap(Header.RANGE, "bytes=12-");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.REQUESTED_RANGE_NOT_SATISFIABLE);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_RANGE, "bytes */12")
        );
    }

    @Test
    void get_partialContent_startGreaterThanLengthOfResource() {
        Request request = new Request(Method.GET, "/existing-file");
        request.headers = Collections.singletonMap(Header.RANGE, "bytes=13-");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.REQUESTED_RANGE_NOT_SATISFIABLE);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_RANGE, "bytes */12")
        );
    }

    @Test
    void get_partialContent_startGreaterThanEnd() {
        Request request = new Request(Method.GET, "/existing-file");
        request.headers = Collections.singletonMap(Header.RANGE, "bytes=6-1");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.REQUESTED_RANGE_NOT_SATISFIABLE);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_RANGE, "bytes */12")
        );
    }

    @Test
    void get_partialContent_endPresentOnly_isZero() {
        Request request = new Request(Method.GET, "/existing-file");
        request.headers = Collections.singletonMap(Header.RANGE, "bytes=-0");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.REQUESTED_RANGE_NOT_SATISFIABLE);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_RANGE, "bytes */12")
        );
    }

    @Test
    void get_partialContent_endPresentOnly_largerThanLengthOfResource() throws IOException {
        Request request = new Request(Method.GET, "/existing-file");
        request.headers = Collections.singletonMap(Header.RANGE, "bytes=-13");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.PARTIAL_CONTENT);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_RANGE, "bytes 0-11/12"),
                entry(Header.CONTENT_LENGTH, 12L)
        );
        assertThat(slurpReadableByteChannel(response.body)).isEqualTo("Hello World!");
    }

    @Test
    void get_partialContent_invalidByteRange() {
        Request request = new Request(Method.GET, "/existing-file");
        request.headers = Collections.singletonMap(Header.RANGE, "bytes=-");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.REQUESTED_RANGE_NOT_SATISFIABLE);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_RANGE, "bytes */12")
        );
    }

    @Test
    void get_partialContent_unknownByteUnit() throws IOException {
        Request request = new Request(Method.GET, "/existing-file");
        request.headers = Collections.singletonMap(Header.RANGE, "unknown=0-3");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_LENGTH, 12L)
        );
        assertThat(slurpReadableByteChannel(response.body)).isEqualTo("Hello World!");
    }

    private String slurpReadableByteChannel(Object body) throws IOException {
        try (ReadableByteChannel rbc = (ReadableByteChannel) body) {
            return ByteChannels.slurp(rbc);
        }
    }
}
