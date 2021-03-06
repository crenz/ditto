/*
 * Copyright (c) 2017 Bosch Software Innovations GmbH.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/index.php
 *
 * Contributors:
 *    Bosch Software Innovations GmbH - initial contribution
 */
package org.eclipse.ditto.services.utils.config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.eclipse.ditto.json.JsonFactory;
import org.eclipse.ditto.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;
import com.typesafe.config.ConfigValueType;

import akka.actor.Address;

/**
 * Utilities for Typesafe {@link Config}.
 */
public final class ConfigUtil {

    /**
     * The hosting environment config key.
     */
    public static final String HOSTING_ENVIRONMENT = "hosting.environment";

    /**
     * The default TCP port used if {@value #ENV_TCP_PORT} is not set.
     */
    private static final String DEFAULT_TCP_PORT = "2551";

    /**
     * Key of the uri for mongodb.
     */
    static final String AKKA_PERSISTENCE_MONGO_URI = "akka.contrib.persistence.mongodb.mongo.mongouri";

    private static final String HOSTING_ENVIRONMENT_DOCKER = "docker";
    private static final String HOSTING_ENVIRONMENT_FILEBASED = "filebased";

    private static final String ENV_HOSTING_ENVIRONMENT_FILE_LOCATION = "HOSTING_ENVIRONMENT_FILE_LOCATION";

    private static final String ENV_INSTANCE_INDEX = "INSTANCE_INDEX";
    private static final String ENV_CF_VCAP_SERVICES = "VCAP_SERVICES";
    private static final String ENV_CONFIG_ALIASES = "CONFIG_ALIASES";
    private static final String ENV_HOSTING_ENVIRONMENT = "HOSTING_ENVIRONMENT";
    private static final String ENV_VCAP_LOCATION = "VCAP_LOCATION";
    private static final String ENV_HOSTNAME = "HOSTNAME";
    private static final String ENV_BIND_TCP_PORT = "BIND_TCP_PORT";
    private static final String ENV_DNS_PREFIX = "DNS_PREFIX";
    private static final String ENV_TCP_PORT = "TCP_PORT";

    private static final String CLOUD_PROFILE_SUFFIX = "-cloud";
    private static final String DOCKER_PROFILE_SUFFIX = "-docker";
    private static final String DEV_PROFILE_SUFFIX = "-dev";

    private static final String VCAP_PREFIX = "vcap";
    private static final String VCAP_SERVICE_NAME = "name";
    private static final String DOT_SEPARATOR = ".";

    private static final String AKKA_CLUSTER_SEED_NODES = "akka.cluster.seed-nodes";
    private static final String AKKA_TCP_PROTOCOL = "akka.tcp";

    private static final String SECRETS_PATH = "/run/secrets";
    private static final String SECRETS_CONFIG_KEY = "secrets";

