package server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import server.data.Method;
import server.data.PatternHandler;
import server.handlers.Dispatcher;
import server.handlers.GetHandler;
import server.util.Maps;

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Timeout(value = 5)
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
        Map<Method, List<PatternHandler>> routes = Maps.of(
                Method.GET, singletonList(new PatternHandler("*", new GetHandler(directory))));
        Handler appHandler = new Dispatcher(routes);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        server = new HttpServer(PORT, appHandler, executor, Duration.ofSeconds(5), Duration.ofMillis(10));
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
