import java.io.*;
import java.nio.file.*;
import java.util.Scanner;

public class recursive_ll_db {

    static volatile Node head = null;
    static File watchedFile = null;
    static Thread watcherThread = null;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("In-Memory CSV Database");
        System.out.println("Commands: load, unload, print, exit");

        while (true) {
            System.out.print("> ");
            String command = scanner.nextLine().trim().toLowerCase();

            switch (command) {
                case "load":
                    if (head != null) {
                        System.out.println("A file is already loaded. Unload it first.");
                        break;
                    }

                    System.out.print("Enter CSV filename: ");
                    String filename = scanner.nextLine().trim();
                    File file = new File(filename);

                    if (!file.exists() || file.isDirectory()) {
                        System.out.println("File not found.");
                        break;
                    }

                    System.out.print("Confirm loading file '" + filename + "'? (yes/no): ");
                    String confirm = scanner.nextLine().trim().toLowerCase();
                    if (!confirm.equals("yes") && !confirm.equals("y")) {
                        System.out.println("Load cancelled.");
                        break;
                    }

                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        head = loadCSV(reader);
                        watchedFile = file;
                        System.out.println("File loaded into memory.");
                        //startFileWatcher(file.toPath().getParent(), file.getName());

                        Path parentDir = file.toPath().getParent();
                        if (parentDir == null) {
                            // file is in current directory, use current working directory instead
                            parentDir = Paths.get(".");
                        }
                        startFileWatcher(parentDir.toAbsolutePath(), file.getName());

                    } catch (IOException e) {
                        System.out.println("Error loading file: " + e.getMessage());
                    }
                    break;

                case "save":
                    if (head == null) {
                        System.out.println("No data to save.");
                        break;
                    }

                    System.out.print("Save to original file (" + watchedFile.getName() + ")? (Y/N): ");
                    String yn = scanner.nextLine().trim().toLowerCase();

                    if (yn.equals("y") || yn.equals("yes")) {
                        saveToFile(watchedFile.getAbsolutePath());
                    } else if (yn.equals("n") || yn.equals("no")) {
                        System.out.print("Enter new filename to save as: ");
                        String newFile = scanner.nextLine().trim();
                        if (!newFile.isEmpty()) {
                            saveToFile(newFile);
                            // Optional: update watchedFile and restart watcher to new file
                            // For simplicity, not changing current watched file here
                        } else {
                            System.out.println("Save cancelled: no filename given.");
                        }
                    } else {
                        System.out.println("Invalid input. Save cancelled.");
                    }
                    break;

                case "unload":
                    if (head == null) {
                        System.out.println("No file is currently loaded.");
                        break;
                    }
                    head = null;
                    watchedFile = null;
                    if (watcherThread != null && watcherThread.isAlive()) {
                        watcherThread.interrupt(); // Stop the watcher
                    }
                    System.out.println("In-memory database unloaded.");
                    break;

                case "print":
                    if (head == null) {
                        System.out.println("No data loaded.");
                    } else {
                        printList(head);
                    }
                    break;

                case "exit":
                    System.out.println("Goodbye!");
                    if (watcherThread != null && watcherThread.isAlive()) {
                        watcherThread.interrupt();
                    }
                    return;

                default:
                    System.out.println("Unknown command. Try: load, unload, print, exit.");
            }
        }
    }

    // ================================
    // Node class to represent each row
    static class Node {
        String[] fields;
        Node next;

        public Node(String[] fields) {
            this.fields = fields;
            this.next = null;
        }
    }

    // ================================
    // Recursive CSV loader
    public static Node loadCSV(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null) return null; // Base case

        String[] fields = line.split(","); // Simple split
        Node node = new Node(fields);
        node.next = loadCSV(reader); // Recursive call
        return node;
    }

    // ================================
    // Print the linked list
    public static void printList(Node head) {
        Node current = head;
        int rowNum = 1;
        while (current != null) {
            System.out.print("Row " + rowNum + ": ");
            for (int i = 0; i < current.fields.length; i++) {
                System.out.print(current.fields[i]);
                if (i < current.fields.length - 1) System.out.print(" | ");
            }
            System.out.println();
            current = current.next;
            rowNum++;
        }
    }

    // ================================
    // File watcher
    public static void startFileWatcher(Path pathToWatch, String filename) {
        watcherThread = new Thread(() -> {
            try {
                WatchService watchService = FileSystems.getDefault().newWatchService();
                pathToWatch.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

                while (!Thread.currentThread().isInterrupted()) {
                    WatchKey key = watchService.take(); // blocking
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path changed = (Path) event.context();

                        if (kind == StandardWatchEventKinds.ENTRY_MODIFY &&
                                changed.toString().equals(filename)) {
                            System.out.println("\n[INFO] Detected file change. Reloading...");

                            try (BufferedReader reader = new BufferedReader(new FileReader(watchedFile))) {
                                head = loadCSV(reader);
                                System.out.println("[INFO] Reload complete.");
                                System.out.print("\n> ");
                            } catch (IOException e) {
                                System.out.println("[ERROR] Reload failed: " + e.getMessage());
                            }
                        }
                    }
                    key.reset();
                }

            } catch (IOException | InterruptedException e) {
                if (!(e instanceof InterruptedException)) {
                    System.out.println("[ERROR] Watcher stopped: " + e.getMessage());
                }
            }
        });

        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    // ================================
    // Save File
    public static void saveToFile(String filename) {
        if (head == null) {
            System.out.println("No data to save.");
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            Node current = head;
            while (current != null) {
                String line = String.join(",", current.fields);
                writer.write(line);
                writer.newLine();
                current = current.next;
            }
            System.out.println("Data saved to " + filename);
        } catch (IOException e) {
            System.out.println("Failed to save file: " + e.getMessage());
        }
    }
}
