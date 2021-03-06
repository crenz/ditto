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
package org.eclipse.ditto.signals.commands.things.query;

import static org.eclipse.ditto.model.base.common.ConditionChecker.checkNotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.eclipse.ditto.json.JsonFactory;
import org.eclipse.ditto.json.JsonField;
import org.eclipse.ditto.json.JsonFieldDefinition;
import org.eclipse.ditto.json.JsonFieldSelector;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.json.JsonObjectBuilder;
import org.eclipse.ditto.json.JsonPointer;
import org.eclipse.ditto.json.JsonValue;
import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.model.base.json.FieldType;
import org.eclipse.ditto.model.base.json.JsonSchemaVersion;
import org.eclipse.ditto.model.things.ThingIdValidator;
import org.eclipse.ditto.signals.base.WithFeatureId;
import org.eclipse.ditto.signals.commands.base.AbstractCommand;
import org.eclipse.ditto.signals.commands.base.CommandJsonDeserializer;

/**
 * This command retrieves a {@link org.eclipse.ditto.model.things.Feature}'s properties.
 */
@Immutable
public final class RetrieveFeatureProperties extends AbstractCommand<RetrieveFeatureProperties> implements
        ThingQueryCommand<RetrieveFeatureProperties>, WithFeatureId {

    /**
     * Name of the "Retrieve Feature Properties" command.
     */
    public static final String NAME = "retrieveFeatureProperties";

    /**
     * Type of this command.
     */
    public static final String TYPE = TYPE_PREFIX + NAME;

    static final JsonFieldDefinition JSON_FEATURE_ID =
            JsonFactory.newFieldDefinition("featureId", String.class, FieldType.REGULAR,
                    // available in schema versions:
                    JsonSchemaVersion.V_1, JsonSchemaVersion.V_2);

    static final JsonFieldDefinition JSON_SELECTED_FIELDS =
            JsonFactory.newFieldDefinition("selectedFields", String.class, FieldType.REGULAR,
                    // available in schema versions:
                    JsonSchemaVersion.V_1, JsonSchemaVersion.V_2);

    private final String thingId;
    private final String featureId;
    @Nullable private final JsonFieldSelector selectedFields;

    private RetrieveFeatureProperties(final String thingId,
            final String featureId,
            @Nullable final JsonFieldSelector selectedFields,
            final DittoHeaders dittoHeaders) {

        super(TYPE, dittoHeaders);
        ThingIdValidator.getInstance().accept(thingId, dittoHeaders);
        this.thingId = thingId;
        this.featureId = checkNotNull(featureId, "Feature ID");
        this.selectedFields = selectedFields;
    }

    /**
     * Returns a Command for retrieving a Feature's Properties on a Thing.
     *
     * @param thingId the {@code Thing}'s ID whose {@code Feature}'s Properties to retrieve.
     * @param featureId the {@code Feature}'s ID whose Properties to retrieve.
     * @param dittoHeaders the headers of the command.
     * @return a Command for retrieving the Properties of the specified Feature.
     * @throws NullPointerException if any argument but {@code thingId} is {@code null}.
     * @throws org.eclipse.ditto.model.things.ThingIdInvalidException if the parsed thing ID did not comply to {@link
     * org.eclipse.ditto.model.things.Thing#ID_REGEX}.
     */
    public static RetrieveFeatureProperties of(final String thingId, final String featureId,
            final DittoHeaders dittoHeaders) {

        return new RetrieveFeatureProperties(thingId, featureId, null, dittoHeaders);
    }

    /**
     * Returns a Command for retrieving a Feature's Properties on a Thing.
     *
     * @param thingId the {@code Thing}'s ID whose {@code Feature}'s Properties to retrieve.
     * @param featureId the {@code Feature}'s ID whose Properties to retrieve.
     * @param selectedFields defines the fields of the JSON representation of the Properties to retrieve.
     * @param dittoHeaders the headers of the command.
     * @return a Command for retrieving the Properties of the specified Feature.
     * @throws NullPointerException if {@code featureId} or {@code dittoHeaders} is {@code null}.
     * @throws org.eclipse.ditto.model.things.ThingIdInvalidException if the parsed thing ID did not comply to {@link
     * org.eclipse.ditto.model.things.Thing#ID_REGEX}.
     */
    public static RetrieveFeatureProperties of(final String thingId,
            final String featureId,
            @Nullable final JsonFieldSelector selectedFields,
            final DittoHeaders dittoHeaders) {

        return new RetrieveFeatureProperties(thingId, featureId, selectedFields, dittoHeaders);
    }

    /**
     * Creates a new {@code RetrieveFeatureProperties} from a JSON string.
     *
     * @param jsonString the JSON string of which the command is to be created.
     * @param dittoHeaders the headers of the command.
     * @return the command.
     * @throws NullPointerException if any argument is {@code null}.
     * @throws IllegalArgumentException if {@code jsonString} is empty.
     * @throws org.eclipse.ditto.json.JsonParseException if the passed in {@code jsonString} was not in the expected
     * format.
     * @throws org.eclipse.ditto.model.things.ThingIdInvalidException if the parsed thing ID did not comply to {@link
     * org.eclipse.ditto.model.things.Thing#ID_REGEX}.
     */
    public static RetrieveFeatureProperties fromJson(final String jsonString, final DittoHeaders dittoHeaders) {
        return fromJson(JsonFactory.newObject(jsonString), dittoHeaders);
    }

    /**
     * Creates a new {@code RetrieveFeatureProperties} from a JSON object.
     *
     * @param jsonObject the JSON object of which the command is to be created.
     * @param dittoHeaders the headers of the command.
     * @return the command.
     * @throws NullPointerException if any argument is {@code null}.
     * @throws org.eclipse.ditto.json.JsonParseException if the passed in {@code jsonObject} was not in the expected
     * format.
     * @throws org.eclipse.ditto.model.things.ThingIdInvalidException if the parsed thing ID did not comply to {@link
     * org.eclipse.ditto.model.things.Thing#ID_REGEX}.
     */
    public static RetrieveFeatureProperties fromJson(final JsonObject jsonObject, final DittoHeaders dittoHeaders) {
        return new CommandJsonDeserializer<RetrieveFeatureProperties>(TYPE, jsonObject).deserialize(
                jsonObjectReader -> {
                    final String thingId = jsonObjectReader.get(ThingQueryCommand.JsonFields.JSON_THING_ID);
                    final String extractedFeatureId = jsonObjectReader.get(JSON_FEATURE_ID);
                    final JsonFieldSelector extractedFieldSelector = jsonObject.getValue(JSON_SELECTED_FIELDS)
                            .filter(JsonValue::isString)
                            .map(JsonValue::asString)
                            .map(str -> JsonFactory.newFieldSelector(str,
                                    JsonFactory.newParseOptionsBuilder().withoutUrlDecoding().build()))
                            .orElse(null);
                    return of(thingId, extractedFeatureId, extractedFieldSelector, dittoHeaders);
                });
    }

    @Override
    public Optional<JsonFieldSelector> getSelectedFields() {
        return Optional.ofNullable(selectedFields);
    }

    @Override
    public String getThingId() {
        return thingId;
    }

    @Override
    public String getFeatureId() {
        return featureId;
    }

    @Override
    public JsonPointer getResourcePath() {
        final String path = "/features/" + featureId + "/properties";
        return JsonPointer.of(path);
    }

    @Override
    protected void appendPayload(final JsonObjectBuilder jsonObjectBuilder, final JsonSchemaVersion schemaVersion,
            final Predicate<JsonField> thePredicate) {

        final Predicate<JsonField> predicate = schemaVersion.and(thePredicate);
        jsonObjectBuilder.set(ThingQueryCommand.JsonFields.JSON_THING_ID, thingId, predicate);
        jsonObjectBuilder.set(JSON_FEATURE_ID, featureId, predicate);
        if (null != selectedFields) {
            jsonObjectBuilder.set(JSON_SELECTED_FIELDS, selectedFields.toString(), predicate);
        }
    }

    @Override
    public RetrieveFeatureProperties setDittoHeaders(final DittoHeaders dittoHeaders) {
        return new RetrieveFeatureProperties(thingId, featureId, selectedFields, dittoHeaders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), thingId, featureId, selectedFields);
    }

    @SuppressWarnings("squid:S1067")
    @Override
    public boolean equals(@Nullable final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final RetrieveFeatureProperties that = (RetrieveFeatureProperties) obj;
        return that.canEqual(this) && Objects.equals(thingId, that.thingId)
                && Objects.equals(featureId, that.featureId)
                && Objects.equals(selectedFields, that.selectedFields) && super.equals(that);
    }

    @Override
    protected boolean canEqual(final Object other) {
        return (other instanceof RetrieveFeatureProperties);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + super.toString() + ", thingId=" + thingId + ", featureId=" +
                featureId
                + ", selectedFields=" + selectedFields + "]";
    }

}
