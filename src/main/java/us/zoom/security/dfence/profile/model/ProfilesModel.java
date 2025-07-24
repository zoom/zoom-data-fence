package us.zoom.security.dfence.profile.model;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record ProfilesModel(String defaultProfile, @NotEmpty Map<String, ProfileModel> profiles) {
}
