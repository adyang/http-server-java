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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class HttpServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
    private static final String SERVER_THREAD_NAME = "server-main";
    private static final int SHUTDOWN_TIMEOUT = 30;
    private static final int SHUTDOWN_NOW_TIMEOUT = 30;

    private final int port;
    private final Handler handler;
    private final ExecutorService executor;
    private final Duration timeout;
    private final Thread serverThread;

    public HttpServer(int port, Handler handler, int numThreads) {
        this(port, handler, Executors.newFixedThreadPool(numThreads), Duration.ofMillis(500));
    }

    public HttpServer(int port, Handler handler, ExecutorService executor, Duration timeout) {
        this.port = port;
        this.handler = handler;
        this.executor = executor;
        this.timeout = timeout;
        this.serverThread = new Thread(this::serverMain, SERVER_THREAD_NAME);
        this.serverThread.setUncaughtExceptionHandler((t, e) -> logger.error("Unhandled exception.", e));
    }

    public void start() {
        serverThread.start();
    }

    private void serverMain() {
        try (ServerSocket serverSocket = newServerSocket(port, timeout)) {
            while (!executor.isShutdown()) {
                waitOrHandleConnection(serverSocket);
            }
        } catch (IOException e) {
            logger.error("Unable to open server socket or accept connection.", e);
        } catch (RejectedExecutionException e) {
            logger.info("Rejecting last connection, server is stopping.", e);
        }
    }

    private ServerSocket newServerSocket(int port, Duration timeout) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout((int) timeout.toMillis());
        return serverSocket;
    }

    private void waitOrHandleConnection(ServerSocket serverSocket) throws IOException {
        try {
            Socket clientSocket = serverSocket.accept();
            executor.submit(() -> handle(clientSocket));
        } catch (SocketTimeoutException ignore) {
        }
    }

    private void handle(Socket clientSocket) {
        try (Socket socket = clientSocket;
             PrintStream out = new PrintStream(socket.getOutputStream(), false, StandardCharsets.UTF_8.name());
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            handleConnection(in, out);
        } catch (Exception e) {
            logger.error("Unable to complete error handling of connection.", e);
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
            logger.error("Error while handling connection.", e);
            ResponseComposer.compose(out, new Response(Status.INTERNAL_SERVER_ERROR, ""));
        }
    }

    public void stop() {
        stop(executor);
        waitTillStop(serverThread);
    }

    private void stop(ExecutorService executor) {
        executor.shutdown();
        try {
            if (executor.awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.SECONDS)) return;
            executor.shutdownNow();
            if (!executor.awaitTermination(SHUTDOWN_NOW_TIMEOUT, TimeUnit.SECONDS))
                logger.error("Executor did not terminate.");
        } catch (InterruptedException e) {
            logger.warn("Interrupted while stopping executor.");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void waitTillStop(Thread serverThread) {
        try {
            serverThread.join();
        } catch (InterruptedException e) {
            logger.warn("Interrupted while stopping server.");
        }
    }
}
