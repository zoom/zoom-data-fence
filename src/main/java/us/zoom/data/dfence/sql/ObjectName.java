package us.zoom.data.dfence.sql;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import us.zoom.data.dfence.exception.ObjectNameException;
import us.zoom.data.dfence.sql.error.ParserException;
import us.zoom.data.dfence.sql.error.RbacAntlrErrorListener;
import us.zoom.data.dfence.sql.models.SqlObject;
import us.zoom.data.dfence.sql.parser.*;
import us.zoom.data.dfence.sql.visitors.objectname.SqlObjectObjectNameVisitor;
import us.zoom.data.dfence.sql.visitors.showgrantsprocedurename.SqlObjectShowGrantsProcedureNameVisitor;

public class ObjectName {

  public static String normalizeObjectNamePart(String value) {
    return normalizeObjectName(value);
  }

  public static String quotedObjectNamePart(String value) {
    String normalized_part = normalizeObjectNamePart(value);
    if (value.startsWith("\"") && normalized_part.endsWith("\"")) {
      return normalized_part;
    }
    return "\"" + normalized_part + "\"";
  }

  public static String unquotedObjectNamePart(String value) {
    if (value.startsWith("\"") && value.endsWith("\"")) {
      return value.substring(1, value.length() - 1);
    }
    return value;
  }

  public static String quotedObjectName(String value) {
    if (value.isEmpty()) {
      return "";
    }
    SqlObject sqlObject = ObjectName.parseSqlObject(value);
    return sqlObject.quotedName();
  }

  public static SqlObject parseSqlObject(String objectName) {
    RbacAntlrErrorListener errorListener = new RbacAntlrErrorListener();
    ObjectNameLexer lexer = new ObjectNameLexer(CharStreams.fromString(objectName));
    lexer.addErrorListener(errorListener);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    ObjectNameParser parser = new ObjectNameParser(tokens);
    parser.addErrorListener(errorListener);
    SqlObjectObjectNameVisitor visitor = new SqlObjectObjectNameVisitor();
    try {
      ParseTree tree = parser.objectName();
      return visitor.visit(tree);
    } catch (ParserException e) {
      throw new ObjectNameException(
          String.format("Object %s is not a valid SQL object name. %s", objectName, e), e);
    }
  }

  public static SqlObject parseSqlObjectFromShowGrantsProcedureName(
      String showGrantsProcedureName) {
    SqlObject outerSqlObject = parseSqlObject(showGrantsProcedureName);
    if (outerSqlObject.getNames().size() != 3) {
      throw new ObjectNameException(
          String.format(
              "Procedure name %s has %s parts. 3 parts expected.",
              showGrantsProcedureName, outerSqlObject.getNames().size()));
    }
    String callableName = outerSqlObject.getNames().get(2);
    RbacAntlrErrorListener errorListener = new RbacAntlrErrorListener();
    ShowGrantsProcedureNameLexer lexer =
        new ShowGrantsProcedureNameLexer(CharStreams.fromString(callableName));
    lexer.addErrorListener(errorListener);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    ShowGrantsProcedureNameParser parser = new ShowGrantsProcedureNameParser(tokens);
    parser.addErrorListener(errorListener);
    ShowGrantsProcedureNameVisitor<SqlObject> visitor =
        new SqlObjectShowGrantsProcedureNameVisitor();
    try {
      ParseTree tree = parser.callableName();
      SqlObject procedureObject = visitor.visit(tree);
      String procedureName = procedureObject.getNames().get(0);
      procedureObject.getNames().clear();
      procedureObject.getNames().addAll(outerSqlObject.getNames().subList(0, 2));
      procedureObject.getNames().add(procedureName);
      return procedureObject;
    } catch (ParserException e) {
      throw new ObjectNameException(
          String.format(
              "Object %s is not a valid SQL show grants procedure callable name.", callableName),
          e);
    }
  }

  public static String normalizeObjectName(String input) {
    try {
      if (input.isEmpty()) {
        return "";
      }
      SqlObject sqlObject = ObjectName.parseSqlObject(input);
      return sqlObject.normalizedName();
    } catch (ObjectNameException e) {
      throw new ObjectNameException(String.format("Unable to normalize object name %s", input), e);
    }
  }

  public static String procedureArgumentsToTypes(String arguments) {
    if (!arguments.startsWith("(") && arguments.endsWith(")")) {
      throw new ObjectNameException("Arguments must be in parenthesis");
    }
    List<String> argumentBlocks =
        StringPattern.splitIgnore(arguments.substring(1, arguments.length() - 1), ',', '(', ')');
    String[] dataTypes = new String[argumentBlocks.size()];
    Pattern argumentBlockPattern = Pattern.compile("[A-Z0-9_]+(\\(.*\\))?$");
    for (int i = 0; i < argumentBlocks.size(); i++) {
      Matcher argumentBlockMatcher = argumentBlockPattern.matcher(argumentBlocks.get(i));
      if (argumentBlockMatcher.find()) {
        dataTypes[i] = argumentBlockMatcher.group(0);
      } else {
        throw new ObjectNameException(
            String.format(
                "Unable to find argument data type within argument %s", argumentBlocks.get(i)));
      }
    }
    return String.join(", ", dataTypes);
  }

  public static String procedureGrantNameToObjectName(String procedureName) {
    if (procedureName.isEmpty()) {
      return "";
    }
    SqlObject sqlObject = ObjectName.parseSqlObjectFromShowGrantsProcedureName(procedureName);
    return sqlObject.normalizedName();
  }

  public static List<String> splitObjectName(String value) {
    try {
      if (value.isEmpty()) {
        return List.of(value);
      }
      SqlObject sqlObject = ObjectName.parseSqlObject(value);
      List<String> names = sqlObject.getNames();
      if (sqlObject.isHasArguments()) {
        String arguments = sqlObject.normalizedArguments();
        names.set(names.size() - 1, names.get(names.size() - 1) + arguments);
      }
      return names;
    } catch (ObjectNameException e) {
      throw new ObjectNameException(String.format("Unable to split object name %s", value), e);
    }
  }

  public static String containerName(String objectName) {
    SqlObject sqlObject = ObjectName.parseSqlObject(objectName);
    return String.join(".", sqlObject.getNames().subList(0, sqlObject.getNames().size() - 1));
  }

  public static Integer qualLevel(String objectName) {
    if (objectName.isEmpty()) {
      return 0;
    }
    return splitObjectName(objectName).size();
  }

  public static Boolean equalObjectName(String qualName, String otherQualName) {
    if (qualName == null) {
      return otherQualName == null;
    }
    return quotedObjectName(qualName).equals(quotedObjectName(otherQualName));
  }
}
