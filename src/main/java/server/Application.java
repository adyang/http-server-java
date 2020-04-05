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

        HttpServer httpServer = new HttpServer(arguments.port, arguments.directory);
        httpServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(httpServer::stop));
    }

    private static Arguments parse(List<String> args) {
        if (!args.contains("-p")) {
            return Arguments.error("Option -p <port> is required");
        }
        if (!args.contains("-d")) {
            return Arguments.error("Option -d <serving-directory> is required");
        }
        Arguments arguments = new Arguments();
        for (int i = 0; i < args.size(); i++) {
            switch (args.get(i)) {
                case "-p":
                    try {
                        arguments.port = Integer.parseInt(args.get(i + 1));
                    } catch (NumberFormatException e) {
                        arguments.error = "Invalid port: " + args.get(i + 1);
                        return arguments;
                    }
                    break;
                case "-d":
                    arguments.directory = args.get(i + 1);
                    break;
            }
        }
        return arguments;
    }

    private static class Arguments {
        Integer port;
        String directory;
        String error;

        static Arguments error(String error) {
            Arguments arguments = new Arguments();
            arguments.error = error;
            return arguments;
        }
    }
}
