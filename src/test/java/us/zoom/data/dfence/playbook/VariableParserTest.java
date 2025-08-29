package us.zoom.data.dfence.playbook;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import us.zoom.data.dfence.exception.VariableNotFoundException;

class VariableParserTest {

  @Test
  void substituteVariables() throws VariableNotFoundException {
    String value = "${var.char1} and ${var.char2} went up the hill";
    String expected = "Jack and Jill went up the hill";
    Map<String, String> variables = new HashMap<>();
    variables.put("char1", "Jack");
    variables.put("char2", "Jill");
    String actual = VariableParser.substituteVariables(value, variables);
    assertEquals(expected, actual);
  }

  @Test
  void parseEnvironment() {
    Map<String, String> env =
        Map.of("DFENCE_FOO_BAR", "foothebar", "SOME_OTHER_VAR", "some other value.");
    Map<String, String> expected =
        new HashMap<>() {
          {
            put("foo-bar", "foothebar");
          }
        };
    Map<String, String> results = VariableParser.parseEnvironment(env);
    assertEquals(expected, results);
  }
}
