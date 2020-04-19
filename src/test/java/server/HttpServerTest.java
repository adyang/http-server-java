package server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HttpServerTest {
    private static final String HOST = "localhost";
    private static final int PORT = 6000;

    @TempDir
    static Path directory;

    private HttpServer server;

    @BeforeAll
    static void beforeAll() throws IOException {
        Files.write(directory.resolve("existing-file"), "Hello World!".getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() {
        Handler appHandler = new DefaultHandler(directory);
        appHandler = new MethodAuthoriser(appHandler, emptyMap(), asList(Method.GET, Method.OPTIONS, Method.HEAD));
        server = new HttpServer(PORT, appHandler, Duration.ofMillis(10));
        server.start();
    }

    @Test
    void connect_whenServerIsStarted() throws IOException {
        try (Socket socket = new Socket(HOST, PORT)) {
            assertThat(socket.isConnected()).isTrue();
        }
    }

    @Test
    void connect_whenServerIsStopped() {
        server.stop();

        assertThatThrownBy(() -> new Socket(HOST, PORT)).isInstanceOf(ConnectException.class);
    }

    @Test
    void getRequest() throws IOException {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.printf("GET /existing-file HTTP/1.1\r\n");
            out.printf("Host: %s:%s\r\n", HOST, PORT);
            out.printf("\r\n");

            assertThat(in.readLine()).isEqualTo("HTTP/1.1 200 OK");
            assertThat(in.readLine()).isEqualTo("Content-Length: 12");
            assertThat(in.readLine()).isEqualTo("");
            assertThat(in.readLine()).isEqualTo("Hello World!");
        }
    }

    @Test
    void notAllowedMethodRequest() throws IOException {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.printf("POST /existing-file HTTP/1.1\r\n");
            out.printf("Host: %s:%s\r\n", HOST, PORT);
            out.printf("\r\n");

            assertThat(in.readLine()).isEqualTo("HTTP/1.1 405 Method Not Allowed");
            assertThat(in.readLine()).isEqualTo("Allow: GET, OPTIONS, HEAD");
            assertThat(in.readLine()).isEqualTo("");
        }
    }

    @Test
    void invalidRequest_parseException() throws IOException {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.printf("GET /existing-file HTTP/1.1\r\n");
            out.printf("Host: %s:%s\r\n", HOST, PORT);
            out.printf("invalid\r\n");

            assertThat(in.readLine()).isEqualTo("HTTP/1.1 400 Bad Request");
            assertThat(in.readLine()).isEqualTo("");
            assertThat(in.readLine()).contains("Invalid header");
        }
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }
}
