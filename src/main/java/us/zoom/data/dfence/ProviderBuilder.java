package us.zoom.data.dfence;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.VariableParser;
import us.zoom.data.dfence.profile.Profile;
import us.zoom.data.dfence.profile.model.ProfilesModel;

@Slf4j
public class ProviderBuilder {
  final Map<String, String> variables = new HashMap<>();
  String profilesYamlString = "";
  String profileName;
  EnvironmentService environmentService = new EnvironmentService();
  ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();

  public ProviderBuilder setEnvironmentService(EnvironmentService environmentService) {
    this.environmentService = environmentService;
    return this;
  }

  /**
   * Set the ForkJoinPool to use for parallel operations. If not set, the common ForkJoinPool will
   * be used.
   *
   * @param forkJoinPool the fork join pool to use
   * @return this builder
   */
  public ProviderBuilder setForkJoinPool(ForkJoinPool forkJoinPool) {
    this.forkJoinPool = forkJoinPool;
    return this;
  }

  /**
   * Set the number of threads to use in the ForkJoinPool. This creates a new ForkJoinPool with the
   * specified number of threads.
   *
   * @param parallelism the number of threads to use
   * @return this builder
   */
  public ProviderBuilder setForkJoinPoolParallelism(int parallelism) {
    this.forkJoinPool = new ForkJoinPool(parallelism);
    return this;
  }

  public String getProfilesYamlString() {
    return profilesYamlString;
  }

  public ProviderBuilder setProfilesYamlString(File profilesFile) {
    try {
      return this.setProfilesYamlString(Files.readString(profilesFile.toPath()));
    } catch (IOException e) {
      throw new RbacDataError(
          String.format("Unable to parse profiles file %s. %s", profilesFile, e));
    }
  }

  public ProviderBuilder setProfilesYamlString(String profilesYamlString) {
    this.profilesYamlString = profilesYamlString;
    return this;
  }

  public Map<String, String> getVariables() {
    return variables;
  }

  public ProviderBuilder putAllVariables(Map<String, String> variables) {
    this.variables.putAll(variables);
    return this;
  }

  public ProviderBuilder putAllVariablesFile(File variablesFile) throws RbacDataError {
    String variablesYamlString;
    if (variablesFile != null) {
      try {
        variablesYamlString = Files.readString(variablesFile.toPath());
      } catch (IOException e) {
        throw new RbacDataError(
            String.format("Unable to read variables file %s. %s", variablesFile, e));
      }
      try {
        this.putAllVariables(VariableParser.parseVariables(variablesYamlString));
      } catch (JsonProcessingException e) {
        throw new RbacDataError(
            String.format("Unable to parse yaml file %s as valid yaml. %s", variablesFile, e));
      }
    }
    return this;
  }

  public ProviderBuilder putAllVariablesFromEnvironment() {
    Map<String, String> environment = environmentService.getEnv();
    this.putAllVariables(VariableParser.parseEnvironment(environment));
    return this;
  }

  public String getProfileName() {
    return profileName;
  }

  public ProviderBuilder setProfileName(String profileName) {
    this.profileName = profileName;
    return this;
  }

  public Provider build() {
    log.debug("Building playbook service.");
    ProfilesModel profilesModel = Profile.parseProfiles(profilesYamlString, variables);
    return Profile.provider(profilesModel, profileName, forkJoinPool);
  }
}
