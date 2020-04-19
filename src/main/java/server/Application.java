package server;

import server.data.Method;
import server.handlers.Authoriser;
import server.handlers.BasicAuthenticator;
import server.handlers.DefaultHandler;
import server.handlers.OptionsHandler;

import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;

public class Application {
    private static final Map<String, Map<String, List<Method>>> ACCESS_CONTROL_LIST = Stream.of(
            new AbstractMap.SimpleImmutableEntry<>("admin", singletonMap("/logs", asList(Method.GET, Method.HEAD, Method.OPTIONS))),
            new AbstractMap.SimpleImmutableEntry<String, Map<String, List<Method>>>("anonymous", singletonMap("/logs", emptyList()))
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    private static final List<Method> DEFAULT_ACCESS = asList(Method.GET, Method.HEAD, Method.OPTIONS, Method.PUT, Method.DELETE);
    private static final String REALM = "default";
    private static final Map<String, String> CREDENTIALS_STORE = singletonMap("admin", "hunter2");
    private static final Map<String, List<Method>> ALLOWED_METHODS = singletonMap(
            "/logs", asList(Method.GET, Method.HEAD, Method.OPTIONS)
    );

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(asList(args));

        Handler appHandler = new DefaultHandler(Paths.get(arguments.directory));
        appHandler = new Authoriser(appHandler, ACCESS_CONTROL_LIST, DEFAULT_ACCESS);
        appHandler = new BasicAuthenticator(appHandler, REALM, protectedPathsFrom(ACCESS_CONTROL_LIST), CREDENTIALS_STORE);
        appHandler = new OptionsHandler(appHandler, ALLOWED_METHODS, DEFAULT_ACCESS);
        HttpServer httpServer = new HttpServer(arguments.port, appHandler);
        httpServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(httpServer::stop));
    }

    private static List<String> protectedPathsFrom(Map<String, Map<String, List<Method>>> accessControlList) {
        return accessControlList.entrySet().stream()
                .filter(e -> !e.getKey().equals("anonymous"))
                .map(Map.Entry::getValue)
                .flatMap(e -> e.keySet().stream())
                .collect(Collectors.toList());
    }
}
