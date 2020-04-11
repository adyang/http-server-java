package server;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;

class RequestParserTest {
    @Test
    void parse_getRequest() throws IOException {
        String input = "GET /existing-file HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "\r\n";
        BufferedReader in = new BufferedReader(new StringReader(input));

        Request request = RequestParser.parse(in);

        assertThat(request.method).isEqualTo("GET");
        assertThat(request.uri).isEqualTo("/existing-file");
    }
}