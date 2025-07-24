package us.zoom.data.dfence.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import us.zoom.data.dfence.ChangesSummary;
import us.zoom.data.dfence.Mappers;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.PlaybookService;
import us.zoom.data.dfence.playbook.PlaybookServiceBuilder;

import java.io.File;
import java.util.concurrent.ForkJoinPool;

@Slf4j
public abstract class PlaybookCommand extends ProviderCommand {
    @CommandLine.Option(
            names = {"--output-format"},
            description = "Output format. One of ${COMPLETION-CANDIDATES}.",
            defaultValue = "YAML")
    @Setter
    protected OutputFormat outputFormat;

    @Setter
    private PlaybookServiceBuilder playbookServiceBuilder = new PlaybookServiceBuilder();
    @CommandLine.Parameters(
            index = "0",
            description = "Configuration file to deploy or a directory containing multiple files " + "ending in \"yaml\" or \"yml\".")
    @Setter
    private File file;


    public String writeChanges(ChangesSummary changes) {
        ObjectMapper objectMapper;
        switch (outputFormat) {
            case JSON -> {
                objectMapper = Mappers.jsonKebabObjectMapper();
            }
            default -> {
                objectMapper = Mappers.yamlKebabObjectMapper();
            }
        }
        try {
            return objectMapper.writeValueAsString(changes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to write changes to string.", e);
        }
    }

    public PlaybookService getPlaybookService() throws RbacDataError {
        log.debug("Creating playbook service.");
        PlaybookServiceBuilder builder = playbookServiceBuilder.setPlaybookYamlStrings(this.file).setProfilesYamlString(this.profileFile)
                .putAllVariablesFile(this.varFile).putAllVariablesFromEnvironment().setProfileName(this.profile);

        if (this.threads != null) {
            builder.setForkJoinPool(
                    new ForkJoinPool(threads)
            );
        }
        return builder.build();
    }
}
