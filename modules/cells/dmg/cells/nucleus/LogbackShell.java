package dmg.cells.nucleus;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import ch.qos.logback.classic.spi.ILoggingEvent;

import org.slf4j.LoggerFactory;

import dmg.util.Args;

public class LogbackShell
{
    private LoggerContext _context;

    public LogbackShell()
    {
        _context = (LoggerContext) LoggerFactory.getILoggerFactory();
    }

    private Logger getLogger(String name)
    {
        return name.equals("root")
            ? _context.getLogger(Logger.ROOT_LOGGER_NAME)
            : _context.getLogger(name);
    }

    private List<Logger> getLoggers()
    {
        return _context.getLoggerList();
    }

    private Map<String,Appender<ILoggingEvent>> getAppenders()
    {
        Map<String,Appender<ILoggingEvent>> appenders =
            new HashMap<String,Appender<ILoggingEvent>>();
        for (Logger logger: getLoggers()) {
            Iterator<Appender<ILoggingEvent>> i = logger.iteratorForAppenders();
            while (i.hasNext()) {
                Appender<ILoggingEvent> appender = i.next();
                appenders.put(appender.getName(), appender);
            }
        }
        return appenders;
    }

    private List<Appender<ILoggingEvent>> getAppenders(Logger logger)
    {
        Iterator<Appender<ILoggingEvent>> appenders =
            logger.iteratorForAppenders();
        List<Appender<ILoggingEvent>> result =
            new ArrayList<Appender<ILoggingEvent>>();
        while (appenders.hasNext()) {
            result.add(appenders.next());
        }
        return result;
    }

    private List<String> getNames(List<Appender<ILoggingEvent>> appenders)
    {
        List<String> result = new ArrayList<String>(appenders.size());
        for (Appender<ILoggingEvent> appender: appenders) {
            result.add(appender.getName());
        }
        return result;
    }

    private String toString(Level level)
    {
        return (level == null) ? "" : level.toString();
    }

    public final static String hh_log_logger_ls = "[-a]";
    public final static String fh_log_logger_ls =
        "Lists logger instances. Loggers that inherit all properties are\n" +
        "not listed unless the -a option is specified.";
    public String ac_log_logger_ls(Args args)
    {
        final String format = "%-5s %-30s %s\n";

        boolean all = args.hasOption("a");
        Formatter f = new Formatter();
        f.format(format, "Level", "Appenders", "Logger");
        f.format(format, "-----", "---------", "------");
        for (Logger logger: getLoggers()) {
            List<Appender<ILoggingEvent>> appenders = getAppenders(logger);
            boolean hasAppenders = !appenders.isEmpty();
            boolean isEndOfRoad = !logger.isAdditive();
            boolean hasLevel = (logger.getLevel() != null);
            boolean isRoot = (logger.getName().equals(Logger.ROOT_LOGGER_NAME));
            if (all || hasAppenders || isEndOfRoad || hasLevel || isRoot) {
                f.format(format,
                         toString(logger.getLevel()),
                         getNames(appenders),
                         logger.getName());
            }
        }
        return f.toString();
    }

    public final static String hh_log_logger_set =
        "<logger> OFF|ERROR|WARN|INFO|DEBUG|TRACE|ALL";
    public final static String fh_log_logger_set =
        "Sets log level of <logger>. Notice that the preferred method to\n" +
        "adjust log levels in dCache is to manipulate the appender log\n" +
        "levels through the 'log set' and 'log reset' commands.";
    public String ac_log_logger_set_$_2(Args args)
    {
        String name = args.argv(0);
        Level level = Level.valueOf(args.argv(1));
        Logger logger = getLogger(name);
        if (logger == null) {
            throw new IllegalArgumentException("Logger not found: " + name);
        }

        logger.setLevel(level);
        return "Log level of " + name + " set to " + level;
    }

    public final static String hh_log_logger_reset =
        "<logger>";
    public final static String fh_log_logger_reset =
        "Resets the log level of <logger>. The effective log level will be\n" +
        "inherited from the parent logger. Notice that the preferred method\n" +
        "to adjust log levels in dCache is to manipulate the appender log\n" +
        "levels through the 'log set' and 'log reset' commands.";
    public String ac_log_logger_reset_$_1(Args args)
    {
        String name = args.argv(0);
        Logger logger = getLogger(name);
        if (logger == null) {
            throw new IllegalArgumentException("Logger not found: " + name);
        }

        logger.setLevel(null);
        return "Log level of " + name + " was reset";
    }

    public final static String hh_log_attach =
        "<logger> <appender>";
    public final static String fh_log_attach =
        "Attach <logger> to output module <appender>.";
    public String ac_log_attach_$_2(Args args)
    {
        String name = args.argv(0);
        String appender = args.argv(1);
        Logger logger = getLogger(name);
        if (logger == null) {
            throw new IllegalArgumentException("Logger not found: " + name);
        }

        Appender<ILoggingEvent> app = getAppenders().get(appender);
        if (app == null) {
            throw new IllegalArgumentException("Appender not found: " + appender);
        }

        logger.addAppender(app);

        return name + " attached to " + appender;
    }

    public final static String hh_log_detach =
        "<logger> <appender>";
    public final static String fh_log_detach =
        "Detach <logger> from output module <appender>.";
    public String ac_log_detach_$_2(Args args)
    {
        String name = args.argv(0);
        String appender = args.argv(1);
        Logger logger = getLogger(name);
        if (logger == null) {
            throw new IllegalArgumentException("Logger not found: " + name);
        }

        logger.detachAppender(appender);

        return name + " detached from " + appender;
    }
}