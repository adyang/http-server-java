package server.handlers;

import server.Handler;
import server.data.Request;
import server.data.Response;
import server.data.Status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DeleteHandler implements Handler {
    private final Path directory;

    public DeleteHandler(Path directory) {
        this.directory = directory;
    }

    @Override
    public Response handle(Request request) throws IOException {
        Path resource = directory.resolve(request.uri.substring(1));
        if (Files.isDirectory(resource)) {
            return new Response(Status.CONFLICT, "Unable to delete: " + resource.getFileName() + " is a directory.");
        } else {
            boolean deleted = Files.deleteIfExists(resource);
            return new Response(deleted ? Status.OK : Status.NOT_FOUND, "");
        }
    }
}
