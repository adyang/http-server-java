package server.data;

import server.Handler;

public class PatternHandler {
    public final String pattern;
    public final Handler handler;

    public PatternHandler(String pattern, Handler handler) {
        this.pattern = pattern;
        this.handler = handler;
    }
}
