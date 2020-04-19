package server.handlers;

import server.Handler;
import server.data.Request;
import server.data.Response;
import server.data.Status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultHandler implements Handler {
    private static final int EOF = -1;
    private final Path directory;

    public DefaultHandler(Path directory) {
        this.directory = directory;
    }

    @Override
    public Response handle(Request request) throws IOException {
        Path resource = directory.resolve(request.uri.substring(1));
        switch (request.method) {
            case HEAD:
                return head(resource);
            case GET:
                return get(request, directory);
            case PUT:
                return put(request, directory);
            case DELETE:
                return delete(request, directory);
            default:
                return new Response(Status.INTERNAL_SERVER_ERROR, "");
        }
    }

    private static Response head(Path resource) throws IOException {
        if (Files.exists(resource)) {
            Map<String, Object> headers = Collections.singletonMap("Content-Length", Files.size(resource));
            return new Response(Status.OK, headers, "");
        } else {
            return new Response(Status.NOT_FOUND, "");
        }
    }

    private static Response get(Request request, Path directory) throws IOException {
        Path resource = directory.resolve(request.uri.substring(1));
        if (Files.isRegularFile(resource)) {
            Map<String, Object> headers = new HashMap<>();
            headers.put("Content-Length", Files.size(resource));
            String contentType = URLConnection.guessContentTypeFromName(resource.getFileName().toString());
            if (contentType != null) headers.put("Content-Type", contentType);
            return new Response(Status.OK, headers, Files.readAllBytes(resource));
        } else if (Files.isDirectory(resource)) {
            String listing = Files.list(resource)
                    .map(Path::getFileName)
                    .map(f -> String.format("<li><a href=\"%s\">%s</a></li>", linkOf(request.uri, f), f))
                    .collect(Collectors.joining());
            String directoryTemplate = slurp("/directory.html");
            String directoryListing = String.format(directoryTemplate, request.uri, listing);
            Map<String, Object> headers = new HashMap<>();
            headers.put("Content-Length", directoryListing.length());
            headers.put("Content-Type", "text/html");
            return new Response(Status.OK, headers, directoryListing);
        } else {
            return new Response(Status.NOT_FOUND, "");
        }
    }

    private static String linkOf(String basePath, Path filename) {
        return basePath.endsWith("/") ? basePath + filename : basePath + "/" + filename;
    }

    private static String slurp(String resourcePath) {
        try (InputStream in = DefaultHandler.class.getResourceAsStream(resourcePath);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != EOF) out.write(buffer, 0, length);
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Response put(Request request, Path directory) {
        Path resource = directory.resolve(request.uri.substring(1));
        if (Files.isRegularFile(resource)) {
            write(resource, request.body);
            return new Response(Status.OK, request.body);
        } else if (Files.notExists(resource)) {
            write(resource, request.body);
            return new Response(Status.CREATED, request.body);
        } else {
            return new Response(Status.CONFLICT, "Unable to create/update: " + resource.getFileName() + " is a directory.");
        }
    }

    private static Response delete(Request request, Path directory) throws IOException {
        Path resource = directory.resolve(request.uri.substring(1));
        if (Files.isDirectory(resource)) {
            return new Response(Status.CONFLICT, "Unable to delete: " + resource.getFileName() + " is a directory.");
        } else {
            boolean deleted = Files.deleteIfExists(resource);
            return new Response(deleted ? Status.OK : Status.NOT_FOUND, "");
        }
    }

    private static Path write(Path resource, String content) {
        try {
            return Files.write(resource, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