    private static final Map<String, String> DEFAULT_CONFIG_ALIASES = new HashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUtil.class);

    private ConfigUtil() {
        // no-op
    }

    /**
     * Determines the {@link Config} to use based on the environment we are running in. Applies a simple logic: <ul>
     * <li>When environment variable "{@value #ENV_CF_VCAP_SERVICES}" is present - CloudFoundry profile - loading
     * "resourceBasename{@value #CLOUD_PROFILE_SUFFIX}" Config</li> <li>When environment variable "{@value
     * #ENV_HOSTING_ENVIRONMENT}" is set to "{@value #HOSTING_ENVIRONMENT_DOCKER}" - Docker profile - loading
     * "resourceBasename{@value #DOCKER_PROFILE_SUFFIX}" Config</li> <li>When nothing else is detected - Development
     * profile - loading "resourceBasename{@value #DEV_PROFILE_SUFFIX}" Config</li> </ul>
     *
     * @param resourceBasename the resource in the classpath to load config from (e.g. "things").
     * @return the determined Config.
     */
    public static Config determineConfig(final String resourceBasename) {
        final Config config;

        final String environment = System.getenv(ENV_HOSTING_ENVIRONMENT);

        if (System.getenv(ENV_CF_VCAP_SERVICES) != null) {
            LOGGER.info("Running with 'CloudFoundry' config");
            final String vcapServices = System.getenv(ENV_CF_VCAP_SERVICES);
            config = ConfigFactory.parseResourcesAnySyntax(resourceBasename + CLOUD_PROFILE_SUFFIX)
                    .withValue(HOSTING_ENVIRONMENT, ConfigValueFactory.fromAnyRef(environment))
                    .withFallback(transformVcapStringToConfig(vcapServices));
        } else if (environment != null && environment.equalsIgnoreCase(HOSTING_ENVIRONMENT_DOCKER)) {
            LOGGER.info("Running with 'Docker' config");
            config = ConfigFactory.parseResourcesAnySyntax(resourceBasename + DOCKER_PROFILE_SUFFIX)
                    .withValue(HOSTING_ENVIRONMENT, ConfigValueFactory.fromAnyRef(environment));
        } else if (environment != null && environment.equalsIgnoreCase(HOSTING_ENVIRONMENT_FILEBASED)) {
            config = buildConfigForFilebased(null)
                    .withValue(HOSTING_ENVIRONMENT, ConfigValueFactory.fromAnyRef(environment));
        } else if (environment != null && !environment.isEmpty()) {
            config = buildConfigForFilebased(environment)
                    .withValue(HOSTING_ENVIRONMENT, ConfigValueFactory.fromAnyRef(environment));
        } else {
            LOGGER.info("Docker environment was not detected - assuming running in 'development' environment");
            config = ConfigFactory.parseResourcesAnySyntax(resourceBasename + DEV_PROFILE_SUFFIX)
                    .withValue(HOSTING_ENVIRONMENT, ConfigValueFactory.fromAnyRef(environment));
        }

        final Config loadedConfig = setAkkaPersistenceMongoUri(
                ConfigFactory.load(config.withFallback(ConfigFactory.parseResourcesAnySyntax(resourceBasename))));
        LOGGER.debug("Using config: {}", loadedConfig.root().render(ConfigRenderOptions.concise()));
        // resolve all properties with default config at the end:
        return loadedConfig;
    }

    private static Config buildConfigForFilebased(@Nullable final String resourceName) {
        final Config initialConfig;
        if (resourceName != null) {
            initialConfig = ConfigFactory.parseResourcesAnySyntax(resourceName);
        } else {
            final String configFileLocation = System.getenv(ENV_HOSTING_ENVIRONMENT_FILE_LOCATION);
            LOGGER.info("Running with 'Filebased backed' config for service from file '{}'", configFileLocation);
            initialConfig = ConfigFactory.parseFileAnySyntax(Paths.get(configFileLocation).toFile());
        }

        final String vcapLocation = System.getenv(ENV_VCAP_LOCATION);
        if (vcapLocation != null) {
            LOGGER.info("Reading vcap config from path '{}'", vcapLocation);

            final String vcap;
            try {
                vcap = new String(Files.readAllBytes(Paths.get(vcapLocation)));
            } catch (final IOException e) {
                LOGGER.error("Could not read vcap config from path '{}'", vcapLocation, e);
                throw new IllegalArgumentException("Could not read vcap config file", e);
            }

            // do the "-cloud" config parsing with fallback to vcap :
            final Config configFromVcap = transformVcapStringToConfig(vcap);
            final Config configFromSecrets = transformSecretsToConfig();

            return initialConfig
                    .withFallback(configFromVcap)
                    .withFallback(configFromSecrets);
        } else {
            return initialConfig
                    .withFallback(transformSecretsToConfig());
        }
    }

    /**
     * Determines from the passed {@code config} whether a manual {@code joinSeedNodes()} is required or not.
     *
     * @param config the config to determine the settings from.
     * @return whether manually joining cluster seed nodes is required or not.
     */
    public static boolean shouldManuallyJoinClusterSeedNodes(final Config config) {
        return !config.hasPath(AKKA_CLUSTER_SEED_NODES) || config.getStringList(AKKA_CLUSTER_SEED_NODES).isEmpty();
    }

    /**
     * Retrieves a list of cluster seed nodes by looking up the passed {@code clusterName} at the DNS.
     * Uses {@code seed-nodes} configured in the passed {@code config} as fallback if the hostname {@code clusterName}
     * could not be resolved at the DNS.
     *
     * @param clusterName the clusterName to use for looking up at the DNS
     * @param config the config as fallback for seed-nodes
     * @return a sorted List (by index id upwards) of resolved cluster seed nodes
     */
    public static List<Address> getClusterSeedNodesExceptOwn(final String clusterName, final Config config) {
        try {
            return getCanonicalHostnameStreamExceptOwn(clusterName)
                    .map(addressString -> buildAddress(clusterName, addressString)) //
                    .collect(Collectors.toList());
        } catch (final UnknownHostException e) {
            LOGGER.debug("UnknownHost: {}", e.getMessage(), e);
            return config.getStringList(AKKA_CLUSTER_SEED_NODES).stream() //
                    .map(ConfigUtil::convertAkkaTcpStringToAddress) //
                    .collect(Collectors.toList());
        }
    }

    /**
     * Retrieves a Stream of {@link InetAddress#getCanonicalHostName()}s by looking up the passed {@code hostName} at
     * the DNS via {@link InetAddress#getAllByName(String)}. This method calculates the canonical hostname which
     * currently (Docker Swarm Mode in Docker 1.13.1) looks like this:
     * <ul>
     * <li>THINGS_policies-leader.1.8dnykvh0b0xt2sv95ooerbkgi.THINGS_blue-net</li>
     * </ul>
     *
     * @param hostName the hostname to lookup at DNS
     * @return a Stream of canonical hostnames
     * @throws UnknownHostException when the passed {@code hostName} could not be resolved
     */
    private static Stream<String> getCanonicalHostnameStreamExceptOwn(final String hostName)
            throws UnknownHostException {
        final String hostNameToUse = Optional.ofNullable(System.getenv(ENV_DNS_PREFIX))
                .map(prefix -> prefix + hostName)
                .orElse(hostName);
        final InetAddress[] allKnownServiceAddresses = InetAddress.getAllByName(hostNameToUse);
        LOGGER.info("All known service addresses for service '{}': {}", hostNameToUse,
                Arrays.stream(allKnownServiceAddresses).map(InetAddress::getCanonicalHostName) //
                        .collect(Collectors.joining(",")));

        final String myHostname = System.getenv(ENV_HOSTNAME);

        return Arrays.stream(allKnownServiceAddresses) //
                .filter(addr -> !addr.getCanonicalHostName().equals(myHostname)) // exclude "my" address
                .map(InetAddress::getCanonicalHostName);
    }

    /**
     * Builds an {@link Address} from the passed in {@code clusterName} and {@code hostname} using the port from
     * environment variables or the default fallback port "{@value DEFAULT_TCP_PORT}".
     *
     * @param clusterName the clusterName to use for building the Address
     * @param hostname the hostname to use for building the Address
     * @return the built Address
     */
    public static Address buildAddress(final String clusterName, final String hostname) {
        final String tcpPort = Optional.ofNullable(System.getenv(ENV_BIND_TCP_PORT))
                .orElse(Optional.ofNullable(System.getenv(ENV_TCP_PORT)).orElse(DEFAULT_TCP_PORT));
        return buildAddress(clusterName, hostname, Integer.parseInt(tcpPort));
    }

    private static Address buildAddress(final String clusterName, final String hostname, final int port) {
        // in this case, we would get a problem with java.net.URI which does not parse URIs well with such hostnames
        if (hostname.contains("_")) {
            final String ip;
            try {
                // use the IP address instead:
                ip = InetAddress.getByName(hostname).getHostAddress();
            } catch (final UnknownHostException e) {
                throw new IllegalStateException("Could not resolve hostname '" + hostname + "'", e);
            }
            return Address.apply(AKKA_TCP_PROTOCOL, clusterName, ip, port);
        } else {
            return Address.apply(AKKA_TCP_PROTOCOL, clusterName, hostname, port);
        }
    }

    private static Address convertAkkaTcpStringToAddress(final String addressString) {
        final String[] split1 = addressString.split("://", 2);
        final String[] split2 = split1[1].split("@", 2);
        final String[] split3 = split2[1].split(":", 2);
        return buildAddress(split2[0], split3[0], Integer.parseInt(split3[1]));
    }

    /**
     * Returns the host name determined from the environment variable "HOSTNAME".
     *
     * @return the host name
     */
    public static String getHostNameFromEnv() {
        return Optional.ofNullable(System.getenv(ENV_HOSTNAME)).orElseGet(() -> {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (final UnknownHostException e) {
                throw new IllegalStateException("Could not retrieve 'localhost' address", e);
            }
        });
    }

    /**
     * Returns the host address determined from the passed in host name.
     *
     * @param hostName the name to determine the address from
     * @return the host address
     */
    private static String getHostAddressByName(final String hostName) {
        try {
            return InetAddress.getByName(hostName).getHostAddress();
        } catch (final UnknownHostException e) {
            throw new IllegalStateException("Could not resolve hostname '" + hostName + "'", e);
        }
    }

    /**
     * Returns the address of the host name which was determined by the env variable "HOSTNAME".
     *
     * @return the host address
     */
    public static String getLocalHostAddress() {
        return getHostAddressByName(getHostNameFromEnv());
    }

    /**
     * Returns the instance index integer.
     *
     * @return the instance index
     */
    public static Integer instanceIndex() {
        return Optional.ofNullable(System.getenv(ENV_INSTANCE_INDEX)).map(Integer::parseInt).orElse(-1);
    }

    /**
     * Calculates a unique suffix for instance-specific resources (e.g. response queues or kamon "Host" value) based on
     * the environment the service runs in. E.g.:
     * <ul>
     * <li>for Docker Swarm environment the suffix would be the Swarm Instance Index (starting from "1")</li>
     * <li>as fallback the "HOSTNAME" environment variable is used</li>
     * </ul>
     *
     * @return the calculated unique suffix.
     */
    public static String calculateInstanceUniqueSuffix() {
        return Optional.ofNullable(System.getenv(ENV_INSTANCE_INDEX)).orElseGet(ConfigUtil::getHostNameFromEnv);
    }

    private static Config transformVcapStringToConfig(final String vcapServices) {
        final List<Config> collect = ConfigFactory.parseString(vcapServices).entrySet().stream().map(entry -> {
            final String key = entry.getKey();
            final ConfigValue value = entry.getValue();
            if (value.valueType() == ConfigValueType.LIST) {
                // when parsing arrays
                return parseList(key, (ConfigList) value);
            } else {
                return ConfigFactory.empty().withValue(key, value);
            }
        }).collect(Collectors.toList());

        Config vcapConfig = ConfigFactory.empty();
        for (final Config c : collect) {
            vcapConfig = vcapConfig.withFallback(c);
        }

        vcapConfig = ConfigFactory.empty().withValue(VCAP_PREFIX, vcapConfig.root());

        final Config aliasConfig = determineAliasesConfig(vcapConfig);
        vcapConfig = vcapConfig.withFallback(aliasConfig);

        return vcapConfig;
    }

    private static Config parseList(final String key, final ConfigList list) {
        Config tmpCfg = ConfigFactory.empty();
        for (int i = 0; i < list.size(); i++) {
            final ConfigValue cVal = list.get(i);
            if (cVal instanceof ConfigObject) {
                // use the "name" as key in a "map" for "objects":
                final String name = ((ConfigObject) cVal).toConfig().getString(VCAP_SERVICE_NAME);
                tmpCfg = tmpCfg.withValue(key + DOT_SEPARATOR + name, cVal);
            } else {
                // use the index as key in a "map":
                tmpCfg = tmpCfg.withValue(key + DOT_SEPARATOR + i, cVal);
            }
        }
        return tmpCfg;
    }

    private static Config determineAliasesConfig(final Config vcapConfig) {
        final Map<String, String> aliases = getAliases();

        Config aliasesConfig = ConfigFactory.empty();
        for (final Map.Entry<String, String> entry : aliases.entrySet()) {
            final String alias = entry.getKey();
            final String originalKey = entry.getValue();

            ConfigValue originalValue = null;
            try {
                originalValue = vcapConfig.getValue(originalKey);
            } catch (final ConfigException e) {
                LOGGER.warn("Could not retrieve value for creating alias {} -> {}", originalKey, alias, e);
            }

            if (originalValue != null) {
                final Config aliasConfig = ConfigFactory.empty().withValue(alias, originalValue);
                aliasesConfig = aliasesConfig.withFallback(aliasConfig);
            }
        }

        return aliasesConfig;
    }

    private static Map<String, String> getAliases() {
        final String configAliasesStr = System.getenv(ENV_CONFIG_ALIASES);
        if (configAliasesStr == null || configAliasesStr.isEmpty()) {
            LOGGER.info("Environment variable {} is not defined or empty, using default config aliases: {}",
                    ENV_CONFIG_ALIASES, DEFAULT_CONFIG_ALIASES);
            return DEFAULT_CONFIG_ALIASES;
        }

        final Map<String, String> aliases = new HashMap<>();
        final JsonObject configAliasesJsonObj = JsonFactory.newObject(configAliasesStr);
        configAliasesJsonObj.stream().forEach(jsonField -> {
            final String alias = jsonField.getKeyName();
            final String originalPathStr = jsonField.getValue().asString();

            aliases.put(alias, originalPathStr);
        });

        LOGGER.info("Environment variable {} defines these config aliases: {}", ENV_CONFIG_ALIASES, aliases);

        return aliases;
    }

    private static Config transformSecretsToConfig() {
        try {
            final Map<String, String> secrets = Files.list(Paths.get(SECRETS_PATH))
                    .map(ConfigUtil::readSecretFromPath)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toMap(Secret::getKey, Secret::getValue));

            final Map<String, ConfigObject> config = new HashMap<>();
            config.put(SECRETS_CONFIG_KEY, ConfigValueFactory.fromMap(secrets));

            return ConfigFactory.parseMap(config);
        } catch (final IOException e) {
            LOGGER.warn("No secrets present at path '{}'", SECRETS_PATH, e);
        }

        return ConfigFactory.empty();
    }

    /**
     * Sets Akka persistence MongoDB URI from configurations in {@code services-utils-config.mongodb}.
     *
     * @param config The loaded config.
     * @return Config with akka persistence MongoDB URI set according to the options in the config.
     */
    private static Config setAkkaPersistenceMongoUri(final Config config) {
        if (MongoConfig.isUriDefined(config)) {
            final Map<String, String> akkaPersistenceMongoUri = new HashMap<>();
            akkaPersistenceMongoUri.put(AKKA_PERSISTENCE_MONGO_URI, MongoConfig.getMongoUri(config));
            return ConfigFactory.parseMap(akkaPersistenceMongoUri).withFallback(config);
        } else {
            return config;
        }
    }

    private static Optional<Secret> readSecretFromPath(final Path path) {
        try {
            final String secretName = path.getName(path.getNameCount() - 1).toString();
            final List<String> lines = Files.readAllLines(path);
            if (lines.size() < 1) {
                final String message = "Expected a secret but found no lines in file '{0}'.";
                throw new IOException(MessageFormat.format(message, path));
            }
            return Optional.of(new Secret(secretName, lines.get(0)));
        } catch (final IOException e) {
            LOGGER.warn("Could not read secret at path '{}'", path);
            return Optional.empty();
        }
    }

    private static final class Secret {

        private final String key;
        private final String value;

        Secret(final String key, final String value) {
            this.key = key;
            this.value = value;
        }

        String getKey() {
            return key;
        }

        String getValue() {
            return value;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final Secret secret = (Secret) o;
            return Objects.equals(key, secret.key) &&
                    Objects.equals(value, secret.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [" +
                    ", key=" + key +
                    ", value=" + value +
                    "]";
        }
    }
}
