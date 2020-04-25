package server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import server.data.Method;
import server.handlers.Dispatcher;
import server.handlers.GetHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static java.util.Collections.singletonMap;
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
        Map<Method, Handler> routes = singletonMap(Method.GET, new GetHandler(directory));
        Handler appHandler = new Dispatcher(routes);
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
    void getRequest_largeResource() throws IOException {
        createLargeFile("large-file", 31);
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.printf("GET /large-file HTTP/1.1\r\n");
            out.printf("Host: %s:%s\r\n", HOST, PORT);
            out.printf("\r\n");

            assertThat(in.readLine()).isEqualTo("HTTP/1.1 200 OK");
            assertThat(in.readLine()).isEqualTo("Content-Length: " + (1L << 31));
            assertThat(in.readLine()).isEqualTo("");
            assertThat(countRemainingBytes(in)).isEqualTo(1L << 31);
        }
    }

    private void createLargeFile(String fileName, int powerOfTwoSize) throws IOException {
        int initialPowerSize = 25;
        try (FileChannel fc = FileChannel.open(directory.resolve(fileName), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            String a = String.join("", Collections.nCopies(1 << initialPowerSize, "a"));
            fc.write(StandardCharsets.UTF_8.encode(a));
        }
        try (FileChannel rfc = FileChannel.open(directory.resolve(fileName));
             FileChannel wfc = FileChannel.open(directory.resolve(fileName), StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            for (int i = 0; i < powerOfTwoSize - initialPowerSize; i++) {
                rfc.transferTo(0, Long.MAX_VALUE, wfc);
            }
        }
    }

    private long countRemainingBytes(BufferedReader in) throws IOException {
        char[] buffer = new char[1 << 13];
        long totalBytes = 0;
        int length;
        while ((length = in.read(buffer)) != -1) totalBytes += length;
        return totalBytes;
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
