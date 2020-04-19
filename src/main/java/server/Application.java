package server;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class Application {
    private static final Map<String, List<Method>> ALLOWED_METHODS = Collections.singletonMap(
            "/logs", asList(Method.GET, Method.HEAD, Method.OPTIONS)
    );
    private static final List<Method> DEFAULT_METHODS = asList(Method.GET, Method.HEAD, Method.OPTIONS, Method.PUT, Method.DELETE);

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(asList(args));

        Handler appHandler = new DefaultHandler(Paths.get(arguments.directory));
        appHandler = new MethodAuthoriser(appHandler, ALLOWED_METHODS, DEFAULT_METHODS);
        HttpServer httpServer = new HttpServer(arguments.port, appHandler);
        httpServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(httpServer::stop));
    }
}
