package com.codetest.auth.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import com.codetest.auth.EndpointException;
import com.codetest.auth.util.Passwords;
import com.codetest.auth.util.TimeUtil;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.AlreadyExistsException;
import com.spotify.apollo.Status;

import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

public class CassandraDataStore implements UserDataStore, Closeable {

  private static final Logger LOG = getLogger(CassandraDataStore.class);
  private final PreparedStatement SELECT;
  private final PreparedStatement INSERT;
  private final PreparedStatement UPDATE;
  private final Clock clock;
  private final Cluster cluster;
  private final Passwords passwords;
  private Session session;

  public CassandraDataStore(String node, final Passwords passwords) {
    this.passwords = passwords;
    this.clock = Clock.systemUTC();
    cluster = connect(node);
    session = cluster.connect();
    createSchema();

    SELECT = session.prepare(
        "SELECT * FROM user.accounts WHERE userid = ?;");
    INSERT = session.prepare(
        "INSERT INTO user.accounts (userid, username, fullname, salt, password, activity) " +
        "VALUES (?, ?, ?, ?, ? , ?)" +
        "IF NOT EXISTS" +
        ";");
    UPDATE = session.prepare(
        "UPDATE user.accounts SET activity = activity + ? WHERE userid = ?"
    );
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
  public UserData createUserData(final String username, final String passwordText, final String fullname) {

    // Check if user exists
    BoundStatement select = SELECT.bind(userid(username));
    ResultSet selectResult= session.execute(select); // FIXME make async
    if (!selectResult.isExhausted()) {
      throw new EndpointException(Status.BAD_REQUEST, "Unable to create user");
    }

    String firstTimestamp = TimeUtil.timestamp(clock);
    // Insert user
    String encryptedPassword = passwords.encryptPassword(passwordText, "salt");
    BoundStatement insert = INSERT.bind(userid(username),
                                        username,
                                        fullname,
                                        "salt", // FIXME salt
                                        encryptedPassword,
                                        Collections.singletonList(firstTimestamp));
    ResultSet insertResult = session.execute(insert);
    if (!insertResult.wasApplied()) {
      throw new EndpointException(Status.INTERNAL_SERVER_ERROR, "Unable to create user");
    }

    return new UserDataBuilder()
        .username(username)
        .fullname(fullname)
        .salt("salt") // FIXME
        .password(encryptedPassword)
        .loginTimestamps(firstTimestamp)
        .build();
  }

  @Override
  public Optional<UserData> fetchUserData(final String username) {
    BoundStatement select = SELECT.bind(userid(username));
    ResultSet selectResult = session.execute(select); // FIXME make async
    if (selectResult.isExhausted()) {
      return Optional.empty();
    }
    Row userRow = selectResult.one();
    return Optional.of(new UserDataBuilder()
                           .username(userRow.getString("username"))
                           .fullname(userRow.getString("fullname"))
                           .salt(userRow.getString("salt"))
                           .password(userRow.getString("password"))
                           .loginTimestamps(userRow.getList("activity", String.class))
                           .build());
  }

  @Override
  public void markUserAccess(final String username) {
    String newTimestamp = TimeUtil.timestamp(clock);
    ResultSet selectResult = session.execute(SELECT.bind(userid(username)));
    if (selectResult.isExhausted()) {
      throw new EndpointException(Status.NOT_FOUND, "User not found");
    }

    BoundStatement update = UPDATE.bind(Collections.singletonList(newTimestamp),
                                        userid(username));
    ResultSet updateResult = session.execute(update);
    if (!updateResult.wasApplied()) {
      LOG.warn("Could not update activity for user {}", username);
      throw new EndpointException(Status.INTERNAL_SERVER_ERROR, "Could not update user");
    }
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

  private static String userid(String username) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      messageDigest.update(username.getBytes());
      return new String(messageDigest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Unable to instantiate hash algorithm");
    }
  }
}
