package server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import server.data.Method;
import server.handlers.Dispatcher;
import server.handlers.GetHandler;
import server.handlers.PutHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(value = 1, unit = TimeUnit.MINUTES)
public class HttpServerLargeResourceTest {
    private static final String HOST = "localhost";
    private static final int PORT = 7000;

    @TempDir
    static Path directory;
    private static Path largeFile;

    private HttpServer server;

    @BeforeAll
    static void beforeAll() throws IOException {
        largeFile = createLargeFile("large-file", 31);
    }

    @BeforeEach
    void setUp() {
        Map<Method, Handler> routes = Stream.of(
                new AbstractMap.SimpleImmutableEntry<>(Method.GET, new GetHandler(directory)),
                new AbstractMap.SimpleImmutableEntry<>(Method.PUT, new PutHandler(directory))
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Handler appHandler = new Dispatcher(routes);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        server = new HttpServer(PORT, appHandler, executor, Duration.ofSeconds(5), Duration.ofMillis(10));
        server.start();
    }

    @Test
    void getRequest() throws IOException {
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

    @Test
    void putRequest() throws IOException {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.printf("PUT /large-input HTTP/1.1\r\n");
            out.printf("Host: %s:%s\r\n", HOST, PORT);
            out.printf("Content-Length: %d\r\n", 1L << 31);
            out.printf("\r\n");
            Files.copy(largeFile, socket.getOutputStream());
            out.flush();

            assertThat(in.readLine()).isEqualTo("HTTP/1.1 201 Created");
            assertThat(Files.size(directory.resolve("large-input"))).isEqualTo(1L << 31);
        }
    }

    private static Path createLargeFile(String fileName, int powerOfTwoSize) throws IOException {
        Path file = directory.resolve(fileName);
        int initialPowerSize = 25;
        try (FileChannel fc = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            String a = String.join("", Collections.nCopies(1 << initialPowerSize, "a"));
            fc.write(StandardCharsets.UTF_8.encode(a));
        }
        try (FileChannel rfc = FileChannel.open(file);
             FileChannel wfc = FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            for (int i = 0; i < powerOfTwoSize - initialPowerSize; i++) {
                rfc.transferTo(0, Long.MAX_VALUE, wfc);
            }
        }
        return file;
    }

    private long countRemainingBytes(BufferedReader in) throws IOException {
        char[] buffer = new char[1 << 13];
        long totalBytes = 0;
        int length;
        while ((length = in.read(buffer)) != -1) totalBytes += length;
        return totalBytes;
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }
}
