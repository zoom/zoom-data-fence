package us.zoom.data.dfence.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import us.zoom.data.dfence.Mappers;
import us.zoom.data.dfence.Provider;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.PlaybookImport;
import us.zoom.data.dfence.playbook.model.PlaybookModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@CommandLine.Command(
        name = "import-roles", showDefaultValues = true, description = "Import roles into an RBAC configuration")
@Slf4j
public class ImportCommand extends ProviderCommand {

    private final ObjectMapper objectMapper = Mappers.yamlKebabObjectMapper();

    @CommandLine.Parameters(index = "0..*", description = "Roles to import.")
    private List<String> roleNames;

    @CommandLine.Option(
            names = {"--out", "-o"}, description = "Output file to write output to.")
    @Setter
    private File outputFile;

    @CommandLine.Option(
            names = {"--patterns"}, description = "If true, then role names are regex patterns.")
    @Setter
    private Boolean patterns = false;

    @Override
    Integer unhandledCall() {
        Provider provider = getProvider();
        PlaybookModel playbook;
        List<Pattern> rolePatterns;
        if (this.patterns) {
            try {
                rolePatterns = roleNames.stream().map(Pattern::compile).toList();
            } catch (PatternSyntaxException e) {
                throw new RbacDataError(String.format("Invalid regex pattern found. %s", e));
            }
            playbook = PlaybookImport.importRolePatterns(rolePatterns, provider);
        } else {
            playbook = PlaybookImport.importRoles(roleNames, provider);
        }
        String playbookYaml;
        try {
            playbookYaml = objectMapper.writeValueAsString(playbook);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Generated playbook cannot be serialized to YAML.", e);
        }
        System.out.println(playbookYaml);
        if (outputFile != null) {
            try {
                log.debug("Writing new playbook file {}", outputFile.getAbsolutePath());
                Files.writeString(outputFile.toPath(), playbookYaml);
            } catch (IOException e) {
                throw new RbacDataError(String.format("Unable to write playbook to output file. %s", e));
            }
        }
        return 0;
    }
}
