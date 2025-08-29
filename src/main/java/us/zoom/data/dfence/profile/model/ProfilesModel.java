package us.zoom.data.dfence.profile.model;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

public record ProfilesModel(String defaultProfile, @NotEmpty Map<String, ProfileModel> profiles) {}
