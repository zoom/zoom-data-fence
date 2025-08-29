package us.zoom.data.dfence.profile;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import us.zoom.data.dfence.Mappers;
import us.zoom.data.dfence.Provider;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.VariableParser;
import us.zoom.data.dfence.profile.model.ProfileModel;
import us.zoom.data.dfence.profile.model.ProfilesModel;
import us.zoom.data.dfence.providers.snowflake.*;
import us.zoom.data.dfence.providers.snowflake.informationschema.SnowflakeObjectsService;

public class Profile {

  private static final ObjectMapper yamlKebabObjectMapper = Mappers.yamlKebabObjectMapper();

  public static ProfilesModel parseProfiles(
      String profilesYamlString, Map<String, String> variables) throws RbacDataError {
    String profilesYamlStringPopulated =
        VariableParser.substituteVariables(profilesYamlString, variables);
    try {
      return yamlKebabObjectMapper.readValue(profilesYamlStringPopulated, ProfilesModel.class);
    } catch (JsonProcessingException e) {
      throw new RbacDataError(String.format("Unable to parse string as valid profile. %s", e));
    }
  }

  /**
   * Create a provider using the common ForkJoinPool.
   *
   * @param profilesModel the profiles model
   * @param profileName the profile name
   * @return the provider
   * @throws RbacDataError if the profile is invalid
   */
  public static Provider provider(ProfilesModel profilesModel, String profileName)
      throws RbacDataError {
    return provider(profilesModel, profileName, ForkJoinPool.commonPool());
  }

  /**
   * Create a provider using the specified ForkJoinPool.
   *
   * @param profilesModel the profiles model
   * @param profileName the profile name
   * @param forkJoinPool the fork join pool to use for parallel operations
   * @return the provider
   * @throws RbacDataError if the profile is invalid
   */
  public static Provider provider(
      ProfilesModel profilesModel, String profileName, ForkJoinPool forkJoinPool)
      throws RbacDataError {
    if (profileName == null) {
      profileName = profilesModel.defaultProfile();
    }
    if (profileName == null) {
      throw new RbacDataError("No profile name specified.");
    }
    if (!profilesModel.profiles().containsKey(profileName)) {
      throw new RbacDataError(String.format("Profile name %s not in profiles.", profileName));
    }
    ProfileModel profileModel = profilesModel.profiles().get(profileName);
    // Leave this as a switch so that we have a place for future providers.
    if (Objects.requireNonNull(profileModel.getProviderName())
        == ProfileModel.ProviderName.SNOWFLAKE) {
      SnowflakeProviderConfigModel config = profileModel.getConnection().getSnowflake();
      SnowflakeConnectionService snowflakeConnectionService =
          new SnowflakeConnectionService(config);
      SnowflakeStatementsService snowflakeStatementsService =
          new SnowflakeStatementsService(snowflakeConnectionService);
      SnowflakeGrantsService snowflakeGrantsService =
          new SnowflakeGrantsService(snowflakeConnectionService);
      SnowflakeObjectsService snowflakeObjectsService =
          new SnowflakeObjectsService(snowflakeConnectionService);
      return new SnowflakeProvider(
          snowflakeStatementsService,
          snowflakeGrantsService,
          snowflakeObjectsService,
          forkJoinPool);
    }
    // We should not ever get here.
    throw new RuntimeException("Unable to create provider.");
  }
}
