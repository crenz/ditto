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
package org.eclipse.ditto.model.thingsearchparser.predicates.ast;

import static java.util.Objects.requireNonNull;

/**
 * Implements logical nodes like AND or OR. A logical node has a name and several children to which the logic of this
 * node has to be applied.
 */
public final class LogicalNode extends SuperNode {

    private final String name;
    private final Type type;

    /**
     * Constructor. Creates a new node with the given name.
     *
     * @param name name of this logical node.
     */
    public LogicalNode(final String name) {
        super();
        this.name = requireNonNull(name);
        this.type = Type.valueOf(name);
    }

    /**
     * Constructor. Creates a new node with the given type.
     *
     * @param type type of this logical node.
     */
    public LogicalNode(final Type type) {
        super();
        this.type = type;
        this.name = type.getName();
    }

    /**
     * Retrieve the name of this logical node.
     *
     * @return the name of the logical node.
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieve the {@link Type} of this logical node.
     *
     * @return the type of the logical node.
     */
    public Type getType() {
        return type;
    }

    @Override
    public void accept(final PredicateVisitor predicateVisitor) {
        predicateVisitor.visit(this);
    }

    @Override
    public String toString() {
        return "LogicalNode [name=" + name + ", type=" + type + ", children=" + getChildren() + "]";
    }

    // CS:OFF generated
    @SuppressWarnings("squid:S109")
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = (prime * result) + ((name == null) ? 0 : name.hashCode());
        result = (prime * result) + ((type == null) ? 0 : type.hashCode());
        return result;
    } // CS:ON hashCode()

    // CS:OFF generated
    @SuppressWarnings("squid:MethodCyclomaticComplexity")
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LogicalNode other = (LogicalNode) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (type != other.type) {
            return false;
        }
        return true;
    } // CS:ON equals(Object obj)

    /**
     * Defines the possible types that a {@link LogicalNode} can have.
     */
    public enum Type {
        /**
         * Represents a logical AND criteria.
         */
        and("and"),

        /**
         * Represents a logical OR criteria.
         */
        or("or"),

        /**
         * Represents a logical NOT criteria.
         */
        not("not");

        private final String name;

        Type(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}
