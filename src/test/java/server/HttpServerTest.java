package server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HttpServerTest {
    private static final String HOST = "localhost";
    private static final int PORT = 6000;

    private HttpServer server;

    @BeforeEach
    void setUp() {
        server = new HttpServer(PORT, Duration.ofMillis(10));
        server.start();
    }

    @Test
    void connect_whenServerIsStarted() throws IOException {
        try (Socket socket = new Socket(HOST, PORT)) {
            assertThat(socket.isConnected()).isTrue();
        }
    }

    @Test
    void connect_whenServerIsStopped() throws InterruptedException {
        server.stop();
        Thread.sleep(20);

        assertThatThrownBy(() -> new Socket(HOST, PORT)).isInstanceOf(ConnectException.class);
    }

    @Test
    void get_absentResource() throws IOException {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.print("GET /does-not-exists.txt HTTP/1.1\r\n");
            out.format("Host: %s:%s\r\n", HOST, PORT);
            out.print("\r\n");

            assertThat(in.readLine()).isEqualTo("HTTP/1.1 404 Not Found");
            assertThat(in.readLine()).isEqualTo("");
        }
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        server.stop();
        Thread.sleep(20);
    }
}
