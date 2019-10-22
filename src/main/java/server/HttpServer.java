package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Duration;

public class HttpServer {
    private final int port;
    private volatile Thread serverThread;
    private Duration timeout;

    public HttpServer(int port, Duration timeout) {
        this.port = port;
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
            out.print("HTTP/1.1 404 Not Found\r\n");
            out.print("\r\n");
            out.flush();
        } catch (SocketTimeoutException ignore) {
        }
    }

    private ServerSocket newServerSocket(int port, Duration timeout) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout((int) timeout.toMillis());
        return serverSocket;
    }

    public void stop() {
        serverThread.interrupt();
    }
}
