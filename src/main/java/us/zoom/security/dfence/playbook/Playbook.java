package us.zoom.security.dfence.playbook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import us.zoom.security.dfence.Mappers;
import us.zoom.security.dfence.exception.RbacDataError;
import us.zoom.security.dfence.exception.VariableNotFoundException;
import us.zoom.security.dfence.playbook.model.PlaybookModel;
import us.zoom.security.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.security.dfence.playbook.model.PlaybookRoleModel;

import java.util.*;
import java.util.stream.Collectors;

public class Playbook {
    private static final ObjectMapper yamlKebabObjectMapper = Mappers.yamlKebabObjectMapper();

    public static PlaybookModel parse(String value, Map<String, String> variables) throws VariableNotFoundException {
        String valuePopulated = VariableParser.substituteVariables(value, variables);
        try {
            return yamlKebabObjectMapper.readValue(valuePopulated, PlaybookModel.class);
        } catch (JsonProcessingException e) {
            throw new RbacDataError(
                    String.format("Unable to parse playbook yaml string as a valid playbook. %s", e),
                    e);
        }
    }

    public static PlaybookModel filterPlaybook(PlaybookModel playbookModel) {
        Map<String, PlaybookRoleModel> playbookRoleModelsFiltered = playbookModel.roles().keySet().stream()
                .filter(k -> playbookModel.roles().get(k).enable())
                .collect(Collectors.toMap(k -> k, k -> filterRole(playbookModel.roles().get(k))));
        return new PlaybookModel(playbookRoleModelsFiltered, playbookModel.roleOwner());
    }

    public static PlaybookRoleModel filterRole(PlaybookRoleModel playbookRoleModel) {
        List<PlaybookPrivilegeGrant> filteredGrants = playbookRoleModel.grants().stream()
                .filter(PlaybookPrivilegeGrant::enable).toList();
        return new PlaybookRoleModel(
                playbookRoleModel.name(),
                filteredGrants,
                playbookRoleModel.create(),
                playbookRoleModel.revokeOtherGrants(),
                playbookRoleModel.enable(),
                playbookRoleModel.roleOwner());
    }

    public static PlaybookModel propagateDefaults(PlaybookModel playbookModel) {
        Map<String, PlaybookRoleModel> roles = playbookModel.roles().keySet().stream().collect(Collectors.toMap(
                k -> k, k -> {
                    PlaybookRoleModel roleSource = playbookModel.roles().get(k);
                    return new PlaybookRoleModel(
                            roleSource.name(),
                            roleSource.grants(),
                            roleSource.create(),
                            roleSource.revokeOtherGrants(),
                            roleSource.enable(),
                            roleSource.roleOwner() != null ? roleSource.roleOwner() : playbookModel.roleOwner());
                }));
        return new PlaybookModel(roles, playbookModel.roleOwner());
    }


    public static String serialize(PlaybookModel playbookModel) throws JsonProcessingException {
        return yamlKebabObjectMapper.writeValueAsString(playbookModel);
    }


    public static PlaybookRoleModel consolidateRolePrivileges(PlaybookRoleModel role) {
        List<PlaybookPrivilegeGrant> consolidatedGrants = consolidatePrivileges(role.grants());
        return new PlaybookRoleModel(role.name(), consolidatedGrants);
    }

    public static List<PlaybookPrivilegeGrant> consolidatePrivileges(List<PlaybookPrivilegeGrant> grants) {
        Map<String, List<PlaybookPrivilegeGrant>> groupedRoles = new HashMap<>();
        grants.forEach(g -> {
            String key = String.join(
                    "::", List.of(
                            String.valueOf(g.databaseName()),
                            String.valueOf(g.schemaName()),
                            String.valueOf(g.objectName()),
                            String.valueOf(g.objectType()),
                            String.valueOf(g.includeFuture()),
                            String.valueOf(g.includeAll())));
            groupedRoles.putIfAbsent(key, new ArrayList<>());
            groupedRoles.get(key).add(g);
        });
        return groupedRoles.values().stream().map(rl -> {
            List<String> privileges = rl.stream().flatMap(r -> r.privileges().stream()).toList();
            return new PlaybookPrivilegeGrant(
                    rl.get(0).objectType(),
                    rl.get(0).objectName(),
                    rl.get(0).schemaName(),
                    rl.get(0).databaseName(),
                    privileges,
                    rl.get(0).includeFuture(),
                    rl.get(0).includeAll());
        }).sorted(Comparator.comparing(Record::toString)).toList();
    }
}
