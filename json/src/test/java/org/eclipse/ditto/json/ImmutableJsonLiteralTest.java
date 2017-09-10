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
package org.eclipse.ditto.json;

import static org.eclipse.ditto.json.assertions.DittoJsonAssertions.assertThat;
import static org.mutabilitydetector.unittesting.MutabilityAssert.assertInstancesOf;
import static org.mutabilitydetector.unittesting.MutabilityMatchers.areImmutable;

import java.lang.ref.SoftReference;

import org.eclipse.ditto.json.assertions.DittoJsonAssertions;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit test for {@link ImmutableJsonLiteral}.
 */
public final class ImmutableJsonLiteralTest {


    @Test
    public void assertImmutability() {
        assertInstancesOf(ImmutableJsonLiteral.class, areImmutable());
    }


    @Test
    public void testHashCodeAndEquals() {
        final SoftReference<JsonValue> red = new SoftReference<>(ImmutableJsonLiteral.TRUE);
        final SoftReference<JsonValue> black = new SoftReference<>(ImmutableJsonLiteral.FALSE);

        EqualsVerifier.forClass(ImmutableJsonLiteral.class) //
                .withIgnoredFields("stringRepresentation") //
                .withRedefinedSuperclass() //
                .withPrefabValues(SoftReference.class, red, black) //
                .suppress(Warning.REFERENCE_EQUALITY) //
                .verify();
    }


    @Test
    public void trueBehavesAsExpected() {
        final JsonValue underTest = ImmutableJsonLiteral.TRUE;

        DittoJsonAssertions.assertThat(underTest).isNotArray();
        DittoJsonAssertions.assertThat(underTest).isBoolean();
        DittoJsonAssertions.assertThat(underTest).isNotNullLiteral();
        DittoJsonAssertions.assertThat(underTest).isNotNumber();
        DittoJsonAssertions.assertThat(underTest).isNotObject();
        DittoJsonAssertions.assertThat(underTest).isNotString();
        assertThat(underTest.toString()).isEqualTo("true");
    }


    @Test
    public void falseBehavesAsExpected() {
        final JsonValue underTest = ImmutableJsonLiteral.FALSE;

        DittoJsonAssertions.assertThat(underTest).isNotArray();
        DittoJsonAssertions.assertThat(underTest).isBoolean();
        DittoJsonAssertions.assertThat(underTest).isNotNullLiteral();
        DittoJsonAssertions.assertThat(underTest).isNotNumber();
        DittoJsonAssertions.assertThat(underTest).isNotObject();
        DittoJsonAssertions.assertThat(underTest).isNotString();
        assertThat(underTest.toString()).isEqualTo("false");
    }

}