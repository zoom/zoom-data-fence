t package us.zoom.data.dfence.playbook.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.UnsupportedRevokeBehavior;

import java.util.*;

public record PlaybookModel(
        Map<String, PlaybookRoleModel> roles, String roleOwner, UnsupportedRevokeBehavior unsupportedRevokeBehavior) {
    public PlaybookModel {
        roles = Map.copyOf(roles);
    }

    public PlaybookModel(Map<String, PlaybookRoleModel> roles) {
        this(roles, null, null);
    }
    
    public PlaybookModel(Map<String, PlaybookRoleModel> roles, String roleOwner) {
        this(roles, roleOwner, null);
    }


    public static PlaybookModel merge(List<PlaybookModel> playbooks) {
        Set<String> intersectionRoleKeys = new HashSet<>();
        HashMap<String, PlaybookRoleModel> newRoles = new HashMap<>();
        List<String> roleOwners = new ArrayList<>();
        List<UnsupportedRevokeBehavior> unsupportedRevokeBehaviors = new ArrayList<>();
        playbooks.forEach(playbook -> {
            intersectionRoleKeys.clear();
            intersectionRoleKeys.addAll(newRoles.keySet());
            intersectionRoleKeys.retainAll(playbook.roles.keySet());
            if (intersectionRoleKeys.size() > 0) {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    throw new RbacDataError(String.format(
                            "Cannot merge playbook models. The following role keys are duplicates: %s",
                            objectMapper.writeValueAsString(intersectionRoleKeys)));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            newRoles.putAll(playbook.roles);
            if (playbook.roleOwner != null) {
                roleOwners.add(playbook.roleOwner);
            }
            if (playbook.unsupportedRevokeBehavior != null) {
                unsupportedRevokeBehaviors.add(playbook.unsupportedRevokeBehavior);
            }
        });
        String roleOwner = null;
        if (roleOwners.size() == 1) {
            roleOwner = roleOwners.get(0);
        }
        if (roleOwners.size() > 1) {
            throw new RbacDataError("Role owner is declared more than once with values " + String.join(
                    ", ",
                    roleOwners));
        }
        
        UnsupportedRevokeBehavior unsupportedRevokeBehavior = null;
        if (unsupportedRevokeBehaviors.size() == 1) {
            unsupportedRevokeBehavior = unsupportedRevokeBehaviors.get(0);
        }
        if (unsupportedRevokeBehaviors.size() > 1) {
            throw new RbacDataError("Unsupported revoke behavior is declared more than once with values " + 
                unsupportedRevokeBehaviors.stream().map(Enum::name).toList());
        }
        
        return new PlaybookModel(newRoles, roleOwner, unsupportedRevokeBehavior);
    }
}
