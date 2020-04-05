package server;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ArgumentsTest {
    @Test
    void parse_optionsPresent() {
        Arguments arguments = Arguments.parse(asList("-p", "1234", "-d", "/path/to/directory"));

        assertThat(arguments.port).isEqualTo(1234);
        assertThat(arguments.directory).isEqualTo("/path/to/directory");
    }

    @Test
    void parse_portOptionAbsent() {
        Throwable error = catchThrowable(() -> Arguments.parse(asList("-d", "/path/to/directory")));

        assertThat(error).isInstanceOf(IllegalArgumentException.class);
        assertThat(error).hasMessageContaining("Option(s) -p <port> required");
    }

    @Test
    void parse_directoryOptionAbsent() {
        Throwable error = catchThrowable(() -> Arguments.parse(asList("-p", "1234")));

        assertThat(error).isInstanceOf(IllegalArgumentException.class);
        assertThat(error).hasMessageContaining("Option(s) -d <directory> required");
    }

    @Test
    void parse_portAndDirectoryOptionsAbsent() {
        Throwable error = catchThrowable(() -> Arguments.parse(emptyList()));

        assertThat(error).isInstanceOf(IllegalArgumentException.class);
        assertThat(error).hasMessageContaining("Option(s) -p <port>, -d <directory> required");
    }

    @Test
    void parse_invalidPort() {
        Throwable error = catchThrowable(() -> Arguments.parse(asList("-p", "notNumber", "-d", "/path/to/directory")));

        assertThat(error).isInstanceOf(IllegalArgumentException.class);
        assertThat(error).hasMessageContaining("Invalid port: notNumber");
    }

    @Test
    void parse_missingPortArgument() {
        Throwable error = catchThrowable(() -> Arguments.parse(asList("-d", "/path/to/directory", "-p")));

        assertThat(error).isInstanceOf(IllegalArgumentException.class);
        assertThat(error).hasMessageContaining("Expected 1 argument for port");
    }

    @Test
    void parse_missingDirectoryArgument() {
        Throwable error = catchThrowable(() -> Arguments.parse(asList("-p", "1234", "-d")));

        assertThat(error).isInstanceOf(IllegalArgumentException.class);
        assertThat(error).hasMessageContaining("Expected 1 argument for directory");
    }
}