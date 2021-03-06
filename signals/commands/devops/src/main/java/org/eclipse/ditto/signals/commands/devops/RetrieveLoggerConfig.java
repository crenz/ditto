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
package org.eclipse.ditto.signals.commands.devops;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.concurrent.Immutable;

import org.eclipse.ditto.json.JsonArray;
import org.eclipse.ditto.json.JsonCollectors;
import org.eclipse.ditto.json.JsonFactory;
import org.eclipse.ditto.json.JsonField;
import org.eclipse.ditto.json.JsonFieldDefinition;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.json.JsonObjectBuilder;
import org.eclipse.ditto.json.JsonValue;
import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.model.base.json.FieldType;
import org.eclipse.ditto.model.base.json.JsonSchemaVersion;

/**
 * Command to retrieve the {@link org.eclipse.ditto.model.devops.LoggerConfig} for each configured Logger.
 */
@Immutable
public final class RetrieveLoggerConfig extends AbstractDevOpsCommand<RetrieveLoggerConfig> {

    /**
     * Name of the command.
     */
    public static final String NAME = "retrieveLoggerConfig";

    /**
     * Type of this command.
     */
    public static final String TYPE = TYPE_PREFIX + NAME;

    static final JsonFieldDefinition JSON_ALL_KNOWN_LOGGERS =
            JsonFactory.newFieldDefinition("allKnownLoggers", boolean.class, FieldType.REGULAR,
                    // available in schema versions:
                    JsonSchemaVersion.V_1, JsonSchemaVersion.V_2);

    static final JsonFieldDefinition JSON_SPECIFIC_LOGGERS =
            JsonFactory.newFieldDefinition("specificLoggers", JsonArray.class, FieldType.REGULAR,
                    // available in schema versions:
                    JsonSchemaVersion.V_1, JsonSchemaVersion.V_2);

    private final boolean allKnownLoggers;
    private final List<String> specificLoggers;

    private RetrieveLoggerConfig(final boolean allKnownLoggers, final List<String> specificLoggers,
            final DittoHeaders dittoHeaders) {
        super(TYPE, dittoHeaders);
        this.allKnownLoggers = allKnownLoggers;
        this.specificLoggers = Collections.unmodifiableList(new ArrayList<>(specificLoggers));
    }

    /**
     * Returns a new instance of {@code RetrieveLoggerConfig}.
     *
     * @param dittoHeaders the headers of the request.
     * @return a new RetrieveLoggerConfig command.
     */
    public static RetrieveLoggerConfig ofAllKnownLoggers(final DittoHeaders dittoHeaders) {
        return new RetrieveLoggerConfig(true, Collections.emptyList(), dittoHeaders);
    }

    /**
     * Returns a new instance of {@code RetrieveLoggerConfig}.
     *
     * @param dittoHeaders the headers of the request.
     * @param specificLoggers one or more loggers to be retrieved.
     * @return a new RetrieveLoggerConfig command.
     */
    public static RetrieveLoggerConfig of(final DittoHeaders dittoHeaders, final String... specificLoggers) {
        return new RetrieveLoggerConfig(false,
                specificLoggers == null ? Collections.emptyList() : Arrays.asList(specificLoggers), dittoHeaders);
    }

    /**
     * Returns a new instance of {@code RetrieveLoggerConfig}.
     *
     * @param dittoHeaders the headers of the request.
     * @param specificLoggers one or more loggers to be retrieved.
     * @return a new RetrieveLoggerConfig command.
     */
    public static RetrieveLoggerConfig of(final DittoHeaders dittoHeaders, final List<String> specificLoggers) {
        return new RetrieveLoggerConfig(false, specificLoggers, dittoHeaders);
    }

    /**
     * Creates a new {@code RetrieveLoggerConfig} from a JSON string.
     *
     * @param jsonString contains the data of the RetrieveLoggerConfig command.
     * @param dittoHeaders the headers of the request.
     * @return the RetrieveLoggerConfig command which is based on the data of {@code jsonString}.
     * @throws NullPointerException if {@code jsonString} is {@code null}.
     * @throws org.eclipse.ditto.json.JsonParseException if the passed in {@code jsonString} was not in the expected
     * format.
     */
    public static RetrieveLoggerConfig fromJson(final String jsonString, final DittoHeaders dittoHeaders) {
        return fromJson(JsonFactory.newObject(jsonString), dittoHeaders);
    }

    /**
     * Creates a new {@code RetrieveLoggerConfig} from a JSON object.
     *
     * @param jsonObject the JSON object of which the command is to be created.
     * @param dittoHeaders the headers of the command.
     * @return the command.
     * @throws NullPointerException if {@code jsonObject} is {@code null}.
     * @throws org.eclipse.ditto.json.JsonParseException if the passed in {@code jsonObject} was not in the expected
     * format.
     */
    public static RetrieveLoggerConfig fromJson(final JsonObject jsonObject, final DittoHeaders dittoHeaders) {
        return new DevOpsCommandJsonDeserializer<RetrieveLoggerConfig>(TYPE, jsonObject).deserialize(
                jsonObjectReader -> {
                    final boolean isAllKnownLoggers = jsonObjectReader.get(JSON_ALL_KNOWN_LOGGERS);

                    if (isAllKnownLoggers) {
                        return ofAllKnownLoggers(dittoHeaders);
                    } else {
                        final List<String> extractedSpecificLoggers =
                                jsonObjectReader.<JsonArray>get(JSON_SPECIFIC_LOGGERS) //
                                        .stream() //
                                        .filter(JsonValue::isString) //
                                        .map(JsonValue::asString) //
                                        .collect(Collectors.toList());
                        return of(dittoHeaders, extractedSpecificLoggers);
                    }
                });
    }

    @Override
    public RetrieveLoggerConfig setDittoHeaders(final DittoHeaders dittoHeaders) {
        return of(dittoHeaders, specificLoggers);
    }

    /**
     * Returns whether all known loggers to retrieve or not.
     *
     * @return whether all known loggers to retrieve or not.
     */
    public boolean isAllKnownLoggers() {
        return allKnownLoggers;
    }

    /**
     * Returns the specific loggers to retrieve.
     *
     * @return the specific loggers to retrieve.
     */
    public List<String> getSpecificLoggers() {
        return specificLoggers;
    }

    @Override
    protected void appendPayload(final JsonObjectBuilder jsonObjectBuilder, final JsonSchemaVersion schemaVersion,
            final Predicate<JsonField> thePredicate)

    {
        final Predicate<JsonField> predicate = schemaVersion.and(thePredicate);
        jsonObjectBuilder.set(JSON_ALL_KNOWN_LOGGERS, allKnownLoggers, predicate);

        if (specificLoggers.size() > 0) {
            jsonObjectBuilder.set(JSON_SPECIFIC_LOGGERS, specificLoggers.stream() //
                    .map(JsonFactory::newValue) //
                    .collect(JsonCollectors.valuesToArray()), predicate);
        }
    }

    @SuppressWarnings("squid:MethodCyclomaticComplexity")
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final RetrieveLoggerConfig that = (RetrieveLoggerConfig) o;
        return that.canEqual(this) && Objects.equals(allKnownLoggers, that.allKnownLoggers) && Objects
                .equals(specificLoggers, that.specificLoggers) && super.equals(that);
    }

    @Override
    protected boolean canEqual(final Object other) {
        return (other instanceof RetrieveLoggerConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), allKnownLoggers, specificLoggers);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + super.toString() + "allKnownLoggers=" + allKnownLoggers
                + ", specificLoggers=" + specificLoggers + "]";
    }
}
