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
package org.eclipse.ditto.signals.commands.things.modify;

import static org.eclipse.ditto.model.base.common.ConditionChecker.checkNotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.eclipse.ditto.json.JsonFactory;
import org.eclipse.ditto.json.JsonField;
import org.eclipse.ditto.json.JsonFieldDefinition;
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
 * This command modifies a single Property of a {@link org.eclipse.ditto.model.things.Feature}'s properties.
 */
@Immutable
public final class ModifyFeatureProperty extends AbstractCommand<ModifyFeatureProperty> implements
        ThingModifyCommand<ModifyFeatureProperty>, WithFeatureId {

    /**
     * Name of the "Modify Feature Property" command.
     */
    public static final String NAME = "modifyFeatureProperty";

    /**
     * Type of this command.
     */
    public static final String TYPE = TYPE_PREFIX + NAME;

    static final JsonFieldDefinition JSON_FEATURE_ID =
            JsonFactory.newFieldDefinition("featureId", String.class, FieldType.REGULAR,
                    // available in schema versions:
                    JsonSchemaVersion.V_1, JsonSchemaVersion.V_2);

    static final JsonFieldDefinition JSON_PROPERTY =
            JsonFactory.newFieldDefinition("property", String.class, FieldType.REGULAR,
                    // available in schema versions:
                    JsonSchemaVersion.V_1, JsonSchemaVersion.V_2);

    static final JsonFieldDefinition JSON_PROPERTY_VALUE =
            JsonFactory.newFieldDefinition("value", JsonValue.class, FieldType.REGULAR,
                    // available in schema versions:
                    JsonSchemaVersion.V_1, JsonSchemaVersion.V_2);

    private final String thingId;
    private final String featureId;
    private final JsonPointer propertyPointer;
    private final JsonValue propertyValue;

    private ModifyFeatureProperty(final String thingId, final String featureId, final JsonPointer propertyPointer,
            final JsonValue propertyValue, final DittoHeaders dittoHeaders) {

        super(TYPE, dittoHeaders);
        ThingIdValidator.getInstance().accept(thingId, dittoHeaders);
        this.thingId = thingId;
        this.featureId = checkNotNull(featureId, "Feature ID");
        this.propertyPointer = checkNotNull(propertyPointer, "Property JsonPointer");
        this.propertyValue = checkNotNull(propertyValue, "Property Value");
    }

    /**
     * Returns a Command for modifying a Feature's Property on a Thing.
     *
     * @param thingId the {@code Thing}'s ID whose {@code Feature}'s Property to modify.
     * @param featureId the {@code Feature}'s ID whose Property to modify.
     * @param propertyJsonPointer the JSON pointer of the Property key to modify.
     * @param propertyValue the value of the Property to modify.
     * @param dittoHeaders the headers of the command.
     * @return a Command for modifying the provided Property.
     * @throws NullPointerException if any argument but {@code thingId} is {@code null}.
     * @throws org.eclipse.ditto.model.things.ThingIdInvalidException if the parsed thing ID did not comply to {@link
     * org.eclipse.ditto.model.things.Thing#ID_REGEX}.
     */
    public static ModifyFeatureProperty of(final String thingId, final String featureId,
            final JsonPointer propertyJsonPointer, final JsonValue propertyValue, final DittoHeaders dittoHeaders) {

        return new ModifyFeatureProperty(thingId, featureId, propertyJsonPointer, propertyValue, dittoHeaders);
    }

    /**
     * Creates a new {@code ModifyFeatureProperty} from a JSON string.
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
    public static ModifyFeatureProperty fromJson(final String jsonString, final DittoHeaders dittoHeaders) {
        return fromJson(JsonFactory.newObject(jsonString), dittoHeaders);
    }

    /**
     * Creates a new {@code ModifyFeatureProperty} from a JSON object.
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
    public static ModifyFeatureProperty fromJson(final JsonObject jsonObject, final DittoHeaders dittoHeaders) {
        return new CommandJsonDeserializer<ModifyFeatureProperty>(TYPE, jsonObject).deserialize(jsonObjectReader -> {
            final String thingId = jsonObjectReader.get(ThingModifyCommand.JsonFields.JSON_THING_ID);
            final String extractedFeatureId = jsonObjectReader.get(JSON_FEATURE_ID);
            final String pointerString = jsonObjectReader.get(JSON_PROPERTY);
            final JsonPointer extractedPointer = JsonFactory.newPointer(pointerString);
            final JsonValue extractedValue = jsonObjectReader.get(JSON_PROPERTY_VALUE);
            return of(thingId, extractedFeatureId, extractedPointer, extractedValue, dittoHeaders);
        });
    }

    /**
     * Returns the JSON pointer of the Property to modify.
     *
     * @return the JSON pointer.
     */
    public JsonPointer getPropertyPointer() {
        return propertyPointer;
    }

    /**
     * Returns the value of the Property to modify.
     *
     * @return the value.
     */
    public JsonValue getPropertyValue() {
        return propertyValue;
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
    public Optional<JsonValue> getEntity(final JsonSchemaVersion schemaVersion) {
        return Optional.of(propertyValue);
    }

    @Override
    public JsonPointer getResourcePath() {
        final String path = "/features/" + featureId + "/properties" + propertyPointer;
        return JsonPointer.of(path);
    }

    @Override
    protected void appendPayload(final JsonObjectBuilder jsonObjectBuilder, final JsonSchemaVersion schemaVersion,
            final Predicate<JsonField> thePredicate) {
        final Predicate<JsonField> predicate = schemaVersion.and(thePredicate);
        jsonObjectBuilder.set(ThingModifyCommand.JsonFields.JSON_THING_ID, thingId, predicate);
        jsonObjectBuilder.set(JSON_FEATURE_ID, featureId, predicate);
        jsonObjectBuilder.set(JSON_PROPERTY, propertyPointer.toString(), predicate);
        jsonObjectBuilder.set(JSON_PROPERTY_VALUE, propertyValue, predicate);
    }

    @Override
    public ModifyFeatureProperty setDittoHeaders(final DittoHeaders dittoHeaders) {
        return of(thingId, featureId, propertyPointer, propertyValue, dittoHeaders);
    }

    @Override
    public boolean changesAuthorization() {
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), thingId, featureId, propertyPointer, propertyValue);
    }

    @SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S1067", "OverlyComplexMethod"})
    @Override
    public boolean equals(@Nullable final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ModifyFeatureProperty that = (ModifyFeatureProperty) obj;
        return that.canEqual(this) && Objects.equals(thingId, that.thingId) && Objects.equals(featureId, that.featureId)
                && Objects.equals(propertyPointer, that.propertyPointer) &&
                Objects.equals(propertyValue, that.propertyValue)
                && super.equals(that);
    }

    @Override
    protected boolean canEqual(final Object other) {
        return (other instanceof ModifyFeatureProperty);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + super.toString() + ", thingId=" + thingId + ", featureId=" +
                featureId
                + ", propertyPointer=" + propertyPointer + ", propertyValue=" + propertyValue + "]";
    }

}
