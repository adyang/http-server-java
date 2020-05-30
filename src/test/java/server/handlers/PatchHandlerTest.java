package server.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import server.data.Header;
import server.data.Method;
import server.data.Request;
import server.data.Response;
import server.data.Status;
import server.util.Maps;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;

public class PatchHandlerTest {
    @TempDir
    Path directory;
    private PatchHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PatchHandler(directory);
    }

    @Test
    void patch_absentResource() {
        Request request = new Request(Method.PATCH, "/new-file");
        String body = "lineOne\nlineTwo";
        request.headers = Maps.of(
                Header.CONTENT_LENGTH, String.valueOf(body.length()),
                Header.IF_MATCH, "doesNotMatter");
        request.body = readableChannelOf(body);

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.NOT_FOUND);
        assertThat(Files.exists(directory.resolve("new-file"))).isFalse();
    }

    @Test
    void patch_existingResource_matchingETag() throws IOException {
        String existingContent = "Hello World!";
        Files.write(directory.resolve("existing-file"), existingContent.getBytes(StandardCharsets.UTF_8));
        Request request = new Request(Method.PATCH, "/existing-file");
        String body = "New Hello World!";
        request.headers = Maps.of(
                Header.CONTENT_LENGTH, String.valueOf(body.length()),
                Header.IF_MATCH, sha1(existingContent));
        request.body = readableChannelOf(body);

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.NO_CONTENT);
        assertThat(response.headers).containsEntry(Header.E_TAG, sha1(body));
        assertThat(Files.readAllLines(directory.resolve("existing-file")))
                .containsExactly("New Hello World!");
    }

    @Test
    void patch_existingResource_mismatchedETag() throws IOException {
        String existingContent = "Hello World!";
        Files.write(directory.resolve("existing-file"), existingContent.getBytes(StandardCharsets.UTF_8));
        Request request = new Request(Method.PATCH, "/existing-file");
        String body = "New Hello World!";
        request.headers = Maps.of(
                Header.CONTENT_LENGTH, String.valueOf(body.length()),
                Header.IF_MATCH, "mismatchedETag");
        request.body = readableChannelOf(body);

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.PRECONDITION_FAILED);
        assertThat(response.body).isEqualTo("ETag does not match file checksum.");
        assertThat(Files.readAllLines(directory.resolve("existing-file")))
                .containsExactly("Hello World!");
    }

    @Test
    void patch_existingResource_noETag() throws IOException {
        String existingContent = "Hello World!";
        Files.write(directory.resolve("existing-file"), existingContent.getBytes(StandardCharsets.UTF_8));
        Request request = new Request(Method.PATCH, "/existing-file");
        String body = "New Hello World!";
        request.headers = Maps.of(Header.CONTENT_LENGTH, String.valueOf(body.length()));
        request.body = readableChannelOf(body);

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.CONFLICT);
        assertThat(response.body).isEqualTo("Missing ETag, unable to patch file.");
        assertThat(Files.readAllLines(directory.resolve("existing-file")))
                .containsExactly("Hello World!");
    }

    @Test
    void patch_existingDirectory() throws IOException {
        Files.createDirectory(directory.resolve("directory"));
        Request request = new Request(Method.PATCH, "/directory");
        String body = "New Hello World!";
        request.headers = Maps.of(
                Header.CONTENT_LENGTH, String.valueOf(body.length()),
                Header.IF_MATCH, "doesNotMatter");
        request.body = readableChannelOf(body);

        Response response = handler.handle(request);

        assertThat(response.status).isEqualTo(Status.CONFLICT);
        assertThat(response.body).isEqualTo("Unable to create/update: directory is a directory.");
    }

    private static ReadableByteChannel readableChannelOf(String body) {
        return Channels.newChannel(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
    }

    private static String sha1(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            digest.update(content.getBytes(StandardCharsets.UTF_8));
            return String.format("%040x", new BigInteger(1, digest.digest()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
