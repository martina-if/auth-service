package com.codetest.auth.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import com.codetest.auth.EndpointException;
import com.codetest.auth.util.Passwords;
import com.codetest.auth.util.TimeUtil;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.AlreadyExistsException;
import com.spotify.apollo.Status;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/***
 * Cassandra storage for user data
 */
public class CassandraUserDataStore implements UserDataStore, Closeable {

  private static final Logger LOG = getLogger(CassandraUserDataStore.class);
  private final PreparedStatement SELECT;
  private final PreparedStatement INSERT;
  private final PreparedStatement UPDATE;
  private final Clock clock;
  private final Passwords passwords;
  private final SecureRandom random = new SecureRandom();
  private final Cluster cluster;
  private Session session;

  public CassandraUserDataStore(String node, final Passwords passwords) {
    this.passwords = passwords;
    this.clock = Clock.systemUTC();
    cluster = connect(node);
    session = cluster.connect();
    createSchema();

    SELECT = session.prepare(
        "SELECT * FROM user.accounts WHERE userid = ?;")
        .setConsistencyLevel(ConsistencyLevel.LOCAL_ONE);
    INSERT = session.prepare(
        "INSERT INTO user.accounts (userid, username, fullname, salt, password, activity) " +
        "VALUES (?, ?, ?, ?, ? , ?)" +
        "IF NOT EXISTS" +
        ";")
        .setConsistencyLevel(ConsistencyLevel.ALL);
    UPDATE = session.prepare(
        "UPDATE user.accounts SET activity = activity + ? WHERE userid = ?")
        .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
  }

  public Cluster connect(String node) {
    Cluster cluster = Cluster.builder()
        .addContactPoint(node)
        .build();
    Metadata metadata = cluster.getMetadata();
    LOG.info("Connected to cluster: %s\n",
             metadata.getClusterName());
    for ( Host host : metadata.getAllHosts() ) {
      LOG.info("Datacenter: {}; Host: {}; Rack: {}\n",
               host.getDatacenter(), host.getAddress(), host.getRack());
    }
    return cluster;
  }

  @Override
  public ListenableFuture<UserData> createUserData(final String username, final String passwordText, final String fullname) {

    // Check if user exists
    BoundStatement select = SELECT.bind(userid(username));
    ResultSetFuture selectResult= session.executeAsync(select);
    return Futures.transform(selectResult, (ResultSet userRow) -> {

      if (!userRow.isExhausted()) {
        throw new EndpointException(Status.BAD_REQUEST, "Unable to create user");
      }

      // Insert user
      String firstTimestamp = TimeUtil.timestamp(clock);
      String salt = newSalt();
      String encryptedPassword = passwords.encryptPassword(passwordText, salt);
      BoundStatement insert = INSERT.bind(userid(username),
                                          username,
                                          fullname,
                                          salt,
                                          encryptedPassword,
                                          Collections.singletonList(firstTimestamp));

      ResultSetFuture insertResultFuture = session.executeAsync(insert);
      return Futures.transform(insertResultFuture, (ResultSet insertResult) -> {
        if (!insertResult.wasApplied()) {
          throw new EndpointException(Status.INTERNAL_SERVER_ERROR, "Unable to create user");
        }

        return new UserDataBuilder()
            .username(username)
            .fullname(fullname)
            .salt(salt)
            .password(encryptedPassword)
            .loginTimestamps(firstTimestamp)
            .build();
      });
    });
  }

  @Override
  public ListenableFuture<Optional<UserData>> fetchUserData(final String username) {
    BoundStatement select = SELECT.bind(userid(username));
    ResultSetFuture selectResultFuture = session.executeAsync(select);

    return Futures.transform(selectResultFuture, (ResultSet selectResult) -> {
      if (selectResult.isExhausted()) {
        return Optional.<UserData>empty();
      }
      Row userRow = selectResult.one();
      return Optional.of(new UserDataBuilder()
                             .username(userRow.getString("username"))
                             .fullname(userRow.getString("fullname"))
                             .salt(userRow.getString("salt"))
                             .password(userRow.getString("password"))
                             .loginTimestamps(userRow.getList("activity", String.class))
                             .build());
    });

  }

  @Override
  public ListenableFuture<Void> markUserAccess(final String username) {
    String newTimestamp = TimeUtil.timestamp(clock);
    ResultSetFuture selectResultFuture = session.executeAsync(SELECT.bind(userid(username)));

    return Futures.transform(selectResultFuture, (ResultSet selectResult) -> {

      if (selectResult.isExhausted()) {
        throw new EndpointException(Status.NOT_FOUND, "User not found");
      }

      BoundStatement update = UPDATE.bind(Collections.singletonList(newTimestamp),
                                          userid(username));

      ResultSetFuture updateResultFuture = session.executeAsync(update);

      return Futures.transform(updateResultFuture, (ResultSet updateResult) -> {

        if (!updateResult.wasApplied()) {
          LOG.warn("Could not update activity for user {}", username);
          throw new EndpointException(Status.INTERNAL_SERVER_ERROR, "Could not update user");
        }
        return Futures.immediateFuture(null);
      });
    });
  }

  @Override
  public void close() throws IOException {
    if (cluster != null)
      cluster.close();
  }

  private void createSchema() {
    try {
      LOG.info("Creating keyspace user");
      session.execute("CREATE KEYSPACE user WITH replication " +
                      "= {'class':'SimpleStrategy', 'replication_factor':1};");

    } catch (AlreadyExistsException e) {
      LOG.warn("Skipping keyspace creation: {}", e.getMessage());
    }
    try {
      LOG.info("Creating table user.accounts");
      session.execute(
          "CREATE TABLE user.accounts (" +
          "userid text PRIMARY KEY," +
          "username text," +
          "fullname text," +
          "salt text," +
          "password text," +
          "activity list<text>," +
          ");");
    } catch (AlreadyExistsException e) {
      LOG.warn("Skipping table creation: {}", e.getMessage());
    }
  }

  @VisibleForTesting
  List<UserData> getAllAccounts() {
    List<UserData> allUsers = Lists.newArrayList();
    ResultSet result = session.execute("SELECT * FROM user.accounts ");
    for (Row row : result) {
      allUsers.add(
          new UserDataBuilder()
              .username(row.getString("username"))
              .fullname(row.getString("fullname"))
              .salt(row.getString("salt"))
              .password(row.getString("password"))
              .build()
      );
    }
    return allUsers;
  }

  /**
   * user ids will be SHA-256 of the username. Mainly to improve data distribution if sharded
   */
  private static String userid(String username) {
    return new String(DigestUtils.sha256(username), Charsets.UTF_8);
  }

  /**
   * Generates a random salt of 6 characters
   */
  private String newSalt() {
    return new BigInteger(30, random).toString(32);
  }
}
