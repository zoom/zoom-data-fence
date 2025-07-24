package us.zoom.security.dfence.sql.models;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SqlObject {
    private final List<SqlDataType> arguments = new ArrayList<>();
    private List<String> names = new ArrayList<>();
    private boolean hasArguments = false;

    public String normalizedArguments() {
        StringBuilder result = new StringBuilder();
        if (hasArguments) {
            result.append("(");
            // functions and procedures can have empty arguments.
            if (!this.arguments.isEmpty()) {
                List<String> argumentsNormalized = arguments.stream().map(SqlDataType::toNormalizedName).toList();
                result.append(String.join(", ", argumentsNormalized));
            }
            result.append(")");
        }
        return result.toString();
    }

    public String normalizedName() {
        String result = String.join(".", this.names) + normalizedArguments();
        return result;
    }

    public String _quotedStringPart(String input) {
        if (input.startsWith("\"")) {
            return input;
        }
        return "\"" + input + "\"";
    }

    public String quotedName() {
        return String.join(".", this.names.stream().map(this::_quotedStringPart).toList()) + normalizedArguments();
    }
}
