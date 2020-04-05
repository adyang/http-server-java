package server;

import static java.util.Arrays.asList;

public class Application {
    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(asList(args));

        HttpServer httpServer = new HttpServer(arguments.port, arguments.directory);
        httpServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(httpServer::stop));
    }
}
