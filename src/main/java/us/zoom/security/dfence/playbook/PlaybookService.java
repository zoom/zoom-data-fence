package us.zoom.security.dfence.playbook;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import us.zoom.security.dfence.ChangesSummary;
import us.zoom.security.dfence.CompiledChanges;
import us.zoom.security.dfence.Provider;
import us.zoom.security.dfence.playbook.model.PlaybookModel;

import java.util.List;

@Slf4j
public class PlaybookService {
    @Getter
    private final Provider provider;

    @Getter
    private final PlaybookModel playbookModel;

    public PlaybookService(Provider provider, PlaybookModel playbookModel) {
        this.provider = provider;
        this.playbookModel = playbookModel;
    }

    public ChangesSummary compileChanges(Boolean ignoreUnknownGrants) {
        log.info("Compiling changes.");
        return new ChangesSummary(
                playbookModel.roles().size(),
                this.getProvider().compileChanges(playbookModel, ignoreUnknownGrants));
    }

    public void applyChanges(List<CompiledChanges> compiledChanges) {
        log.info("Applying {} changes", compiledChanges.size());
        this.provider.applyRolesChanges(compiledChanges);
        this.provider.applyPrivilegeChanges(compiledChanges);
    }
}
