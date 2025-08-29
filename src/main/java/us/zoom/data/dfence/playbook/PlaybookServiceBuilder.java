package us.zoom.data.dfence.playbook;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.EnvironmentService;
import us.zoom.data.dfence.Provider;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookModel;
import us.zoom.data.dfence.profile.Profile;
import us.zoom.data.dfence.profile.model.ProfilesModel;

@Slf4j
public class PlaybookServiceBuilder {
  private final Map<String, String> variables = new HashMap<>();
  private List<String> playbookYamlStrings = List.of();
  private String profilesYamlString = "";
  private String profileName;
  private EnvironmentService environmentService = new EnvironmentService();
  private ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();

  public PlaybookServiceBuilder setEnvironmentService(EnvironmentService environmentService) {
    this.environmentService = environmentService;
    return this;
  }

  public List<String> getPlaybookYamlStrings() {
    return playbookYamlStrings;
  }

  public PlaybookServiceBuilder setPlaybookYamlStrings(List<String> playbookYamlStrings) {
    this.playbookYamlStrings = List.copyOf(playbookYamlStrings);
    return this;
  }

  public PlaybookServiceBuilder setPlaybookYamlStrings(File playbookFile) {
    if (!playbookFile.exists()) {
      throw new RbacDataError(String.format("File %s does not exist.", playbookFile));
    }
    if (playbookFile.isFile()) {
      log.debug("Using role file {}.", playbookFile);
      try {
        return this.setPlaybookYamlStrings(List.of(Files.readString(playbookFile.toPath())));
      } catch (IOException e) {
        throw new RbacDataError(
            String.format("Unable to parse playbook file %s. %s", playbookFile, e));
      }
    } else {
      log.debug("Using role directory {}.", playbookFile);
      FileSystem fileSystem = FileSystems.getDefault();
      PathMatcher pathMatcher = fileSystem.getPathMatcher("glob:**/*.{yml,yaml}");
      try {
        List<String> yamlStrings =
            Files.find(
                    playbookFile.toPath(),
                    20,
                    (filePath, fileAttr) ->
                        pathMatcher.matches(filePath) && fileAttr.isRegularFile())
                .map(
                    x -> {
                      try {
                        log.debug("Using file {}", x.getFileName());
                        return Files.readString(x);
                      } catch (IOException e) {
                        log.debug("Using role file {}", x);
                        throw new RbacDataError(String.format("Unable to read file %s", x), e);
                      }
                    })
                .toList();
        return this.setPlaybookYamlStrings(yamlStrings);
      } catch (IOException e) {
        throw new RbacDataError(e);
      }
    }
  }

  public String getProfilesYamlString() {
    return profilesYamlString;
  }

  public PlaybookServiceBuilder setProfilesYamlString(File profilesFile) {
    try {
      return this.setProfilesYamlString(Files.readString(profilesFile.toPath()));
    } catch (IOException e) {
      throw new RbacDataError(
          String.format("Unable to parse profiles file %s. %s", profilesFile, e));
    }
  }

  public PlaybookServiceBuilder setProfilesYamlString(String profilesYamlString) {
    this.profilesYamlString = profilesYamlString;
    return this;
  }

  public Map<String, String> getVariables() {
    return variables;
  }

  public PlaybookServiceBuilder putAllVariables(Map<String, String> variables) {
    this.variables.putAll(variables);
    return this;
  }

  public PlaybookServiceBuilder putAllVariablesFile(File variablesFile) throws RbacDataError {
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

  public PlaybookServiceBuilder putAllVariablesFromEnvironment() {
    Map<String, String> environment = environmentService.getEnv();
    this.putAllVariables(VariableParser.parseEnvironment(environment));
    return this;
  }

  public String getProfileName() {
    return profileName;
  }

  public PlaybookServiceBuilder setProfileName(String profileName) {
    this.profileName = profileName;
    return this;
  }

  public PlaybookService build() {
    log.debug("Building playbook service.");
    List<PlaybookModel> playbookModels =
        playbookYamlStrings.stream().map(x -> Playbook.parse(x, variables)).toList();
    log.debug("Using {} playbook models", playbookModels.size());
    PlaybookModel playbookModel = PlaybookModel.merge(playbookModels);
    playbookModel = Playbook.filterPlaybook(playbookModel);
    playbookModel = Playbook.propagateDefaults(playbookModel);
    ProfilesModel profilesModel = Profile.parseProfiles(profilesYamlString, variables);
    Provider provider = Profile.provider(profilesModel, profileName, forkJoinPool);
    return new PlaybookService(provider, playbookModel);
  }

  public void setForkJoinPool(ForkJoinPool forkJoinPool) {
    this.forkJoinPool = forkJoinPool;
  }
}
