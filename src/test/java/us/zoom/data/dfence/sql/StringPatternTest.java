package us.zoom.data.dfence.sql;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class StringPatternTest {

    static Stream<SplitIgnoreTestParams> splitIgnoreTestParams() {
        return Stream.of(
                new SplitIgnoreTestParams("ABC,DEF(ABC,123)", ',', '(', ')', List.of("ABC", "DEF(ABC,123)")),
                new SplitIgnoreTestParams(
                        "ABC,DEF(ABC,(ABC,DEF))",
                        ',',
                        '(',
                        ')',
                        List.of("ABC", "DEF(ABC,(ABC,DEF))")),
                new SplitIgnoreTestParams(
                        "\"ABC,DEF\",D\"E\"F(ABC,(ABC,DEF))",
                        ',',
                        '(',
                        ')',
                        List.of("\"ABC,DEF\"", "D\"E\"F(ABC,(ABC,DEF))")),
                new SplitIgnoreTestParams("\"ABC.DEF\".GHI.JKL", '.', '(', ')', List.of("\"ABC.DEF\"", "GHI", "JKL")),
                new SplitIgnoreTestParams("", '.', '(', ')', List.of())

        );
    }

    @ParameterizedTest
    @MethodSource("splitIgnoreTestParams")
    void splitIgnore(SplitIgnoreTestParams params) {
        List<String> actual = StringPattern.splitIgnore(
                params.value,
                params.delimiter,
                params.ignoreStart,
                params.ignoreEnd);
        assertArrayEquals(params.expected.toArray(), actual.toArray());
    }

    record SplitIgnoreTestParams(
            String value,
            char delimiter,
            char ignoreStart,
            char ignoreEnd,
            List<String> expected) {
    }
}