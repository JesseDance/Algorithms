In-Memory CSV Database

Overview

This Java program provides a simple in-memory database by recursively loading CSV data into a linked list. It supports dynamic synchronization with the CSV file on disk by watching for file changes and automatically updating the in-memory data. The program operates through a command-line interface, allowing the user to load, unload, print, save, and exit.

Features

Load CSV: Recursively reads a CSV file into a linked list.

Unload Data: Clears the loaded data from memory.

Print Data: Displays the in-memory CSV rows.

Save Data: Saves the in-memory data back to disk, either overwriting the original file or to a new filename.

File Watcher: Monitors the loaded CSV file for external changes and automatically reloads the in-memory data on modification.

Command Interface: Simple commands — load, unload, print, save, and exit.

Usage

Compile the program:
javac InMemoryDatabase.java

Run the program:
java InMemoryDatabase

Use commands:

load: Prompt for CSV filename, confirm, and load into memory.

unload: Clear the current in-memory data and stop watching the file.

print: Display all loaded rows.

save: Save in-memory data to disk, either overwriting the original or saving as a new file.

exit: Exit the program.

How It Works

The CSV file is read line-by-line recursively. Each line becomes a node in a singly linked list.

When a file is loaded, a background watcher thread monitors the file's directory for modifications.

If the CSV file changes externally (e.g., edited in a text editor), the watcher triggers an automatic reload of the linked list.

The program allows saving the current in-memory linked list back to disk.

Limitations and Notes

CSV parsing is simple: fields are split on commas without special handling for quoted commas.

The watcher monitors directory changes and triggers reloads only on file modification events.

Only one CSV file can be loaded at a time.

The program assumes UTF-8 encoding for CSV files.

Potential Extensions

Improved CSV parsing to handle quoted fields.

Support for editing data in-memory.

Additional commands like search, insert, and delete.

Saving the watcher state or multi-file support.

Example Session

In-Memory CSV Database
Commands: load, unload, print, save, exit

load
Enter CSV filename: data.csv
Confirm loading file 'data.csv'? (yes/no): yes
File loaded into memory.
print
Row 1: Name | Age | City
Row 2: Alice | 30 | Seattle
Row 3: Bob | 25 | Portland
save
Save to original file (data.csv)? (Y/N): n
Enter new filename to save as: data_backup.csv
Data saved to data_backup.csv
unload
In-memory database unloaded.
exit
Goodbye!