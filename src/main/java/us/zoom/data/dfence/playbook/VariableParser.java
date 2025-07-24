package us.zoom.data.dfence.playbook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import us.zoom.data.dfence.Mappers;
import us.zoom.data.dfence.exception.VariableNotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class VariableParser {
    private static final TypeReference<HashMap<String, String>> genericTypeReference = new TypeReference<>() {
    };
    private static final ObjectMapper yamlObjectMapper = Mappers.yamlObjectMapper();
    private static final Pattern varPattern = Pattern.compile("(\\$\\{var\\.)([a-z0-9-]+)(\\})");

    public static String substituteVariables(String value, Map<String, String> variables)
            throws VariableNotFoundException {
        Matcher matcher = varPattern.matcher(value);
        StringBuilder stringBuilder = new StringBuilder();
        while (matcher.find()) {
            String variableName = matcher.group(2);
            String variableValue = variables.get(variableName);
            if (variableValue == null) {
                throw new VariableNotFoundException(String.format("variable %s not found", variableName));
            }
            matcher.appendReplacement(stringBuilder, variableValue);
        }
        matcher.appendTail(stringBuilder);
        return stringBuilder.toString();
    }

    public static HashMap<String, String> parseVariables(String value) throws JsonProcessingException {
        return new HashMap<>(yamlObjectMapper.readValue(value, genericTypeReference));
    }

    public static Map<String, String> parseEnvironment(Map<String, String> allEnvVariables) {
        return allEnvVariables.entrySet().stream().filter(e -> e.getKey().startsWith("DFENCE_")).collect(Collectors.toMap(
                e -> CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, e.getKey().substring(7)),
                Map.Entry::getValue));
    }
}
