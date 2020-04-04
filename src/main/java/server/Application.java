package server;

import java.util.List;

import static java.util.Arrays.asList;

public class Application {
    public static void main(String[] args) {
        Arguments arguments = parse(asList(args));
        if (arguments.error != null) {
            System.err.printf("Error: %s", arguments.error);
            System.exit(1);
        }

        HttpServer httpServer = new HttpServer(arguments.port);
        httpServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(httpServer::stop));
    }

    private static Arguments parse(List<String> args) {
        if (!args.contains("-p")) {
            return Arguments.error("Option -p <port> is required");
        }
        Arguments arguments = new Arguments();
        for (int i = 0; i < args.size(); i++) {
            if ("-p".equals(args.get(i))) {
                try {
                    arguments.port = Integer.parseInt(args.get(i + 1));
                } catch (NumberFormatException e) {
                    arguments.error = "Invalid port: " + args.get(i + 1);
                    return arguments;
                }
            }
        }
        return arguments;
    }

    private static class Arguments {
        Integer port;
        String error;

        static Arguments error(String error) {
            Arguments arguments = new Arguments();
            arguments.error = error;
            return arguments;
        }
    }
}
