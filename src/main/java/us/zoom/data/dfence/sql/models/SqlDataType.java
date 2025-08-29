package us.zoom.data.dfence.sql.models;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SqlDataType {
  private final List<SqlDataType> columnTypes = new ArrayList<>();
  private final List<String> params = new ArrayList<>();
  private String typeName;

  public SqlDataType(String typeName, List<SqlDataType> columnTypes, List<String> params) {
    this.typeName = typeName;
    this.columnTypes.addAll(columnTypes);
    this.params.addAll(params);
  }

  public SqlDataType(String typeName) {
    this.typeName = typeName;
  }

  public String toNormalizedName() {
    StringBuilder builder = new StringBuilder();
    builder.append(typeName);
    if (!columnTypes.isEmpty()) {
      builder.append("(");
      List<String> columnNormalizedNames =
          columnTypes.stream().map(SqlDataType::toNormalizedName).toList();
      builder.append(String.join(", ", columnNormalizedNames));
      builder.append(")");
    }
    if (!params.isEmpty()) {
      builder.append("(");
      builder.append(String.join(",", params));
      builder.append(")");
    }
    return builder.toString();
  }
}
