package us.zoom.security.dfence.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import us.zoom.security.dfence.ChangesSummary;
import us.zoom.security.dfence.Mappers;
import us.zoom.security.dfence.exception.RbacDataError;
import us.zoom.security.dfence.playbook.PlaybookService;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

@CommandLine.Command(
        name = "apply", mixinStandardHelpOptions = true, showDefaultValues = true)
@Slf4j
public class ApplyCommand extends PlaybookCommand {

    private static final ObjectMapper yamlKebabObjectMapper = Mappers.yamlKebabObjectMapper();

    @CommandLine.Option(
            names = {"--auto-approve"}, description = "Do not prompt for approval.", defaultValue = "false")
    private Boolean autoApprove;

    @CommandLine.Option(
            names = {"--changes-file", "-c"}, description = "Existing compiled changes file to read output from.")
    private File changesFile;

    @CommandLine.Option(
            names = {"--ignore-unknown-grants"},
            description = "Ignore existing grants of an unknown type.",
            defaultValue = "false")
    @Setter
    private Boolean ignoreUnknownGrants;


    @Override
    public Integer unhandledCall() throws RbacDataError {
        PlaybookService service = getPlaybookService();
        ChangesSummary changes;
        if (changesFile != null) {
            log.info("Reading changes from file {}", changesFile);
            try {
                changes = yamlKebabObjectMapper.readValue(changesFile, ChangesSummary.class);
            } catch (IOException e) {
                throw new RbacDataError(
                        String.format("Unable to read changes file %s. %s", changesFile.toString(), e),
                        e);
            }
        } else {
            log.info("No changes file provided. Compiling changes.");
            changes = service.compileChanges(ignoreUnknownGrants);
        }
        String changesSummary = writeChanges(changes);
        System.out.println(changesSummary);
        if (changes.changes().size() > 0 && !autoApprove && changesFile == null) {
            Scanner cmdLineScanner = new Scanner(System.in);
            System.out.println("Approve changes? y/n");
            String response = cmdLineScanner.nextLine();
            if (!response.equalsIgnoreCase("y")) {
                return 128;
            }
        }
        if (changes.changes().size() > 0) {
            log.info("Applying {} changes", changes.changes().size());
            service.applyChanges(changes.changes());
        } else {
            log.info("No changes found. Nothing to do.");
        }
        return 0;
    }
}
