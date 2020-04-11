package server;

import java.io.BufferedReader;
import java.io.IOException;

public class RequestParser {
    static Request parse(BufferedReader in) throws IOException {
        Request request = parseRequestLine(in.readLine());
        drain(in);
        return request;
    }

    private static Request parseRequestLine(String line) {
        String[] tokens = line.split(" ");
        return new Request(tokens[0], tokens[1]);
    }

    private static void drain(BufferedReader in) throws IOException {
        for (int i = 0; i < 2; i++) in.readLine();
    }
}
