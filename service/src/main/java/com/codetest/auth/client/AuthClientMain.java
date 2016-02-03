package com.codetest.auth.client;

import com.google.common.collect.ImmutableList;

import com.spotify.apollo.Response;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.List;

import okio.ByteString;

public class AuthClientMain {

  private static final List<String> COMMANDS = ImmutableList.of("register", "login", "activity");
  private static AuthClient authClient;

  public static void main(String... args) {
    if (args.length < 1 || !COMMANDS.contains(args[0])) {
      error("Error parsing arguments. Missing command: register, login, activity");
    }
    try {
      CommandLine commandLine = parseArgs(args);

      authClient = new AuthClient(new HttpClient(), getAddress(commandLine));

      switch (args[0]) {
        case "login":
          printResponse(authClient.sendLoginRequest(getOption(commandLine, "u"),
                                                    getOption(commandLine, "p")));
          break;
        case "register":
          printResponse(authClient.sendRegisterRequest(getOption(commandLine, "u"),
                                                       getOption(commandLine, "p"),
                                                       getOption(commandLine, "f")));
          break;
        case "activity":
          printResponse(authClient.sendActivityRequest(getOption(commandLine, "a"),
                                                       getOption(commandLine, "u"),
                                                       getOption(commandLine, "s")));
          break;
        default:
          error("Unrecognized command");
      }

    } catch (ParseException e) {
      error("Error parsing arguments: " + e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void printResponse(Response<ByteString> response) {
    System.out.println("Status code: " + response.status());
    if (response.payload().isPresent()) {
      System.out.println("Payload: " + response.payload().get().utf8());
    }
  }

  private static CommandLine parseArgs(String... args) throws ParseException {
    Options options = new Options();
    options.addOption(Option.builder("u")
                          .argName("username")
                          .hasArg()
                          .desc("Username for authentication")
                          .required()
                          .build());
    options.addOption(Option.builder("p")
                          .argName("password")
                          .hasArg()
                          .desc("Username's password")
                          .build());
    options.addOption(Option.builder("s")
                          .argName("session")
                          .hasArg()
                          .desc("Session token for authentication")
                          .build());
    options.addOption(Option.builder("a")
                          .argName("login-username")
                          .hasArg()
                          .desc("Username to lookup login times for")
                          .build());
    options.addOption(Option.builder("f")
                          .argName("full-name")
                          .hasArg()
                          .desc("Full name")
                          .build());
    options.addOption(Option.builder("h")
                      .argName("host")
                      .hasArg()
                      .desc("Host")
                      .build());
    options.addOption(Option.builder("P")
                      .argName("port")
                      .hasArg()
                      .desc("Port")
                      .build());
    CommandLineParser parser = new DefaultParser();

    return parser.parse(options, args);
  }

  /**
   * Reads command line arguments and converts host and port to an
   * address or uses default values localhost and port 8080
   */
  private static String getAddress(CommandLine commandLine) {
    String host = commandLine.getOptionValue("h") != null ?
                  commandLine.getOptionValue("h") : "localhost";
    String port = commandLine.getOptionValue("P") != null ?
                  commandLine.getOptionValue("P") : "8080";
    return host + ":" + port;
  }

  private static String getOption(CommandLine commandLine, String argName) {
    String optionValue = commandLine.getOptionValue(argName);
    if (optionValue == null) {
      System.err.println("Expected option: " + argName);
      System.exit(-1);
    }
    return optionValue;
  }

  private static void error(String message) {
    System.err.println(message);
    System.exit(-1);
  }

}
