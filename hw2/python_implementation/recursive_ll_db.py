import os
import threading
import time
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler

head = None
watched_file = None
watcher_thread = None
observer = None

class Node:
    def __init__(self, fields):
        self.fields = fields
        self.next = None

def load_csv_recursive(file_obj):
    line = file_obj.readline()
    if not line:
        return None
    fields = line.strip().split(",")
    node = Node(fields)
    node.next = load_csv_recursive(file_obj)
    return node

def print_list(head):
    current = head
    row_num = 1
    while current:
        print(f"Row {row_num}: {' | '.join(current.fields)}")
        current = current.next
        row_num += 1

def save_to_file(filename):
    global head
    if head is None:
        print("No data to save.")
        return

    try:
        with open(filename, 'w') as f:
            current = head
            while current:
                f.write(','.join(current.fields) + '\n')
                current = current.next
        print(f"Data saved to {filename}")
    except Exception as e:
        print(f"Failed to save file: {e}")

class FileChangeHandler(FileSystemEventHandler):
    def __init__(self, target_file):
        super().__init__()
        self.target_file = os.path.abspath(target_file)

    def on_modified(self, event):
        global head
        if os.path.abspath(event.src_path) == self.target_file:
            print("\n[INFO] Detected file change. Reloading...")
            try:
                with open(self.target_file, 'r') as f:
                    head = load_csv_recursive(f)
                print("[INFO] Reload complete.")
                print("> ", end="", flush=True)
            except Exception as e:
                print(f"[ERROR] Reload failed: {e}")

def start_file_watcher(filepath):
    global observer, watcher_thread
    dir_to_watch = os.path.dirname(os.path.abspath(filepath))
    handler = FileChangeHandler(filepath)

    observer = Observer()
    observer.schedule(handler, path=dir_to_watch, recursive=False)
    observer.start()

def stop_file_watcher():
    global observer
    if observer:
        observer.stop()
        observer.join()

def main():
    global head, watched_file

    print("In-Memory CSV Database")
    print("Commands: load, unload, print, save, exit")

    while True:
        try:
            command = input("> ").strip().lower()
        except (EOFError, KeyboardInterrupt):
            command = "exit"

        if command == "load":
            if head:
                print("A file is already loaded. Unload it first.")
                continue

            filename = input("Enter CSV filename: ").strip()
            if not os.path.exists(filename):
                print("File not found.")
                continue

            confirm = input(f"Confirm loading file '{filename}'? (yes/no): ").strip().lower()
            if confirm not in ("yes", "y"):
                print("Load cancelled.")
                continue

            try:
                with open(filename, 'r') as f:
                    head = load_csv_recursive(f)
                    watched_file = filename
                    print("File loaded into memory.")
                    start_file_watcher(filename)
            except Exception as e:
                print(f"Error loading file: {e}")

        elif command == "save":
            if not head:
                print("No data to save.")
                continue

            yn = input(f"Save to original file ({os.path.basename(watched_file)})? (Y/N): ").strip().lower()
            if yn in ("y", "yes"):
                save_to_file(watched_file)
            elif yn in ("n", "no"):
                new_file = input("Enter new filename to save as: ").strip()
                if new_file:
                    save_to_file(new_file)
                else:
                    print("Save cancelled: no filename given.")
            else:
                print("Invalid input. Save cancelled.")

        elif command == "unload":
            if not head:
                print("No file is currently loaded.")
                continue
            head = None
            watched_file = None
            stop_file_watcher()
            print("In-memory database unloaded.")

        elif command == "print":
            if not head:
                print("No data loaded.")
            else:
                print_list(head)

        elif command == "exit":
            print("Goodbye!")
            stop_file_watcher()
            break

        else:
            print("Unknown command. Try: load, unload, print, save, exit")

if __name__ == "__main__":
    main()
