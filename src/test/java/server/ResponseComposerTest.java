package server;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseComposerTest {
    @Test
    void compose_200Ok() {
        StringWriter output = new StringWriter();

        ResponseComposer.compose(new PrintWriter(output), new Response(200, "body"));

        assertThat(output.toString())
                .isEqualTo(
                        "HTTP/1.1 200 OK\r\n" +
                                "\r\n" +
                                "body\r\n");
    }

    @Test
    void compose_200Ok_noBody() {
        StringWriter output = new StringWriter();

        ResponseComposer.compose(new PrintWriter(output), new Response(200, null));

        assertThat(output.toString())
                .isEqualTo(
                        "HTTP/1.1 200 OK\r\n" +
                                "\r\n");
    }

    @Test
    void compose_404NotFound() {
        StringWriter output = new StringWriter();

        ResponseComposer.compose(new PrintWriter(output), new Response(404, null));

        assertThat(output.toString())
                .isEqualTo(
                        "HTTP/1.1 404 Not Found\r\n" +
                                "\r\n");
    }
}