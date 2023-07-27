/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.internal.kernel.api.helpers.traversal.productgraph;

import java.util.List;

final class ListCursor<T> implements SourceCursor<List<T>, T> {
    private List<T> list;
    private int index = -1;

    public void setSource(List<T> list) {
        this.list = list;
        this.index = -1;
    }

    public boolean next() {
        return ++this.index < this.list.size();
    }

    public T current() {
        return this.list.get(this.index);
    }

    public void reset() {
        this.index = -1;
    }

    public void close() {
        this.list = null;
    }
}