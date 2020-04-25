package server.handlers;

import server.Handler;
import server.data.Header;
import server.data.Request;
import server.data.Response;
import server.data.Status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

public class HeadHandler implements Handler {
    private final Path directory;

    public HeadHandler(Path directory) {
        this.directory = directory;
    }

    @Override
    public Response handle(Request request) throws IOException {
        Path resource = directory.resolve(request.uri.substring(1));
        if (Files.exists(resource)) {
            Map<String, Object> headers = Collections.singletonMap(Header.CONTENT_LENGTH, Files.size(resource));
            return new Response(Status.OK, headers, "");
        } else {
            return new Response(Status.NOT_FOUND, "");
        }
    }
}
