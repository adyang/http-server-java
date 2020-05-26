package server.handlers;

import server.data.Header;
import server.data.Request;
import server.data.Response;
import server.data.Status;
import server.util.Maps;

import java.util.concurrent.atomic.AtomicReference;

public class CatFormHandler {
    public static final String CAT_KEY = "data";
    private final AtomicReference<String> cat;

    public CatFormHandler(AtomicReference<String> cat) {
        this.cat = cat;
    }

    public Response get(Request request) {
        String cat = this.cat.get();
        if (cat == null)
            return new Response(Status.NOT_FOUND, "");
        else
            return new Response(Status.OK, CAT_KEY + "=" + cat);
    }

    public Response post(Request request) {
        if (!request.parameters.containsKey(CAT_KEY))
            return new Response(Status.BAD_REQUEST, "Unable to cat: data parameter is absent.");
        if (cat.get() != null)
            return new Response(Status.CONFLICT, "Unable to create cat: cat already present.");

        String newCat = request.parameters.get(CAT_KEY);
        cat.set(newCat);
        return new Response(Status.CREATED, Maps.of(Header.LOCATION, "/cat-form/" + CAT_KEY), "");
    }

    public Response put(Request request) {
        if (request.parameters.containsKey(CAT_KEY)) {
            String newCat = request.parameters.get(CAT_KEY);
            cat.set(newCat);
            return new Response(Status.OK, "");
        } else {
            return new Response(Status.BAD_REQUEST, "Unable to cat: data parameter is absent.");
        }
    }

    public Response delete(Request request) {
        if (cat.getAndUpdate(old -> null) == null)
            return new Response(Status.NOT_FOUND, "");
        else
            return new Response(Status.OK, "");
    }
}
