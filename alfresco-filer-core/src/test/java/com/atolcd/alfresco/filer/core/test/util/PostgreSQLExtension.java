package com.atolcd.alfresco.filer.core.test.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;

public class PostgreSQLExtension implements BeforeAllCallback {

  private static final Logger LOGGER = LoggerFactory.getLogger(PostgreSQLExtension.class);

  private static final String PORT_PROPERTY = "DB_PORT";
  private static final String DATABASE_NAME_PROPERTY = "DB_NAME";
  private static final String USERNAME_PROPERTY = "DB_USERNAME";

  /**
   * Cache for the database.
   * <p>
   * This needs to be static, since test instances may be destroyed and recreated
   * between invocations of individual test methods, as is the case with JUnit.
   * </p>
   * @see org.springframework.test.context.TestContextManager#contextCache
   */
  private static final Map<Class<?>, EmbeddedPostgres> CACHE = new ConcurrentHashMap<>();

  @Override
  public void beforeAll(final ExtensionContext context) throws Exception {
    CACHE.computeIfAbsent(getClass(), this::initDatabase);
  }

  private EmbeddedPostgres initDatabase(final Class<?> testClass) { // NOPMD - method must be a Function
    try {
      EmbeddedPostgres database = EmbeddedPostgres.builder().start();

      // The database closes itself automatically by adding a shutdown hook to the JVM

      String userName = buildRandomName();
      String databaseName = buildRandomName();

      createDatabase(database, userName, databaseName);

      LOGGER.info("Database {} created with user {} at port {}", databaseName, userName, database.getPort());

      System.setProperty(PORT_PROPERTY, String.valueOf(database.getPort()));
      System.setProperty(DATABASE_NAME_PROPERTY, databaseName);
      System.setProperty(USERNAME_PROPERTY, userName);

      return database;
    } catch (RuntimeException e) { // NOPMD - for logging purposes
      LOGGER.error("Error starting database: ", e);
      throw e;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void createDatabase(final EmbeddedPostgres embeddedPostgres, final String userName, final String databaseName) {
    try (Connection connection = embeddedPostgres.getPostgresDatabase().getConnection();
        PreparedStatement createUserStatement = connection
            .prepareStatement(String.format("CREATE USER %s WITH CREATEDB", userName));
        PreparedStatement createDatabaseStatement = connection
            .prepareStatement(String.format("CREATE DATABASE %s OWNER %s ENCODING = 'utf8'", databaseName, userName))) {
      createUserStatement.execute();
      createDatabaseStatement.execute();
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Build a random name (12 lower case alphabetic characters)
   *
   * <p>
   * Name generation is taken from {@link com.opentable.db.postgres.embedded.PreparedDbProvider.PrepPipeline#run()}
   * </p>
   *
   * @return Generated name
   */
  private static String buildRandomName() {
    return RandomStringUtils.randomAlphabetic(12).toLowerCase(Locale.ENGLISH);
  }
}
