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
  private static AuthClient authClient = new AuthClient(new HttpClient());

  public static void main(String... args) {
    if (args.length < 1 || !COMMANDS.contains(args[0])) {
      error("Error parsing arguments. Missing command: register, login, activity");
    }
    try {
      CommandLine commandLine = parseArgs(args);

      switch (args[0]) {
        case "login":
          Response<ByteString> response = authClient.sendLoginRequest(getOption(commandLine, "u"), getOption(commandLine, "p"));
          printResponse(response);
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
    options.addOption(Option.builder("l")
                          .argName("login-username")
                          .hasArg()
                          .desc("Username to lookup login times for")
                          .build());
    CommandLineParser parser = new DefaultParser();

    return parser.parse(options, args);
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
