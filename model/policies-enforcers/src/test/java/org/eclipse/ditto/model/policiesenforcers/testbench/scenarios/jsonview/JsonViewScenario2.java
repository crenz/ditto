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
package org.eclipse.ditto.model.policiesenforcers.testbench.scenarios.jsonview;


import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.ditto.json.JsonFieldSelector;
import org.eclipse.ditto.model.policies.SubjectId;
import org.eclipse.ditto.model.policies.SubjectIssuer;
import org.eclipse.ditto.model.policiesenforcers.testbench.scenarios.Scenario;
import org.eclipse.ditto.model.policiesenforcers.testbench.scenarios.ScenarioSetup;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;


@State(Scope.Benchmark)
public class JsonViewScenario2 implements JsonViewScenario {

    private final ScenarioSetup setup;

    public JsonViewScenario2() {
        setup = Scenario.newScenarioSetup( //
                true, //
                "Subject has READ+WRITE granted on '/features'. "
                        + "Is able to READ '/'. Can see in JsonView: '/features'.", //
                getPolicy(), //
                Scenario.newAuthorizationContext(SUBJECT_FEATURES_READ_GRANTED), //
                "/", //
                THING, //
                THING.toJson(JsonFieldSelector.newInstance("/features")),
                Stream.of(
                        SubjectId.newInstance(SubjectIssuer.GOOGLE_URL, SUBJECT_ALL_GRANTED).toString())
                        .collect(Collectors.toSet()), //
                "READ");
    }

    @Override
    public ScenarioSetup getSetup() {
        return setup;
    }
}
