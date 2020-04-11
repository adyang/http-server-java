package server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class Handler {
    static Response handle(Request request, Path directory) throws IOException {
        Path resource = directory.resolve(request.uri.substring(1));
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