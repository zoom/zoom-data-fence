package us.zoom.data.dfence.providers.snowflake.grant.builder;

import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.NoGrantBuilderError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.models.GrantValidationDefinition;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.sql.ObjectName;

import java.util.List;

@Slf4j
public abstract class SnowflakeGrantBuilder {

    public static SnowflakeGrantBuilder fromGrant(SnowflakeGrantModel grant) {
        return fromGrant(grant, new SnowflakeGrantBuilderOptions());
    }

    public static SnowflakeGrantBuilder fromGrant(SnowflakeGrantModel grant, Boolean suppressErrors) {
        SnowflakeGrantBuilderOptions options = new SnowflakeGrantBuilderOptions();
        options.setSuppressErrors(suppressErrors);
        return fromGrant(grant, options);
    }

    public static SnowflakeGrantBuilder fromGrant(
            SnowflakeGrantModel grant,
            SnowflakeGrantBuilderOptions options
    ) {
        SnowflakeObjectType objectType;
        try {
            objectType = SnowflakeObjectType.fromString(grant.grantedOn());

        } catch (IllegalArgumentException e) {
            String msg = String.format("Invalid object type %s", grant.grantedOn());
            NoGrantBuilderError err = new NoGrantBuilderError(msg, e);
            if (options.getSuppressErrors()) {
                log.error(msg, err);
                return null;
            }
            throw err;
        }
        // ToDo: We should really convert to object type once and use it in the rest of the app.
        SnowflakeGrantModel normalizedGrant = new SnowflakeGrantModel(
                grant.privilege(),
                objectType.name(),
                grant.name(),
                grant.grantedTo(),
                grant.granteeName(),
                grant.grantOption(),
                grant.future(),
                grant.all()
        );
        SnowflakeGrantBuilder[] snowflakeGrantBuilders = new SnowflakeGrantBuilder[]{
                new SnowflakeRoleGrantBuilder(normalizedGrant, options),
                new SnowflakePermissionGrantBuilder(normalizedGrant, options),
                new SnowflakeOwnershipGrantBuilder(normalizedGrant, options),
                new SnowflakeAccountPermissionGrantBuilder(normalizedGrant, options),
                new SnowflakeMLPermissionGrantBuilder(normalizedGrant, options),
                new SnowflakeCorePermissionGrantBuilder(normalizedGrant, options),
                new SnowflakeFuturePermissionGrantBuilder(normalizedGrant, options),
                new SnowflakeFutureMLPermissionGrantBuilder(normalizedGrant, options),
                new SnowflakeFutureCorePermissionGrantBuilder(normalizedGrant, options),
                new SnowflakeFutureOwnershipGrantBuilder(normalizedGrant, options),
                new SnowflakeDatabaseRoleGrantBuilder(normalizedGrant, options),
                new SnowflakeAllPermissionGrantBuilder(normalizedGrant, options),
                new SnowflakeAllOwnershipGrantBuilder(normalizedGrant, options),
                new SnowflakeAllMLPermissionGrantBuilder(normalizedGrant, options),
                new SnowflakeAllCorePermissionGrantBuilder(normalizedGrant, options),
                new SnowflakeUnsupportedOwnershipManagementGrantBuilder(normalizedGrant, options)};
        for (SnowflakeGrantBuilder builder : snowflakeGrantBuilders) {
            if (builder.isValid()) {
                return builder;
            }
        }
        String msg = String.format("No compatible grant builder found for grant %s", normalizedGrant);
        NoGrantBuilderError err = new NoGrantBuilderError(msg);
        if (options.getSuppressErrors()) {
            log.warn(msg, err);
            return null;
        }
        throw err;
    }

    abstract public List<GrantValidationDefinition> getValidCombinations();

    public Boolean isValid() {
        return !this.getGrant().all() && !this.getGrant().future() && this.getValidCombinations().stream()
                .anyMatch(x -> x.validate(getGrant()));
    }

    public abstract List<String> getGrantStatements();

    public abstract List<String> getRevokeStatements();

    public abstract SnowflakeGrantModel getGrant();

    public PlaybookPrivilegeGrant playbookPrivilegeGrant() {
        List<String> parts = ObjectName.splitObjectName(this.getGrant().name());
        SnowflakeObjectType snowflakeObjectType = SnowflakeObjectType.fromString(this.getGrant().grantedOn());
        String databaseName = null;
        String schemaName = null;
        String objectName = null;
        boolean includeFuture = false;
        if (snowflakeObjectType.getQualLevel() == 0) {
            objectName = parts.get(0);
        }
        if (snowflakeObjectType.getQualLevel() >= 1) {
            if (snowflakeObjectType.getQualLevel() == 1 && !snowflakeObjectType.equals(SnowflakeObjectType.DATABASE)) {
                objectName = parts.get(0);
            } else {
                databaseName = parts.get(0);
            }
        }
        if (getGrant().future()) {
            includeFuture = true;
            if (SnowflakePatterns.getFutureElementPattern().matcher(parts.get(1)).find()) {
                schemaName = "*";
            } else {
                schemaName = parts.get(1);
            }
            if (snowflakeObjectType != SnowflakeObjectType.SCHEMA) {
                objectName = "*";
            }
        } else {
            if (snowflakeObjectType.getQualLevel() >= 2) {
                schemaName = parts.get(1);
            }
            if (snowflakeObjectType.getQualLevel() == 3) {
                objectName = parts.get(2);
            }
        }
        return new PlaybookPrivilegeGrant(
                this.getGrant().grantedOn().toLowerCase(),
                objectName,
                schemaName,
                databaseName,
                List.of(this.getGrant().privilege().toLowerCase()),
                includeFuture,
                false);
    }

    public String getKey() {
        return String.join(
                "::",
                getGrant().privilege().toUpperCase(),
                SnowflakeObjectType.fromString(getGrant().grantedOn()).getAliasFor(),
                getGrant().name(),
                getGrant().grantedTo().toUpperCase(),
                getGrant().granteeName().toUpperCase(),
                getGrant().future().toString());
    }

}
