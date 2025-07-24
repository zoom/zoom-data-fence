package us.zoom.security.dfence.cli;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import us.zoom.security.dfence.Provider;
import us.zoom.security.dfence.ProviderBuilder;

import java.io.File;

@Slf4j
public abstract class ProviderCommand extends ErrorHandledCallable {
    @CommandLine.Option(names = {"--var-file"}, description = "Variables file.")
    @Setter
    protected File varFile;
    @CommandLine.Option(
            names = {"--profile-file"}, description = "Connection profile file", defaultValue = "./profiles.yml")
    @Setter
    protected File profileFile;
    @CommandLine.Option(
            names = {"--profile", "-p"}, description = "Profile name to use for the connection.")
    protected String profile;
    @CommandLine.Option(
            names = {"--threads"}, description = "Number of threads to use for parallel operations. If not specified, the common ForkJoinPool will be used.")
    protected Integer threads;
    @Setter
    private ProviderBuilder providerBuilder = new ProviderBuilder();

    public Provider getProvider() {
        log.debug("Creating provider.");
        ProviderBuilder builder = providerBuilder.setProfilesYamlString(profileFile)
                .setProfileName(profile)
                .putAllVariablesFile(varFile)
                .putAllVariablesFromEnvironment();

        if (threads != null) {
            log.debug("Setting ForkJoinPool parallelism to {}", threads);
            builder.setForkJoinPoolParallelism(threads);
        }

        return builder.build();
    }
}
