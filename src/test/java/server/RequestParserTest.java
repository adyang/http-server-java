package server;

import org.junit.jupiter.api.Test;
import server.data.Header;
import server.data.Method;
import server.data.Request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.*;

class RequestParserTest {
    @Test
    void parse_requestWithoutBody() throws IOException {
        String input = "GET /existing-file HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "\r\n";
        BufferedReader in = new BufferedReader(new StringReader(input));

        Request request = RequestParser.parse(in);

        assertThat(request.method).isEqualTo(Method.GET);
        assertThat(request.uri).isEqualTo("/existing-file");
        assertThat(request.body).isEqualTo("");
    }

    @Test
    void parse_requestWithBody() throws IOException {
        String input = "PUT /existing-file HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "Content-Length: 26\r\n" +
                "\r\n" +
                "lineOne\n" +
                "lineTwo\n" +
                "lineThree\n";
        BufferedReader in = new BufferedReader(new StringReader(input));

        Request request = RequestParser.parse(in);

        assertThat(request.method).isEqualTo(Method.PUT);
        assertThat(request.uri).isEqualTo("/existing-file");
        assertThat(request.headers).containsOnly(
                entry("Host", "localhost:8080"),
                entry(Header.CONTENT_LENGTH, "26")
        );
        assertThat(request.body).isEqualTo("lineOne\nlineTwo\nlineThree\n");
    }

    @Test
    void parse_requestWithInvalidMethod() {
        String input = "INVALID /existing-file HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "\r\n";
        BufferedReader in = new BufferedReader(new StringReader(input));

        Throwable error = catchThrowable(() -> RequestParser.parse(in));

        assertThat(error).isInstanceOf(RequestParser.InvalidMethodException.class);
        assertThat(error).hasMessageContaining("Invalid method: INVALID");
    }

    @Test
    void parse_requestWithMissingEmptyLineAtEndOfStream() {
        String input = "PUT /existing-file HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "Content-Length: 26\r\n";
        BufferedReader in = new BufferedReader(new StringReader(input));

        Throwable error = catchThrowable(() -> RequestParser.parse(in));

        assertThat(error).isInstanceOf(RequestParser.ParseException.class);
        assertThat(error).hasMessageContaining("Malformed request: missing blank line after header(s)");
    }

    @Test
    void parse_requestWithInvalidContentLength() {
        String input = "PUT /existing-file HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "Content-Length: invalid\r\n" +
                "\r\n";
        BufferedReader in = new BufferedReader(new StringReader(input));

        Throwable error = catchThrowable(() -> RequestParser.parse(in));

        assertThat(error).isInstanceOf(RequestParser.ParseException.class);
        assertThat(error).hasMessageContaining("Invalid Content-Length: invalid");
    }

    @Test
    void parse_requestWithInvalidHeader() {
        String input = "PUT /existing-file HTTP/1.1\r\n" +
                "invalidHeader\r\n";

        BufferedReader in = new BufferedReader(new StringReader(input));

        Throwable error = catchThrowable(() -> RequestParser.parse(in));

        assertThat(error).isInstanceOf(RequestParser.ParseException.class);
        assertThat(error).hasMessageContaining("Invalid header: invalidHeader");
    }

    @Test
    void parse_requestWithMismatchedContentLength() {
        String input = "PUT /existing-file HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "Content-Length: 20\r\n" +
                "\r\n" +
                "lessThanTwenty";
        BufferedReader in = new BufferedReader(new StringReader(input));

        Throwable error = catchThrowable(() -> RequestParser.parse(in));

        assertThat(error).isInstanceOf(RequestParser.ParseException.class);
        assertThat(error).hasMessageContaining("Content-Length mismatched: body is not 20 byte(s)");
    }
}