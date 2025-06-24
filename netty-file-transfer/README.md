# Netty File Transfer

This project demonstrates how to send files between two Java instances using the Netty framework.

## Features

- File Sender: Connects to a server and sends a specified file.
- File Receiver: Listens on a port for incoming file transfers and saves them to a directory.
- Uses Netty's `ChunkedWriteHandler` for efficient streaming of large files.

## Prerequisites

- Java Development Kit (JDK) 8 or higher
- Apache Maven

## Building the Project

1. Clone the repository (or ensure you have the project files).
2. Navigate to the project's root directory (`netty-file-transfer`).
3. Run the Maven package command:
   ```bash
   mvn package
   ```
   This will compile the project and create a JAR file in the `target/` directory (e.g., `netty-file-transfer-1.0-SNAPSHOT.jar`).

## Running the Application

The application can be run in two modes: `send` or `receive`.

### Receiver Mode

To start the file receiver, run the following command in a terminal:

```bash
java -cp target/netty-file-transfer-1.0-SNAPSHOT.jar com.example.App receive <port> <output_directory>
```

- `<port>`: The port number the receiver should listen on (e.g., 8080).
- `<output_directory>`: The directory where received files will be saved (e.g., `received_files/`). This directory will be created if it doesn't exist.

**Example:**

```bash
java -cp target/netty-file-transfer-1.0-SNAPSHOT.jar com.example.App receive 8080 received_data
```
This will start the receiver on port 8080, saving files to the `received_data` directory.

### Sender Mode

To send a file, run the following command in a separate terminal:

```bash
java -cp target/netty-file-transfer-1.0-SNAPSHOT.jar com.example.App send <host> <port> <file_path>
```

- `<host>`: The hostname or IP address of the receiver (e.g., `localhost`).
- `<port>`: The port number the receiver is listening on (e.g., 8080).
- `<file_path>`: The path to the file you want to send (e.g., `my_document.txt`).

**Example:**

```bash
java -cp target/netty-file-transfer-1.0-SNAPSHOT.jar com.example.App send localhost 8080 path/to/your/file.txt
```
This will send `file.txt` to the receiver running on `localhost` at port 8080.

## How it Works

1.  **File Size Transfer**: The sender first sends the total size of the file (as a `long`) to the receiver.
2.  **File Content Transfer**:
    *   The `FileSender` uses Netty's `ChunkedFile` to send the file in manageable chunks. This is efficient for large files as it avoids loading the entire file into memory at once.
    *   The `FileReceiverHandler` on the receiver side reads these chunks. It first reads the 8-byte long value representing the file size.
    *   Then, it creates a new file in the specified output directory (with a timestamped name for uniqueness in this example) and writes the incoming byte chunks to this file.
    *   The receiver keeps track of the received bytes and closes the file stream once the expected number of bytes (indicated by the initial file size message) has been received.

## Code Structure

-   `com.example.App`: Main class to parse command-line arguments and start either the sender or receiver.
-   `com.example.FileSender`: Contains the client-side logic to connect to the receiver and send a file.
-   `com.example.FileReceiver`: Contains the server-side logic to listen for incoming connections.
-   `com.example.FileReceiverHandler`: A Netty channel handler that processes incoming data, reconstructs the file, and saves it.

## Notes

-   The current implementation of the receiver saves files with a generated name (`received_file_<timestamp>`). A more advanced implementation could include sending the original filename as part of the transfer protocol.
-   Error handling is basic. Production applications would require more robust error handling and recovery mechanisms.
-   For simplicity, this example does not use encryption (SSL/TLS) for the data transfer. For sensitive data, encryption should be added.
