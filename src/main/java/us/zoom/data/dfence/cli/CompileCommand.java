package us.zoom.data.dfence.cli;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import us.zoom.data.dfence.ChangesSummary;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.PlaybookService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@CommandLine.Command(
        name = "compile", description = "Compile changes.", showDefaultValues = true, mixinStandardHelpOptions = true)
@Slf4j
public class CompileCommand extends PlaybookCommand {

    @CommandLine.Option(
            names = {"--out", "-o"}, description = "Output file to write output to.")
    @Setter
    private File outputFile;

    @CommandLine.Option(
            names = {"--ignore-unknown-grants"},
            description = "Ignore existing grants of an unknown type.",
            defaultValue = "false")
    @Setter
    private Boolean ignoreUnknownGrants;

    @CommandLine.Option(
            names = {"--skip-consistency-check"},
            description = "Skip grant-revoke consistency verification. "
                + "By default, consistency check is always enabled and will abort if divergence is detected. "
                + "Use this flag only if you need to bypass the check (NOT RECOMMENDED).",
            defaultValue = "false")
    @Setter
    private Boolean skipConsistencyCheck;

    @Override
    public Integer unhandledCall() {
        PlaybookService service = getPlaybookService();
        Boolean enableGrantRevokeConsistencyCheck = !skipConsistencyCheck;
        ChangesSummary changes = service.compileChanges(ignoreUnknownGrants, enableGrantRevokeConsistencyCheck);
        String changesOutput = writeChanges(changes);
        System.out.println(changesOutput);
        if (outputFile != null) {
            try {
                log.debug("Writing changes to output file {}", outputFile.getAbsolutePath());
                Files.writeString(outputFile.toPath(), changesOutput);
            } catch (IOException e) {
                throw new RbacDataError(String.format("Unable to write changes to output file. %s", e));
            }
        }
        return 0;
    }
}
