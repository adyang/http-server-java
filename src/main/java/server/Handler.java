package server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Collectors;

public class Handler {
    static Response handle(Request request, Path directory) throws IOException {
        Path resource = directory.resolve(request.uri.substring(1));
        switch (request.method) {
            case "OPTIONS":
                return options(resource);
            case "HEAD":
                return head(resource);
            case "GET":
                return get(resource);
            default:
                return new Response(500, null);
        }
    }

    private static Response options(Path resource) {
        if (resource.getFileName().equals(Paths.get("logs"))) {
            return new Response(200, Collections.singletonMap("Allow", "GET, HEAD, OPTIONS"), null);
        } else {
            return new Response(200, Collections.singletonMap("Allow", "GET, HEAD, OPTIONS, PUT, DELETE"), null);
        }
    }

    private static Response head(Path resource) {
        if (Files.exists(resource)) {
            return new Response(200, null);
        } else {
            return new Response(404, null);
        }
    }

    private static Response get(Path resource) throws IOException {
        if (Files.isRegularFile(resource)) {
            return new Response(200, new String(Files.readAllBytes(resource), StandardCharsets.UTF_8));
        } else if (Files.isDirectory(resource)) {
            String listing = Files.list(resource)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.joining(System.lineSeparator(), "", System.lineSeparator()));
            return new Response(200, listing);
        } else {
            return new Response(404, null);
        }
    }
}
