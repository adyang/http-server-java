package server;

import org.junit.jupiter.api.Test;
import server.data.Response;
import server.data.Status;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseComposerTest {
    @Test
    void compose_stringBody() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ResponseComposer.compose(new PrintStream(output), new Response(Status.OK, "body"));

        assertThat(output.toString())
                .isEqualTo("HTTP/1.1 200 OK\r\n"
                        + "\r\n"
                        + "body");
    }

    @Test
    void compose_byteArrayBody() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ResponseComposer.compose(new PrintStream(output), new Response(Status.OK, "body".getBytes(StandardCharsets.UTF_8)));

        assertThat(output.toString())
                .isEqualTo("HTTP/1.1 200 OK\r\n"
                        + "\r\n"
                        + "body");
    }

    @Test
    void compose_readableByteChannelBody() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String content = String.join("", Collections.nCopies(1024 * 3 + 1, "a"));
        ReadableByteChannel body = Channels.newChannel(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

        ResponseComposer.compose(new PrintStream(output), new Response(Status.OK, body));

        assertThat(output.toString())
                .isEqualTo("HTTP/1.1 200 OK\r\n"
                        + "\r\n"
                        + content);
    }

    @Test
    void compose_noBody() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ResponseComposer.compose(new PrintStream(output), new Response(Status.OK, ""));

        assertThat(output.toString())
                .isEqualTo("HTTP/1.1 200 OK\r\n"
                        + "\r\n");
    }

    @Test
    void compose_404NotFound() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ResponseComposer.compose(new PrintStream(output), new Response(Status.NOT_FOUND, ""));

        assertThat(output.toString())
                .isEqualTo("HTTP/1.1 404 Not Found\r\n"
                        + "\r\n");
    }

    @Test
    void compose_headersPresent() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Map<String, Object> headers = Stream.of(
                new AbstractMap.SimpleImmutableEntry<>("headerOne", "valueOne"),
                new AbstractMap.SimpleImmutableEntry<>("headerTwo", 2L)
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        ResponseComposer.compose(new PrintStream(output), new Response(Status.OK, headers, ""));

        assertThat(output.toString())
                .isEqualTo("HTTP/1.1 200 OK\r\n"
                        + "headerOne: valueOne\r\n"
                        + "headerTwo: 2\r\n"
                        + "\r\n");
    }
}