package server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Collectors;

public class Handler {
    private static final int EOF = -1;

    static Response handle(Request request, Path directory) throws IOException {
        Path resource = directory.resolve(request.uri.substring(1));
        switch (request.method) {
            case "OPTIONS":
                return options(resource);
            case "HEAD":
                return head(resource);
            case "GET":
                return get(request, directory);
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

    private static Response get(Request request, Path directory) throws IOException {
        Path resource = directory.resolve(request.uri.substring(1));
        if (Files.isRegularFile(resource)) {
            return new Response(200, new String(Files.readAllBytes(resource), StandardCharsets.UTF_8));
        } else if (Files.isDirectory(resource)) {
            String listing = Files.list(resource)
                    .map(Path::getFileName)
                    .map(f -> String.format("<li><a href=\"%s\">%s</a></li>", linkOf(request.uri, f), f))
                    .collect(Collectors.joining());
            String directoryTemplate = slurp("/directory.html");
            return new Response(200, String.format(directoryTemplate, request.uri, listing));
        } else {
            return new Response(404, null);
        }
    }

    private static String linkOf(String basePath, Path filename) {
        return basePath.endsWith("/") ? basePath + filename : basePath + "/" + filename;
    }

    private static String slurp(String resourcePath) {
        try (InputStream in = Handler.class.getResourceAsStream(resourcePath);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != EOF) out.write(buffer, 0, length);
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
