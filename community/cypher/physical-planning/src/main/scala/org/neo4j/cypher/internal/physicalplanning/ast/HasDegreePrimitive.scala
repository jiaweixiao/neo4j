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
package org.neo4j.cypher.internal.physicalplanning.ast

import org.neo4j.cypher.internal.expressions.Expression
import org.neo4j.cypher.internal.expressions.SemanticDirection
import org.neo4j.cypher.internal.runtime.ast.BooleanRuntimeExpression

case class HasDegreeGreaterThanPrimitive(
  offset: Int,
  typ: Option[Either[Int, String]],
  direction: SemanticDirection,
  degree: Expression
) extends BooleanRuntimeExpression

case class HasDegreeGreaterThanOrEqualPrimitive(
  offset: Int,
  typ: Option[Either[Int, String]],
  direction: SemanticDirection,
  degree: Expression
) extends BooleanRuntimeExpression

case class HasDegreePrimitive(
  offset: Int,
  typ: Option[Either[Int, String]],
  direction: SemanticDirection,
  degree: Expression
) extends BooleanRuntimeExpression

case class HasDegreeLessThanPrimitive(
  offset: Int,
  typ: Option[Either[Int, String]],
  direction: SemanticDirection,
  degree: Expression
) extends BooleanRuntimeExpression

case class HasDegreeLessThanOrEqualPrimitive(
  offset: Int,
  typ: Option[Either[Int, String]],
  direction: SemanticDirection,
  degree: Expression
) extends BooleanRuntimeExpression