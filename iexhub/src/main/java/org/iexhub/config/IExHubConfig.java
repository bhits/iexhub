package org.iexhub.config;

import org.apache.log4j.Logger;
import org.iexhub.exceptions.UnexpectedServerException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

public class IExHubConfig {
    public static final String IEXHUB_CONFIG_LOCATION_PROP_NAME = "iexhub.config.location";
    public static final String DEFAULT_IEXHUB_CONFIG_LOCATION = "/java/iexhub/config";

    public static final String IEXHUB_CONFIG_FILE_PROP_NAME = "iexhub.config.file";
    public static final String DEFAULT_IEXHUB_CONFIG_FILE = "IExHub.properties";

    /**
     * Configuration location is externally configurable using
     * <code>{@value IEXHUB_CONFIG_LOCATION_PROP_NAME}</code> and <code>{@value IEXHUB_CONFIG_FILE_PROP_NAME}</code>
     * system properties. The external configuration file will be loaded from
     * <code>${{@value IEXHUB_CONFIG_LOCATION_PROP_NAME}}/${{@value IEXHUB_CONFIG_FILE_PROP_NAME}}</code> file.
     * <br/>
     * <p>The default values are:<ul>
     * <li> <code>{@value IEXHUB_CONFIG_LOCATION_PROP_NAME}</code>: {@value DEFAULT_IEXHUB_CONFIG_LOCATION}
     * <li> <code>{@value IEXHUB_CONFIG_FILE_PROP_NAME}</code>: {@value DEFAULT_IEXHUB_CONFIG_FILE}
     *
     * @see IExHubConfig#DEFAULT_IEXHUB_CONFIG_LOCATION
     * @see IExHubConfig#DEFAULT_IEXHUB_CONFIG_FILE
     */
    public static final String CONFIG_LOCATION = Optional.ofNullable(System.getProperty(IEXHUB_CONFIG_LOCATION_PROP_NAME)).orElse(DEFAULT_IEXHUB_CONFIG_LOCATION);
    public static final String CONFIG_FILE = CONFIG_LOCATION + "/" + Optional.ofNullable(System.getProperty(IEXHUB_CONFIG_FILE_PROP_NAME)).orElse(DEFAULT_IEXHUB_CONFIG_FILE);
    private static final Logger logger = Logger.getLogger(IExHubConfig.class);

    private static final Properties iexhubProperties;

    static {
        Properties props;
        final Path path = Paths.get(CONFIG_FILE);
        if (Files.exists(path)) {
            logger.info("Found configuration file at " + path.toAbsolutePath().toString());
        } else {
            logger.info("Could not find the configuration file at " + path.toAbsolutePath().toString());
        }
        try (InputStream is = Files.newInputStream(path)) {
            props = new Properties();
            props.load(is);
        } catch (IOException e) {
            final String errorMessage = "Error encountered loading properties file, "
                    + CONFIG_FILE
                    + ", "
                    + e.getMessage();
            logger.error(errorMessage);
            throw new UnexpectedServerException(errorMessage);
        }
        iexhubProperties = props;
    }

    private IExHubConfig() {
    }

    public static String getProperty(String key) {
        return iexhubProperties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return Optional.ofNullable(iexhubProperties.getProperty(key)).orElse(defaultValue);
    }

    public static boolean getProperty(String key, boolean defaultValue) {
        return Optional.ofNullable(iexhubProperties.getProperty(key)).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    public static int getProperty(String key, int defaultValue) {
        return Optional.ofNullable(iexhubProperties.getProperty(key)).map(Integer::parseInt).orElse(defaultValue);
    }

    public static String getConfigLocation() {
        return CONFIG_LOCATION;
    }

    public static String getConfigLocationPath(String filename) {
        return getConfigLocation() + "/" + filename;
    }
}
