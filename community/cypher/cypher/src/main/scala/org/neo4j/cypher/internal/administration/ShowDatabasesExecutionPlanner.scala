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
package org.neo4j.cypher.internal.administration

import org.neo4j.common.DependencyResolver
import org.neo4j.cypher.internal.AdministrationCommandRuntime.internalKey
import org.neo4j.cypher.internal.AdministrationShowCommandUtils
import org.neo4j.cypher.internal.ExecutionEngine
import org.neo4j.cypher.internal.ExecutionPlan
import org.neo4j.cypher.internal.ExistingDataOption
import org.neo4j.cypher.internal.ExistingSeedInstanceOption
import org.neo4j.cypher.internal.LogEnrichmentOption
import org.neo4j.cypher.internal.SeedConfigOption
import org.neo4j.cypher.internal.SeedCredentialsOption
import org.neo4j.cypher.internal.SeedURIOption
import org.neo4j.cypher.internal.administration.ShowDatabaseExecutionPlanner.accessibleDbsKey
import org.neo4j.cypher.internal.ast.DatabaseScope
import org.neo4j.cypher.internal.ast.Return
import org.neo4j.cypher.internal.ast.ShowDatabase.ACCESS_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.ADDRESS_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.ALIASES_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.CONSTITUENTS_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.CREATION_TIME_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.CURRENT_PRIMARIES_COUNT_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.CURRENT_SECONDARIES_COUNT_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.CURRENT_STATUS_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.DATABASE_ID_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.DEFAULT_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.HOME_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.LAST_COMMITTED_TX_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.LAST_START_TIME_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.LAST_STOP_TIME_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.NAME_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.OPTIONS_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.REPLICATION_LAG_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.REQUESTED_PRIMARIES_COUNT_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.REQUESTED_SECONDARIES_COUNT_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.REQUESTED_STATUS_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.ROLE_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.SERVER_ID_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.STATUS_MSG_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.STORE_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.WRITER_COL
import org.neo4j.cypher.internal.ast.Yield
import org.neo4j.cypher.internal.procs.SystemCommandExecutionPlan
import org.neo4j.dbms.api.DatabaseManagementService
import org.neo4j.dbms.database.DatabaseInfoService
import org.neo4j.dbms.database.ExtendedDatabaseInfo
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.COMPOSITE_DATABASE
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DATABASE
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DATABASE_CREATED_AT_PROPERTY
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DATABASE_DEFAULT_PROPERTY
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DATABASE_DESIGNATED_SEEDER_PROPERTY
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DATABASE_LABEL
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DATABASE_LOG_ENRICHMENT_PROPERTY
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DATABASE_NAME
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DATABASE_NAME_PROPERTY
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DATABASE_PRIMARIES_PROPERTY
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DATABASE_SECONDARIES_PROPERTY
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DATABASE_SEED_CONFIG_PROPERTY
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DATABASE_SEED_CREDENTIALS_ENCRYPTED_PROPERTY
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DATABASE_SEED_URI_PROPERTY
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DATABASE_STARTED_AT_PROPERTY
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DATABASE_STATUS_PROPERTY
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DATABASE_STOPPED_AT_PROPERTY
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DEFAULT_NAMESPACE
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DISPLAY_NAME_PROPERTY
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.NAMESPACE_PROPERTY
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.NAME_PROPERTY
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.TARGETS
import org.neo4j.internal.kernel.api.security.SecurityAuthorizationHandler
import org.neo4j.kernel.database.DatabaseReferenceRepository
import org.neo4j.kernel.database.DefaultDatabaseResolver
import org.neo4j.values.virtual.VirtualValues

