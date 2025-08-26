package us.zoom.data.dfence;

import java.util.List;
import java.util.regex.Pattern;

import us.zoom.data.dfence.playbook.model.PlaybookModel;
import us.zoom.data.dfence.playbook.model.PlaybookRoleModel;

public interface Provider {

  List<CompiledChanges> compileChanges(PlaybookModel playbookModel, Boolean ignoreUnknownChanges);

  void applyPrivilegeChanges(List<CompiledChanges> compiledChanges);

  void applyRolesChanges(List<CompiledChanges> compiledChanges);

  List<PlaybookRoleModel> importRoles(List<String> roleNames);

  List<PlaybookRoleModel> importRolePatterns(List<Pattern> rolePatterns);

  List<String> listRoles();
}
