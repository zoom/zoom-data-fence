package us.zoom.security.dfence.playbook;

import com.google.common.base.CaseFormat;
import us.zoom.security.dfence.Provider;
import us.zoom.security.dfence.playbook.model.PlaybookModel;
import us.zoom.security.dfence.playbook.model.PlaybookRoleModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class PlaybookImport {
    public static PlaybookModel importRoles(List<String> roleNames, Provider provider) {
        List<PlaybookRoleModel> roles = provider.importRoles(roleNames);
        return conditionImportedRoles(roles);
    }

    public static PlaybookModel importRolePatterns(List<Pattern> rolePatterns, Provider provider) {
        List<PlaybookRoleModel> roles = provider.importRolePatterns(rolePatterns);
        return conditionImportedRoles(roles);
    }

    private static PlaybookModel conditionImportedRoles(List<PlaybookRoleModel> roles) {
        List<PlaybookRoleModel> consolidatedGrantRoles = roles.stream().map(Playbook::consolidateRolePrivileges)
                .toList();
        Map<String, PlaybookRoleModel> roleMap = new HashMap<>();
        consolidatedGrantRoles.forEach(r -> roleMap.put(
                CaseFormat.LOWER_UNDERSCORE.to(
                        CaseFormat.LOWER_HYPHEN,
                        r.name().toLowerCase()), r));
        return new PlaybookModel(roleMap);
    }
}
