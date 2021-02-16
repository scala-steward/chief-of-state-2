/*
 * Copyright 2020 Namely Inc.
 *
 * SPDX-License-Identifier: MIT
 */

package com.namely.chiefofstate

import com.typesafe.config.Config
import org.slf4j.{Logger, LoggerFactory}
import slick.basic.DatabaseConfig
import slick.jdbc.PostgresProfile

import java.sql.{Connection, Statement}

/**
 * Utility class to create the necessary schemas for the write side
 */
object SchemasUtil {
  final val log: Logger = LoggerFactory.getLogger(getClass)

  /**
   * legacyJournalStatement returns the journal ddl
   *
   * @return the sql statement
   */
  private def legacyJournalStatement(): Seq[String] = {
    Seq(
      s"""
     CREATE TABLE IF NOT EXISTS journal (
      ordering        BIGSERIAL,
      persistence_id  VARCHAR(255) NOT NULL,
      sequence_number BIGINT       NOT NULL,
      deleted         BOOLEAN      DEFAULT FALSE,
      tags            VARCHAR(255) DEFAULT NULL,
      message         BYTEA        NOT NULL,
      PRIMARY KEY (persistence_id, sequence_number)
     )""",
      // create index
      s"""CREATE UNIQUE INDEX IF NOT EXISTS journal_ordering_idx on journal(ordering)"""
    )
  }

  /**
   * legacySnapshotTableStatement returns the legacy ddl statement
   *
   * @return the sql statement
   */
  private def legacySnapshotTableStatement(): String =
    s"""
     CREATE TABLE IF NOT EXISTS snapshot (
      persistence_id  VARCHAR(255) NOT NULL,
      sequence_number BIGINT       NOT NULL,
      created         BIGINT       NOT NULL,
      snapshot        BYTEA        NOT NULL,
      PRIMARY KEY (persistence_id, sequence_number)
     )"""

  /**
   *  Attempts to create the various write side legacy data stores
   */
  private def createLegacySchemas(config: Config): Boolean = {
    val dc: DatabaseConfig[PostgresProfile] =
      DatabaseConfig.forConfig[PostgresProfile]("write-side-slick", config)

    val journalSQLs: Seq[String] = legacyJournalStatement()
    val snapshotSQL: String = legacySnapshotTableStatement()

    val conn: Connection = dc.db.createSession().conn

    try {
      val stmt: Statement = conn.createStatement()
      try {
        log.info("setting up journal and snapshot stores....")
        // create the journal table and snapshot journal
        // if DDLs failed, it will raise an SQLException
        journalSQLs
          .map(stmt.execute)
          .map(_ => stmt.execute(snapshotSQL))
          .forall(identity)

      } finally {
        stmt.close()
      }
    } finally {
      log.info("journal and snapshot stores setup. Releasing resources....")
      conn.close()
      dc.db.close()
    }
  }

  /**
   * Creates the required schemas for the write side data stores
   *
   * @param config the application config
   * @return true when successful and false when it fails
   */
  def createIfNotExists(config: Config): Boolean = createLegacySchemas(config)
}