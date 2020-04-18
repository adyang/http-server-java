package server;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseComposerTest {
    @Test
    void compose_200Ok() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ResponseComposer.compose(new PrintStream(output), new Response(200, "body"));

        assertThat(output.toString())
                .isEqualTo("HTTP/1.1 200 OK\r\n"
                        + "\r\n"
                        + "body");
    }

    @Test
    void compose_byteArrayBody() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ResponseComposer.compose(new PrintStream(output), new Response(200, "body".getBytes(StandardCharsets.UTF_8)));

        assertThat(output.toString())
                .isEqualTo("HTTP/1.1 200 OK\r\n"
                        + "\r\n"
                        + "body");
    }

    @Test
    void compose_200Ok_noBody() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ResponseComposer.compose(new PrintStream(output), new Response(200, ""));

        assertThat(output.toString())
                .isEqualTo("HTTP/1.1 200 OK\r\n"
                        + "\r\n");
    }

    @Test
    void compose_404NotFound() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ResponseComposer.compose(new PrintStream(output), new Response(404, ""));

        assertThat(output.toString())
                .isEqualTo("HTTP/1.1 404 Not Found\r\n"
                        + "\r\n");
    }

    @Test
    void compose_400BadRequest() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ResponseComposer.compose(new PrintStream(output), new Response(400, ""));

        assertThat(output.toString())
                .isEqualTo("HTTP/1.1 400 Bad Request\r\n"
                        + "\r\n");
    }

    @Test
    void compose_headersPresent() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Map<String, Object> headers = Stream.of(
                new AbstractMap.SimpleImmutableEntry<>("headerOne", "valueOne"),
                new AbstractMap.SimpleImmutableEntry<>("headerTwo", "valueTwo")
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        ResponseComposer.compose(new PrintStream(output), new Response(200, headers, ""));

        assertThat(output.toString())
                .isEqualTo("HTTP/1.1 200 OK\r\n"
                        + "headerOne: valueOne\r\n"
                        + "headerTwo: valueTwo\r\n"
                        + "\r\n");
    }
}