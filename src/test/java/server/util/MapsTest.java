package server.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;

public class MapsTest {
    @Test
    void immutable_empty() {
        Map<String, Integer> map = Maps.of();

        assertThat(map).isEmpty();
        assertThatThrownBy(() -> map.put("new", 1)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void immutable_singleEntry() {
        Map<String, Integer> map = Maps.of("first", 1);

        assertThat(map).containsOnly(entry("first", 1));
        assertThatThrownBy(() -> map.put("new", 1)).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest(name = "{index} entries)")
    @MethodSource("entriesFromTwoToTen")
    void immutable_twoToTenEntries(Object[] inputs, Class<?>[] parameterTypes, Map.Entry<String, Integer>[] expectedEntries) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method mapsOf = Maps.class.getMethod("of", parameterTypes);

        Map<String, Integer> map = (Map<String, Integer>) mapsOf.invoke(null, inputs);

        assertThat(map).containsOnly(expectedEntries);
        assertThatThrownBy(() -> map.put("new", 1)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mutable_empty() {
        Map<String, Integer> map = Maps.mutableOf();

        assertThat(map).isEmpty();
        map.put("new", 1);
        assertThat(map).containsEntry("new", 1);
    }

    @Test
    void mutable_singleEntry() {
        Map<String, Integer> map = Maps.mutableOf("first", 1);

        assertThat(map).containsOnly(entry("first", 1));
        map.put("new", 1);
        assertThat(map).containsEntry("new", 1);
    }

    @ParameterizedTest(name = "{index} entries)")
    @MethodSource("entriesFromTwoToTen")
    void mutable_twoToTenEntries(Object[] inputs, Class<?>[] parameterTypes, Map.Entry<String, Integer>[] expectedEntries) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method mapsMutableOf = Maps.class.getMethod("mutableOf", parameterTypes);

        Map<String, Integer> map = (Map<String, Integer>) mapsMutableOf.invoke(null, inputs);

        assertThat(map).containsOnly(expectedEntries);
        map.put("new", 1);
        assertThat(map).containsEntry("new", 1);
    }

    private static Stream<Arguments> entriesFromTwoToTen() {
        List<Map.Entry<String, Integer>> allEntries = asList(
                entry("first", 1), entry("second", 2), entry("third", 3), entry("fourth", 4), entry("fifth", 5),
                entry("sixth", 6), entry("seventh", 7), entry("eighth", 8), entry("ninth", 9), entry("tenth", 10));
        return IntStream.range(0, allEntries.size())
                .mapToObj(i -> allEntries.subList(0, i + 1))
                .map(MapsTest::argumentsForCaseWith);
    }

    private static Arguments argumentsForCaseWith(List<Map.Entry<String, Integer>> currEntries) {
        Object[] inputs = currEntries.stream().flatMap(e -> Stream.of(e.getKey(), e.getValue())).toArray();
        Class<?>[] parameterTypes = Collections.nCopies(inputs.length, Object.class).toArray(new Class<?>[0]);
        Map.Entry<String, Integer>[] expectedEntries = currEntries.toArray(new Map.Entry[0]);
        return Arguments.of(inputs, parameterTypes, expectedEntries);
    }
}
