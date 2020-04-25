package server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.data.Request;
import server.data.Response;
import server.data.Status;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class HttpServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

    private final int port;
    private final Handler handler;
    private final Duration timeout;
    private volatile Thread serverThread;

    public HttpServer(int port, Handler handler) {
        this(port, handler, Duration.ofMillis(500));
    }

    public HttpServer(int port, Handler handler, Duration timeout) {
        this.port = port;
        this.handler = handler;
        this.timeout = timeout;
    }

    public void start() {
        serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = newServerSocket(port, timeout)) {
                while (!Thread.currentThread().isInterrupted()) {
                    waitOrHandleConnection(serverSocket);
                }
            } catch (IOException e) {
                logger.error("Unable to open server socket.", e);
            }
        });
        serverThread.start();
    }

    private void waitOrHandleConnection(ServerSocket serverSocket) {
        try (Socket clientSocket = serverSocket.accept();
             PrintStream out = new PrintStream(clientSocket.getOutputStream(), false, StandardCharsets.UTF_8.name());
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8))) {
            handleConnection(in, out);
        } catch (SocketTimeoutException ignore) {
        } catch (Exception e) {
            logger.warn("Unable to handle connection.", e);
        }
    }

    private void handleConnection(BufferedReader in, PrintStream out) {
        try {
            Request request = RequestParser.parse(in);
            Response response = handler.handle(request);
            ResponseComposer.compose(out, response);
        } catch (RequestParser.ParseException e) {
            ResponseComposer.compose(out, new Response(Status.BAD_REQUEST, e.getMessage() + System.lineSeparator()));
        } catch (RequestParser.InvalidMethodException e) {
            ResponseComposer.compose(out, new Response(Status.NOT_IMPLEMENTED, e.getMessage() + System.lineSeparator()));
        } catch (ResponseComposer.ComposeException e) {
            throw e; // Unable to compose, hence unable to send error response
        } catch (Exception e) {
            ResponseComposer.compose(out, new Response(Status.INTERNAL_SERVER_ERROR, e.getMessage() + System.lineSeparator()));
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
