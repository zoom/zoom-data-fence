package us.zoom.security.dfence;

import us.zoom.security.dfence.playbook.model.PlaybookModel;
import us.zoom.security.dfence.playbook.model.PlaybookRoleModel;

import java.util.List;
import java.util.regex.Pattern;

public interface Provider {

    List<CompiledChanges> compileChanges(PlaybookModel playbookModel, Boolean ignoreUnknownChanges);

    void applyPrivilegeChanges(List<CompiledChanges> compiledChanges);

    void applyRolesChanges(List<CompiledChanges> compiledChanges);

    List<PlaybookRoleModel> importRoles(List<String> roleNames);

    List<PlaybookRoleModel> importRolePatterns(List<Pattern> rolePatterns);

    List<String> listRoles();
}
