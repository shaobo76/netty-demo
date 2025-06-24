package com.example;

import java.util.Arrays;

/**
 * Main application class to run as either a FileSender or FileReceiver.
 */
public class App {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            printUsage();
            return;
        }

        String mode = args[0];
        String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);

        if ("send".equalsIgnoreCase(mode)) {
            if (remainingArgs.length != 3) {
                System.err.println("Invalid arguments for send mode.");
                printUsage();
                return;
            }
            // Call FileSender.main directly, as it handles its own argument parsing
            FileSender.main(remainingArgs);
        } else if ("receive".equalsIgnoreCase(mode)) {
            if (remainingArgs.length != 2) {
                System.err.println("Invalid arguments for receive mode.");
                printUsage();
                return;
            }
            // Call FileReceiver.main directly
            FileReceiver.main(remainingArgs);
        } else {
            System.err.println("Invalid mode: " + mode);
            printUsage();
        }
    }

    private static void printUsage() {
        System.err.println("Usage: java -cp <classpath> com.example.App <mode> [options]");
        System.err.println("Modes:");
        System.err.println("  send <host> <port> <file_path>    - Run as file sender");
        System.err.println("  receive <port> <output_directory> - Run as file receiver");
        System.err.println("\nExamples:");
        System.err.println("  To send a file:");
        System.err.println("    java -cp target/netty-file-transfer-1.0-SNAPSHOT.jar com.example.App send localhost 8080 myfile.txt");
        System.err.println("  To receive a file:");
        System.err.println("    java -cp target/netty-file-transfer-1.0-SNAPSHOT.jar com.example.App receive 8080 /path/to/received_files");
    }
}
