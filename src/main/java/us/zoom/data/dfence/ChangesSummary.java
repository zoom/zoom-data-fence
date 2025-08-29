package us.zoom.data.dfence;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChangesSummary(Integer totalRoles, List<CompiledChanges> changes) {
  public ChangesSummary {
    changes = List.copyOf(changes);
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public Integer totalChanges() {
    return changes.size();
  }
}
