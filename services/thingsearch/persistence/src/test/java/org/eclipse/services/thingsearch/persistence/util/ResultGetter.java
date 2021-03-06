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
package org.eclipse.services.thingsearch.persistence.util;

/**
 * Interface defining a getter.
 *
 * @param <T> the return type of the getter
 */
public interface ResultGetter<T> {

    /**
     * Get the result.
     *
     * @return the result
     */
    T get();
}
