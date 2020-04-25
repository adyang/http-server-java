package server.handlers;

import server.Handler;
import server.data.Request;
import server.data.Response;
import server.data.Status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class PutHandler implements Handler {
    private final Path directory;

    public PutHandler(Path directory) {
        this.directory = directory;
    }

    @Override
    public Response handle(Request request) {
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

    private static Path write(Path resource, String content) {
        try {
            return Files.write(resource, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
