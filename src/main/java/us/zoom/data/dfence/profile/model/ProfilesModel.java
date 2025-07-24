package us.zoom.data.dfence.profile.model;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record ProfilesModel(String defaultProfile, @NotEmpty Map<String, ProfileModel> profiles) {
}
