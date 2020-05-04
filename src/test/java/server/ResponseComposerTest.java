package server;

import org.junit.jupiter.api.Test;
import server.data.Response;
import server.data.Status;
import server.util.Maps;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

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
        Map<String, Object> headers = Maps.of(
                "headerOne", "valueOne",
                "headerTwo", 2L
        );

        ResponseComposer.compose(new PrintStream(output), new Response(Status.OK, headers, ""));

        assertThat(output.toString())
                .isEqualTo("HTTP/1.1 200 OK\r\n"
                        + "headerOne: valueOne\r\n"
                        + "headerTwo: 2\r\n"
                        + "\r\n");
    }

    @Test
    void compose_exceptionOnWritingResponse() {
        IOException exception = new IOException("Error during write.");
        ErrorPrintStream out = new ErrorPrintStream(exception);
        Response response = new Response(Status.OK, new byte[0]);

        Throwable error = catchThrowable(() -> ResponseComposer.compose(out, response));

        assertThat(error).isInstanceOf(ResponseComposer.ComposeException.class);
        assertThat(error).hasRootCause(exception);
    }

    private static class ErrorPrintStream extends PrintStream {
        private final IOException exception;

        public ErrorPrintStream(IOException exception) {
            super(new ByteArrayOutputStream());
            this.exception = exception;
        }

        @Override
        public void write(byte[] b) throws IOException {
            throw exception;
        }
    }
}