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
package org.eclipse.ditto.model.things;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mutabilitydetector.unittesting.AllowedReason.provided;
import static org.mutabilitydetector.unittesting.MutabilityAssert.assertInstancesOf;
import static org.mutabilitydetector.unittesting.MutabilityMatchers.areImmutable;

import org.eclipse.ditto.json.JsonObject;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit test for {@link NullFeatureProperties}.
 */
public final class NullFeaturePropertiesTest {


    @Test
    public void testHashCodeAndEquals() {
        EqualsVerifier.forClass(NullFeatureProperties.class)
                .usingGetClass()
                .verify();
    }


    @Test
    public void assertImmutability() {
        assertInstancesOf(NullFeatureProperties.class, areImmutable(), provided(JsonObject.class).isAlsoImmutable());
    }

    @Test
    public void createInstanceReturnsTheExpectedJson() {
        final FeatureProperties properties = NullFeatureProperties.newInstance();

        assertThat(properties.toJsonString()).isEqualTo("null");
    }

}
