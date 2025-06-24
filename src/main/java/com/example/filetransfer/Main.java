package com.example.filetransfer;

import com.example.filetransfer.client.FileTransferClient;
import com.example.filetransfer.server.FileTransferServer;

import java.util.Arrays;

/**
 * 应用程序的主入口点。
 * 根据命令行参数启动服务器或客户端。
 */
public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            printUsage();
            return;
        }

        String mode = args[0];

        if ("server".equalsIgnoreCase(mode)) {
            if (args.length != 3) {
                printUsage();
                return;
            }
            int port = Integer.parseInt(args[1]);
            String fileRoot = args[2];
            FileTransferServer.start(port, fileRoot);
        } else if ("client".equalsIgnoreCase(mode)) {
            if (args.length != 5) {
                printUsage();
                return;
            }
            String host = args[1];
            int port = Integer.parseInt(args[2]);
            String fileName = args[3];
            String savePath = args[4];
            FileTransferClient.start(host, port, fileName, savePath);
        } else {
            printUsage();
        }
    }

    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("  To start server: java -jar your-jar-name.jar server <port> <file-root-path>");
        System.err.println("  To start client: java -jar your-jar-name.jar client <host> <port> <file-name-to-download> <save-path>");
        System.err.println("\nExamples:");
        System.err.println("  java -jar netty-filetransfer-1.0-SNAPSHOT-jar-with-dependencies.jar server 8080 /path/to/server/files");
        System.err.println("  java -jar netty-filetransfer-1.0-SNAPSHOT-jar-with-dependencies.jar client 127.0.0.1 8080 my-document.pdf /path/to/client/downloads/my-document.pdf");
    }
}