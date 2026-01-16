package us.zoom.data.dfence.providers.snowflake.models;

import java.util.List;

public record PartitionedGrantStatements(
    List<List<String>> ownershipStatements,
    List<List<String>> nonOwnershipStatements) {}

