package server;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

class Arguments {
    private static final List<Option> OPTIONS = asList(
            new Option("-p", "port", Integer::parseInt, (a, v) -> a.port = (Integer) v),
            new Option("-d", "directory", Function.identity(), (a, v) -> a.directory = (String) v));

    public Integer port;
    public String directory;

    public static Arguments parse(List<String> args) {
        validateRequired(args);

        Arguments arguments = new Arguments();
        for (Option o : OPTIONS) {
            int i = args.indexOf(o.name);
            if (args.size() <= i + 1) throw new IllegalArgumentException("Expected 1 argument for " + o.id);
            try {
                Object value = o.parse.apply(args.get(i + 1));
                o.assoc.accept(arguments, value);
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format("Invalid %s: %s", o.id, args.get(i + 1)));
            }
        }
        return arguments;
    }

    private static void validateRequired(List<String> args) {
        String missingOptions = OPTIONS.stream()
                .filter(o -> !args.contains(o.name))
                .map(o -> String.format("%s <%s>", o.name, o.id))
                .collect(Collectors.joining(", "));
        if (!missingOptions.isEmpty()) throw new IllegalArgumentException("Option(s) " + missingOptions + " required");
    }

    private static class Option {
        final String name;
        final String id;
        final Function<String, ?> parse;
        final BiConsumer<Arguments, Object> assoc;

        public Option(String name, String id, Function<String, ?> parse, BiConsumer<Arguments, Object> assoc) {
            this.name = name;
            this.id = id;
            this.parse = parse;
            this.assoc = assoc;
        }
    }
}
