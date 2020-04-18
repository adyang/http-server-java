package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

public class HttpServer {
    private final int port;
    private final Path directory;
    private volatile Thread serverThread;
    private Duration timeout;

    public HttpServer(int port, String directory) {
        this(port, directory, Duration.ofMillis(500));
    }

    public HttpServer(int port, String directory, Duration timeout) {
        this.port = port;
        this.directory = Paths.get(directory);
        this.timeout = timeout;
    }

    public void start() {
        serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = newServerSocket(port, timeout)) {
                while (!Thread.currentThread().isInterrupted()) {
                    waitOrHandleConnection(serverSocket);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        serverThread.start();
    }

    private void waitOrHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket clientSocket = serverSocket.accept();
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            handleConnection(in, out);
        } catch (SocketTimeoutException ignore) {
        }
    }

    private void handleConnection(BufferedReader in, PrintWriter out) throws IOException {
        try {
            Request request = RequestParser.parse(in);
            Response response = Handler.handle(request, directory);
            ResponseComposer.compose(out, response);
        } catch (RequestParser.ParseException e) {
            ResponseComposer.compose(out, new Response(400, e.getMessage() + System.lineSeparator()));
        }
    }

    private ServerSocket newServerSocket(int port, Duration timeout) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout((int) timeout.toMillis());
        return serverSocket;
    }

    public void stop() {
        serverThread.interrupt();
        waitTillStop(serverThread);
    }

    private void waitTillStop(Thread serverThread) {
        try {
            serverThread.join();
        } catch (InterruptedException e) {
            System.err.println("Interrupted while stopping server.");
        }
    }
}
