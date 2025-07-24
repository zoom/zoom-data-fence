package us.zoom.data.dfence;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.Encoder;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.encoder.LogstashEncoder;
import net.logstash.logback.fieldnames.LogstashFieldNames;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import us.zoom.data.dfence.cli.*;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.concurrent.Callable;

@Command(
        name = "rbac",
        description = "Database access control.",
        showDefaultValues = true,
        mixinStandardHelpOptions = true,
        subcommands = {
                ApplyCommand.class, CompileCommand.class, ImportCommand.class, ListRolesCommand.class})
@Slf4j
public class Main implements Callable<Integer> {
    private static final String packageName = "us.zoom.security.dfence";
    @Option(
            names = {"--log-format"},
            description = "Logging format. One of ${COMPLETION-CANDIDATES}.",
            defaultValue = "TEXT")
    private LogFormat logFormat;
    @Option(names = {"--log-level"},
            description = "Logging level", defaultValue = "INFO")
    private String logLevel;

    @Option(names = {"--root-log-level"}, description = "Root log level.", defaultValue = "WARN")
    private String rootLogLevel;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).setExecutionStrategy(new CommandLine.RunAll()).execute(args);
        System.exit(exitCode);
    }

    public void setUpLogging() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();
        Encoder<ILoggingEvent> encoder;
        switch (logFormat) {
            case JSON -> {
                LogstashEncoder logstashEncoder = new LogstashEncoder();
                LogstashFieldNames logstashFieldNames = new LogstashFieldNames();
                logstashFieldNames.setTimestamp("timestamp");
                logstashFieldNames.setVersion("[ignore]");
                logstashFieldNames.setLevelValue("[ignore]");
                logstashEncoder.setFieldNames(logstashFieldNames);
                encoder = logstashEncoder;
            }
            default -> {
                PatternLayoutEncoder patternLayoutEncoder = new PatternLayoutEncoder();
                patternLayoutEncoder.setPattern("%d{HH:mm:ss. SSS} [%thread] %-5level %logger{36} – %msg%n");
                encoder = patternLayoutEncoder;
            }
        }
        encoder.setContext(loggerContext);
        encoder.start();
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(loggerContext);
        consoleAppender.setEncoder(encoder);
        consoleAppender.setName("STDERR");
        consoleAppender.setTarget("System.err");
        consoleAppender.start();
        Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(consoleAppender);
        rootLogger.setLevel(Level.toLevel(rootLogLevel));
        Logger packageLogger = loggerContext.getLogger(packageName);
        packageLogger.setLevel(Level.toLevel(logLevel));
        log.debug("Log level set to {}.", logLevel);
        log.debug("Log format set to {}.", logFormat);
        log.debug("Root log level set to {}.", rootLogLevel);
    }

    @Override
    public Integer call() throws Exception {
        setUpLogging();
        return 0;
    }

}