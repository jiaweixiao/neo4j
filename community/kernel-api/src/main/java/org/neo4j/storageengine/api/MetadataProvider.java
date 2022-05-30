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
package org.neo4j.storageengine.api;

import java.io.Closeable;
import java.util.UUID;
import org.neo4j.io.pagecache.context.CursorContext;

/**
 * Provider for metadata that describes stores properties, ids, store level implementation details
 */
public interface MetadataProvider
        extends DatabaseIdStore,
                TransactionIdStore,
                LogVersionRepository,
                StoreIdProvider,
                KernelVersionRepository,
                Closeable {
    void regenerateMetadata(LegacyStoreId storeId, UUID externalStoreUUID, CursorContext cursorContext);
}