case class ShowDatabasesExecutionPlanner(
  resolver: DependencyResolver,
  normalExecutionEngine: ExecutionEngine,
  securityAuthorizationHandler: SecurityAuthorizationHandler
)(implicit extendedDatabaseInfoMapper: DatabaseInfoMapper[ExtendedDatabaseInfo]) {

  private val OPTIONS_TX_LOG_ENRICHMENT_KEY = LogEnrichmentOption.KEY
  private val OPTIONS_EXISTING_DATA_KEY = ExistingDataOption.KEY
  private val OPTIONS_SEED_URI_KEY = SeedURIOption.KEY
  private val OPTIONS_SEED_CONFIG_KEY = SeedConfigOption.KEY
  private val OPTIONS_SEED_CREDENTIALS_KEY = SeedCredentialsOption.KEY
  private val OPTIONS_SEED_INSTANCE_KEY = ExistingSeedInstanceOption.KEY

  private val dbms = resolver.resolveDependency(classOf[DatabaseManagementService])
  private val defaultDatabaseResolver = resolver.resolveDependency(classOf[DefaultDatabaseResolver])
  private val infoService = resolver.resolveDependency(classOf[DatabaseInfoService])
  private val referenceResolver = resolver.resolveDependency(classOf[DatabaseReferenceRepository])

  def planShowDatabases(
    scope: DatabaseScope,
    verbose: Boolean,
    symbols: List[String],
    yields: Option[Yield],
    returns: Option[Return]
  ): ExecutionPlan = {
    val usernameKey = internalKey("username")
    val optionsKey = internalKey("options")
    val isCompositeKey = internalKey("isComposite")

    def optionsOutputMap(keys: Set[String], mapName: String): String = {

      /** For a set of keys [A, B] that might be found in the map [mapName] produce:
       * CASE
       *    WHEN mapName.A IS NULL AND mapName.B IS NULL THEN {} 
       *    WHEN mapName.A IS NULL THEN {B: mapName.B}
       *    WHEN mapName.B IS NULL THEN {A: mapName.A}
       * ELSE mapName
       */
      val whens: Seq[String] = keys.subsets()
        .filter(_.nonEmpty)
        // Sort so that stricter predicates occur before less strict predicates
        .toList.sortBy(_.size).reverse
        .map(predicateKeys => {
          val predicates = predicateKeys.map(key => s"$mapName.$key IS NULL").toList.sorted

          val remainingKeys = keys -- predicateKeys
          val entries = remainingKeys.map(key => s"$key: $mapName.$key").toList.sorted
          val filteredMap = s"{${entries.mkString(", ")}}"

          s"WHEN ${predicates.mkString(" AND ")} THEN $filteredMap"
        })

      s"CASE ${whens.mkString(java.lang.System.lineSeparator())} ELSE $mapName END"
    }

    val optionsMap = optionsOutputMap(
      Set(
        OPTIONS_EXISTING_DATA_KEY,
        OPTIONS_SEED_URI_KEY,
        OPTIONS_SEED_CONFIG_KEY,
        OPTIONS_SEED_CREDENTIALS_KEY,
        OPTIONS_SEED_INSTANCE_KEY,
        OPTIONS_TX_LOG_ENRICHMENT_KEY
      ),
      optionsKey
    )

    val verboseColumns =
      if (verbose) {
        s""", props.$DATABASE_ID_COL as $DATABASE_ID_COL,
           |props.$CURRENT_PRIMARIES_COUNT_COL as $CURRENT_PRIMARIES_COUNT_COL,
           |props.$CURRENT_SECONDARIES_COUNT_COL as $CURRENT_SECONDARIES_COUNT_COL,
           |d.$DATABASE_PRIMARIES_PROPERTY as $REQUESTED_PRIMARIES_COUNT_COL,
           |d.$DATABASE_SECONDARIES_PROPERTY as $REQUESTED_SECONDARIES_COUNT_COL,
           |props.$LAST_COMMITTED_TX_COL as $LAST_COMMITTED_TX_COL,
           |props.$REPLICATION_LAG_COL as $REPLICATION_LAG_COL,
           |d.$DATABASE_CREATED_AT_PROPERTY as $CREATION_TIME_COL,
           |d.$DATABASE_STARTED_AT_PROPERTY as $LAST_START_TIME_COL,
           |d.$DATABASE_STOPPED_AT_PROPERTY as $LAST_STOP_TIME_COL,
           |props.$STORE_COL as $STORE_COL,
           |d:$COMPOSITE_DATABASE as $isCompositeKey,
           |{ $OPTIONS_EXISTING_DATA_KEY: CASE WHEN coalesce(d.$DATABASE_SEED_URI_PROPERTY, d.$DATABASE_DESIGNATED_SEEDER_PROPERTY) IS NOT NULL THEN 'use' ELSE NULL END,
           |  $OPTIONS_SEED_URI_KEY: d.$DATABASE_SEED_URI_PROPERTY,
           |  $OPTIONS_SEED_CONFIG_KEY: d.$DATABASE_SEED_CONFIG_PROPERTY,
           |  $OPTIONS_SEED_CREDENTIALS_KEY: CASE WHEN d.$DATABASE_SEED_CREDENTIALS_ENCRYPTED_PROPERTY IS NOT NULL THEN '********' ELSE NULL END,
           |  $OPTIONS_SEED_INSTANCE_KEY: d.$DATABASE_DESIGNATED_SEEDER_PROPERTY,
           |  $OPTIONS_TX_LOG_ENRICHMENT_KEY: d.$DATABASE_LOG_ENRICHMENT_PROPERTY } as $optionsKey
           |with *, CASE WHEN $isCompositeKey THEN NULL ELSE $optionsMap END as $OPTIONS_COL
           |""".stripMargin
      } else {
        ""
      }
    val verboseNames =
      if (verbose) {
        s", $DATABASE_ID_COL, $SERVER_ID_COL, $REQUESTED_PRIMARIES_COUNT_COL, $REQUESTED_SECONDARIES_COUNT_COL, $CURRENT_PRIMARIES_COUNT_COL, " +
          s"$CURRENT_SECONDARIES_COUNT_COL, $CREATION_TIME_COL, $LAST_START_TIME_COL, $LAST_STOP_TIME_COL, $STORE_COL, $LAST_COMMITTED_TX_COL, $REPLICATION_LAG_COL, " +
          s"$OPTIONS_COL"
      } else {
        ""
      }
    val returnClause = AdministrationShowCommandUtils.generateReturnClause(symbols, yields, returns, Seq("name"))

    val query = Predef.augmentString(
      s"""// First resolve which database is the home database
           |OPTIONAL MATCH (default:$DATABASE_LABEL {$DATABASE_DEFAULT_PROPERTY: true})
           |OPTIONAL MATCH (user:User {$NAME_PROPERTY: $$`$usernameKey`})
           |WITH coalesce(user.homeDatabase, default.$DATABASE_NAME_PROPERTY) as homeDbName
           |
           |UNWIND $$`$accessibleDbsKey` AS props
           |MATCH (d:$DATABASE)<-[:$TARGETS]-(dn:$DATABASE_NAME {$NAME_PROPERTY: props.name, $NAMESPACE_PROPERTY: '$DEFAULT_NAMESPACE'})
           |WITH d, dn, props, homeDbName
           |OPTIONAL MATCH (d)<-[:$TARGETS]-(a:$DATABASE_NAME)
           |WITH a, d, dn, props, homeDbName ORDER BY a.$DISPLAY_NAME_PROPERTY
           |OPTIONAL MATCH (constituent:$DATABASE_NAME {$NAMESPACE_PROPERTY: dn.$NAME_PROPERTY})
           |WHERE d:$COMPOSITE_DATABASE AND constituent <> dn
           |WITH d.name as name,
           |collect(a) as aliases,
           |collect(constituent.$DISPLAY_NAME_PROPERTY) as constituents,
           |props.$ACCESS_COL as $ACCESS_COL,
           |props.$ADDRESS_COL as $ADDRESS_COL,
           |props.$ROLE_COL as $ROLE_COL,
           |props.$WRITER_COL as $WRITER_COL,
           | // serverID needs to be part of the grouping key here as it is guaranteed to be different on different servers
           |props.$SERVER_ID_COL as $SERVER_ID_COL,
           |d.$DATABASE_STATUS_PROPERTY as requestedStatus,
           |props.$CURRENT_STATUS_COL as $CURRENT_STATUS_COL,
           |props.$STATUS_MSG_COL as $STATUS_MSG_COL,
           |props.type as type,
           |d.$DATABASE_DEFAULT_PROPERTY as default,
           |homeDbName,
           |coalesce( homeDbName in collect(a.$DISPLAY_NAME_PROPERTY) + [d.name], false ) as home
           |$verboseColumns
           |
           |WITH name AS $NAME_COL,
           |type,
           |[alias in aliases WHERE NOT (name = alias.$NAME_PROPERTY AND alias.$NAMESPACE_PROPERTY = '$DEFAULT_NAMESPACE') | alias.$DISPLAY_NAME_PROPERTY] as $ALIASES_COL,
           |$ACCESS_COL,
           |$ADDRESS_COL,
           |$ROLE_COL,
           |$WRITER_COL,
           |requestedStatus AS $REQUESTED_STATUS_COL,
           |$CURRENT_STATUS_COL,
           |$STATUS_MSG_COL,
           |default AS $DEFAULT_COL,
           |home AS $HOME_COL,
           |constituents as $CONSTITUENTS_COL
           |$verboseNames
           |$returnClause
           |"""
    ).stripMargin
    SystemCommandExecutionPlan(
      scope.showCommandName,
      normalExecutionEngine,
      securityAuthorizationHandler,
      query,
      VirtualValues.EMPTY_MAP,
      parameterTransformer = new DatabaseListParameterTransformerFunction(
        referenceResolver,
        defaultDatabaseResolver,
        infoService,
        dbms,
        yields,
        verbose,
        scope
      )
    )
  }
}

object ShowDatabaseExecutionPlanner {
  protected[administration] val accessibleDbsKey: String = internalKey("accessibleDbs")
}
