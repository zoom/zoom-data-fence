package us.zoom.security.dfence.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine;
import us.zoom.security.dfence.Mappers;
import us.zoom.security.dfence.Provider;

import java.util.List;

@CommandLine.Command(
        name = "list-roles", description = "List all roles in the database.", mixinStandardHelpOptions = true)
public class ListRolesCommand extends ProviderCommand {
    private static final ObjectMapper yamlKebabObjectMapper = Mappers.yamlKebabObjectMapper();

    @Override
    Integer unhandledCall() {
        Provider provider = getProvider();
        List<String> roles = provider.listRoles();
        String rolesYaml;
        try {
            rolesYaml = yamlKebabObjectMapper.writeValueAsString(roles);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to write roles to string.", e);
        }
        System.out.println(rolesYaml);
        return 0;
    }
}
