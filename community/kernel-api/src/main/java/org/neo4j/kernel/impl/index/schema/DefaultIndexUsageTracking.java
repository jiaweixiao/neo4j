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
package org.neo4j.kernel.impl.index.schema;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicLong;
import org.neo4j.kernel.api.index.IndexUsageStats;

public class DefaultIndexUsageTracking implements IndexUsageTracking {
    private final long trackedSince;
    private final AtomicLong readCount = new AtomicLong();
    private final AtomicLong lastRead = new AtomicLong();
    private final Clock clock;

    public DefaultIndexUsageTracking(Clock clock) {
        this.clock = clock;
        this.trackedSince = clock.millis();
    }

    @Override
    public IndexUsageTracker track() {
        return new DefaultIndexUsageTracker();
    }

    @Override
    public IndexUsageStats getAndReset() {
        long queryCount = this.readCount.get();
        this.readCount.addAndGet(-queryCount);
        return new IndexUsageStats(lastRead.get(), queryCount, trackedSince);
    }

    private void add(long queryCount, long lastTimeUsed) {
        this.readCount.addAndGet(queryCount);
        this.lastRead.getAndUpdate(operand -> Long.max(operand, lastTimeUsed));
    }

    private class DefaultIndexUsageTracker implements IndexUsageTracker {
        private long localLastRead;
        private long localReadCount;

        @Override
        public void queried() {
            localReadCount++;
            localLastRead = clock.millis();
        }

        @Override
        public void close() {
            add(localReadCount, localLastRead);
        }
    }
}
