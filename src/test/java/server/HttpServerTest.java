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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.util.Lists.list;

public class HttpServerTest {
    private static final String HOST = "localhost";
    private static final int PORT = 6000;

    @TempDir
    static Path directory;

    private HttpServer server;

    @BeforeAll
    static void beforeAll() throws IOException {
        Files.write(directory.resolve("existing-file"), "Hello World!".getBytes(StandardCharsets.UTF_8));
        Files.createFile(directory.resolve("another-file"));
        Files.createFile(directory.resolve(".hidden-file"));
        Files.createDirectory(directory.resolve("directory"));
    }

    @BeforeEach
    void setUp() {
        server = new HttpServer(PORT, directory.toString(), Duration.ofMillis(10));
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
    void get_absentResource() throws IOException {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.printf("GET /does-not-exists HTTP/1.1\r\n");
            out.printf("Host: %s:%s\r\n", HOST, PORT);
            out.printf("\r\n");

            assertThat(in.readLine()).isEqualTo("HTTP/1.1 404 Not Found");
            assertThat(in.readLine()).isEqualTo("");
        }
    }

    @Test
    void get_existingResource() throws IOException {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.printf("GET /existing-file HTTP/1.1\r\n");
            out.printf("Host: %s:%s\r\n", HOST, PORT);
            out.printf("\r\n");

            assertThat(in.readLine()).isEqualTo("HTTP/1.1 200 OK");
            assertThat(in.readLine()).isEqualTo("");
            assertThat(in.readLine()).isEqualTo("Hello World!");
        }
    }

    @Test
    void get_directory() throws IOException {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.printf("GET / HTTP/1.1\r\n");
            out.printf("Host: %s:%s\r\n", HOST, PORT);
            out.printf("\r\n");

            assertThat(in.readLine()).isEqualTo("HTTP/1.1 200 OK");
            assertThat(in.readLine()).isEqualTo("");
            assertThat(list(in.readLine(), in.readLine(), in.readLine(), in.readLine()))
                    .containsExactlyInAnyOrder("existing-file", "another-file", ".hidden-file", "directory");
        }
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }
}
