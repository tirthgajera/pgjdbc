/*
 * Copyright (c) 2016, PostgreSQL Global Development Group
 * See the LICENSE file in the project root for more information.
 */

package org.postgresql.core;

import org.postgresql.replication.PGReplicationStream;
import org.postgresql.replication.fluent.logical.LogicalReplicationOptions;
import org.postgresql.replication.fluent.physical.PhysicalReplicationOptions;

import java.sql.SQLException;

/**
 * Abstracts the protocol-specific details of physic and logic replication.
 *
 * <p>With each connection open with replication options associate own instance ReplicationProtocol.</p>
 */
public interface ReplicationProtocol {
  /**
   * Starts logical replication.
   * @param options not null options for logical replication stream
   * @return not null stream instance from which available fetch wal logs that was decode by output
   *     plugin
   * @throws SQLException on error
   */
  PGReplicationStream startLogical(LogicalReplicationOptions options) throws SQLException;

  /**
   * Starts physical replication.
   * @param options not null options for physical replication stream
   * @return not null stream instance from which available fetch wal logs
   * @throws SQLException on error
   */
  PGReplicationStream startPhysical(PhysicalReplicationOptions options) throws SQLException;
}
