package us.zoom.data.dfence;

import com.google.common.collect.ImmutableList;

import java.util.List;

public record CompiledChanges(
        String roleId, String roleName, List<List<String>> ownershipGrantStatements,
        List<String> roleCreationStatements, List<List<String>> roleGrantStatements) {
    public CompiledChanges {
        ownershipGrantStatements = ImmutableList.copyOf(ownershipGrantStatements);
        roleCreationStatements = ImmutableList.copyOf(roleCreationStatements);
        roleGrantStatements = ImmutableList.copyOf(roleGrantStatements);
    }

    public Boolean containsChanges() {
        return !ownershipGrantStatements.isEmpty() || !roleCreationStatements.isEmpty() || !roleGrantStatements.isEmpty();
    }
}
