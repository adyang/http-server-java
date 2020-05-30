package server;

import server.data.Method;
import server.data.PatternHandler;
import server.handlers.Authoriser;
import server.handlers.BasicAuthenticator;
import server.handlers.CatFormHandler;
import server.handlers.CookieHandler;
import server.handlers.DefaultResponseHeaderWrapper;
import server.handlers.DeleteHandler;
import server.handlers.Dispatcher;
import server.handlers.GetHandler;
import server.handlers.HeadHandler;
import server.handlers.OptionsHandler;
import server.handlers.ParametersEchoHandler;
import server.handlers.ParametersWrapper;
import server.handlers.PatchHandler;
import server.handlers.PutHandler;
import server.handlers.RedirectHandler;
import server.handlers.TeapotHandler;
import server.util.Maps;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class Application {
    private static final Map<String, Map<String, List<Method>>> ACCESS_CONTROL_LIST = Maps.of(
            "admin", Maps.of("/logs", asList(Method.GET, Method.HEAD, Method.OPTIONS)),
            "anonymous", Maps.of(
                    "/logs", emptyList(),
                    "/cat-form", asList(Method.GET, Method.PUT, Method.POST, Method.DELETE)));
    private static final List<Method> DEFAULT_ACCESS = asList(Method.GET, Method.HEAD, Method.OPTIONS, Method.PUT, Method.DELETE, Method.PATCH);
    private static final String REALM = "default";
    private static final Map<String, String> CREDENTIALS_STORE = Maps.of("admin", "hunter2");
    private static final Map<String, List<Method>> ALLOWED_METHODS = Maps.of(
            "/logs", asList(Method.GET, Method.HEAD, Method.OPTIONS)
    );
    private static final Duration SO_TIMEOUT = Duration.ofSeconds(20);

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(asList(args));
        System.setProperty("logDir", arguments.directory);

        Handler appHandler = new Dispatcher(routes(Paths.get(arguments.directory)));
        appHandler = new ParametersWrapper(appHandler);
        appHandler = new Authoriser(appHandler, ACCESS_CONTROL_LIST, DEFAULT_ACCESS);
        appHandler = new BasicAuthenticator(appHandler, REALM, protectedPathsFrom(ACCESS_CONTROL_LIST), CREDENTIALS_STORE);
        appHandler = new OptionsHandler(appHandler, ALLOWED_METHODS, DEFAULT_ACCESS);
        appHandler = new DefaultResponseHeaderWrapper(appHandler);
        int numThreads = Runtime.getRuntime().availableProcessors() * (1 + 18);
        HttpServer httpServer = new HttpServer(arguments.port, appHandler, numThreads, SO_TIMEOUT);
        httpServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(httpServer::stop));
    }

    private static Map<Method, List<PatternHandler>> routes(Path directory) {
        CatFormHandler catForm = new CatFormHandler(new AtomicReference<>());
        return Maps.of(
                Method.HEAD, singletonList(new PatternHandler("*", new HeadHandler(directory))),
                Method.GET, asList(
                        new PatternHandler("/cookie", CookieHandler::cookie),
                        new PatternHandler("/eat_cookie", CookieHandler::eatCookie),
                        new PatternHandler("/parameters", ParametersEchoHandler::handle),
                        new PatternHandler("/cat-form/data", catForm::get),
                        new PatternHandler("/redirect", new RedirectHandler("/")),
                        new PatternHandler("/coffee", TeapotHandler::handleCoffee),
                        new PatternHandler("/tea", TeapotHandler::handleTea),
                        new PatternHandler("*", new GetHandler(directory))),
                Method.PUT, asList(
                        new PatternHandler("/cat-form/data", catForm::put),
                        new PatternHandler("*", new PutHandler(directory))),
                Method.POST, singletonList(new PatternHandler("/cat-form", catForm::post)),
                Method.DELETE, asList(
                        new PatternHandler("/cat-form/data", catForm::delete),
                        new PatternHandler("*", new DeleteHandler(directory))),
                Method.PATCH, singletonList(new PatternHandler("*", new PatchHandler(directory)))
        );
    }

    private static List<String> protectedPathsFrom(Map<String, Map<String, List<Method>>> accessControlList) {
        return accessControlList.entrySet().stream()
                .filter(e -> !e.getKey().equals("anonymous"))
                .map(Map.Entry::getValue)
                .flatMap(e -> e.keySet().stream())
                .collect(Collectors.toList());
    }
}
