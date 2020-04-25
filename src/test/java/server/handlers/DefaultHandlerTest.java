package server.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import server.Handler;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class DefaultHandlerTest {
    @TempDir
    Path directory;
    private Handler handler;

    @BeforeEach
    void setUp() throws IOException {
        Files.write(directory.resolve("existing-file"), "Hello World!".getBytes(StandardCharsets.UTF_8));
        Files.createFile(directory.resolve("another-file"));
        Files.createFile(directory.resolve(".hidden-file"));
        Files.createDirectory(directory.resolve("directory"));
        Files.createFile(directory.resolve("directory").resolve("inner-file"));

        handler = new DefaultHandler(directory);
    }

    @Test
    void get_absentResource() throws IOException {
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
    void get_rootDirectory() throws IOException {
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
    void get_nonRootDirectory() throws IOException {
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
        Map<String, String> headers = Collections.singletonMap(Header.RANGE, "bytes=6-10");
        Request request = new Request(Method.GET, "/existing-file", headers, "");

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
        Map<String, String> headers = Collections.singletonMap(Header.RANGE, "bytes=6-18");
        Request request = new Request(Method.GET, "/existing-file", headers, "");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.PARTIAL_CONTENT);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_RANGE, "bytes 6-11/12"),
                entry(Header.CONTENT_LENGTH, 6L)
        );
        assertThat(slurpReadableByteChannel(response.body)).isEqualTo("World!");
    }

    @Test
    void get_partialContent_startAndEndPresent_largeNumbers() throws IOException {
        String byteRange = String.format("bytes=%d-%d", (Integer.MAX_VALUE + 1L), (Integer.MAX_VALUE + 2L));
        Map<String, String> headers = Collections.singletonMap(Header.RANGE, byteRange);
        Request request = new Request(Method.GET, "/existing-file", headers, "");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.REQUESTED_RANGE_NOT_SATISFIABLE);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_RANGE, "bytes */12")
        );
    }

    @Test
    void get_partialContent_startPresentOnly() throws IOException {
        Map<String, String> headers = Collections.singletonMap(Header.RANGE, "bytes=6-");
        Request request = new Request(Method.GET, "/existing-file", headers, "");

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
        Map<String, String> headers = Collections.singletonMap(Header.RANGE, "bytes=-3");
        Request request = new Request(Method.GET, "/existing-file", headers, "");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.PARTIAL_CONTENT);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_RANGE, "bytes 9-11/12"),
                entry(Header.CONTENT_LENGTH, 3L)
        );
        assertThat(slurpReadableByteChannel(response.body)).isEqualTo("ld!");
    }

    @Test
    void get_partialContent_startEqualToLengthOfResource() throws IOException {
        Map<String, String> headers = Collections.singletonMap(Header.RANGE, "bytes=12-");
        Request request = new Request(Method.GET, "/existing-file", headers, "");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.REQUESTED_RANGE_NOT_SATISFIABLE);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_RANGE, "bytes */12")
        );
    }

    @Test
    void get_partialContent_startGreaterThanLengthOfResource() throws IOException {
        Map<String, String> headers = Collections.singletonMap(Header.RANGE, "bytes=13-");
        Request request = new Request(Method.GET, "/existing-file", headers, "");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.REQUESTED_RANGE_NOT_SATISFIABLE);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_RANGE, "bytes */12")
        );
    }

    @Test
    void get_partialContent_startGreaterThanEnd() throws IOException {
        Map<String, String> headers = Collections.singletonMap(Header.RANGE, "bytes=6-1");
        Request request = new Request(Method.GET, "/existing-file", headers, "");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.REQUESTED_RANGE_NOT_SATISFIABLE);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_RANGE, "bytes */12")
        );
    }

    @Test
    void get_partialContent_endPresentOnly_isZero() throws IOException {
        Map<String, String> headers = Collections.singletonMap(Header.RANGE, "bytes=-0");
        Request request = new Request(Method.GET, "/existing-file", headers, "");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.REQUESTED_RANGE_NOT_SATISFIABLE);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_RANGE, "bytes */12")
        );
    }

    @Test
    void get_partialContent_endPresentOnly_largerThanLengthOfResource() throws IOException {
        Map<String, String> headers = Collections.singletonMap(Header.RANGE, "bytes=-13");
        Request request = new Request(Method.GET, "/existing-file", headers, "");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.PARTIAL_CONTENT);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_RANGE, "bytes 0-11/12"),
                entry(Header.CONTENT_LENGTH, 12L)
        );
        assertThat(slurpReadableByteChannel(response.body)).isEqualTo("Hello World!");
    }

    @Test
    void get_partialContent_invalidByteRange() throws IOException {
        Map<String, String> headers = Collections.singletonMap(Header.RANGE, "bytes=-");
        Request request = new Request(Method.GET, "/existing-file", headers, "");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.REQUESTED_RANGE_NOT_SATISFIABLE);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_RANGE, "bytes */12")
        );
    }

    @Test
    void get_partialContent_unknownByteUnit() throws IOException {
        Map<String, String> headers = Collections.singletonMap(Header.RANGE, "unknown=0-3");
        Request request = new Request(Method.GET, "/existing-file", headers, "");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.headers).containsOnly(
                entry(Header.CONTENT_LENGTH, 12L)
        );
        assertThat(slurpReadableByteChannel(response.body)).isEqualTo("Hello World!");
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
        Request request = new Request(Method.HEAD, "/existing-file");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.OK);
        assertThat(response.headers).containsOnly(entry(Header.CONTENT_LENGTH, 12L));
        assertThat(response.body).isEqualTo("");
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
        Request request = new Request(Method.PUT, "/directory", "New Hello World!");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.CONFLICT);
        assertThat(response.body).isEqualTo("Unable to create/update: directory is a directory.");
    }

    @Test
    void delete_existingResource() throws IOException {
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
        Request request = new Request(Method.DELETE, "/directory");

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.CONFLICT);
        assertThat(response.body).isEqualTo("Unable to delete: directory is a directory.");
    }

    private String slurpReadableByteChannel(Object body) throws IOException {
        try (ReadableByteChannel rbc = (ReadableByteChannel) body) {
            return ByteChannels.slurp(rbc);
        }
    }
}