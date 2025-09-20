package main

import (
	"bufio"
	"fmt"
	"io"
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"
)

// Node represents one CSV row as linked list node
type Node struct {
	fields []string
	next   *Node
}

// InMemoryDB holds the head of linked list and file info
type InMemoryDB struct {
	head         *Node
	filename     string
	watcherQuit  chan struct{}
	watcherWG    sync.WaitGroup
	watcherMutex sync.Mutex
}

func main() {
	db := &InMemoryDB{}
	reader := bufio.NewReader(os.Stdin)

	fmt.Println("In-Memory CSV Database")
	fmt.Println("Commands: load, unload, print, save, exit")

	for {
		fmt.Print("> ")
		cmd, _ := reader.ReadString('\n')
		cmd = strings.TrimSpace(strings.ToLower(cmd))

		switch cmd {
		case "load":
			if db.head != nil {
				fmt.Println("A file is already loaded. Unload it first.")
				continue
			}
			fmt.Print("Enter CSV filename: ")
			filename, _ := reader.ReadString('\n')
			filename = strings.TrimSpace(filename)

			if _, err := os.Stat(filename); err != nil {
				fmt.Println("File not found.")
				continue
			}

			fmt.Printf("Confirm loading file '%s'? (yes/no): ", filename)
			confirm, _ := reader.ReadString('\n')
			confirm = strings.TrimSpace(strings.ToLower(confirm))
			if confirm != "yes" && confirm != "y" {
				fmt.Println("Load cancelled.")
				continue
			}

			file, err := os.Open(filename)
			if err != nil {
				fmt.Println("Error opening file:", err)
				continue
			}

			head, err := loadCSVRecursive(bufio.NewReader(file))
			file.Close()
			if err != nil {
				fmt.Println("Error loading CSV:", err)
				continue
			}

			db.head = head
			db.filename = filename

			// Start watcher
			db.startWatcher()

			fmt.Println("File loaded into memory.")

		case "unload":
			if db.head == nil {
				fmt.Println("No file is currently loaded.")
				continue
			}
			db.stopWatcher()
			db.head = nil
			db.filename = ""
			fmt.Println("In-memory database unloaded.")

		case "print":
			if db.head == nil {
				fmt.Println("No data loaded.")
			} else {
				printList(db.head)
			}

		case "save":
			if db.head == nil {
				fmt.Println("No data to save.")
				continue
			}

			fmt.Printf("Save to original file (%s)? (Y/N): ", db.filename)
			yn, _ := reader.ReadString('\n')
			yn = strings.TrimSpace(strings.ToLower(yn))

			if yn == "y" || yn == "yes" {
				if err := saveToFile(db.filename, db.head); err != nil {
					fmt.Println("Failed to save file:", err)
				} else {
					fmt.Println("Data saved to", db.filename)
				}
			} else if yn == "n" || yn == "no" {
				fmt.Print("Enter new filename to save as: ")
				newFile, _ := reader.ReadString('\n')
				newFile = strings.TrimSpace(newFile)
				if newFile == "" {
					fmt.Println("Save cancelled: no filename given.")
				} else {
					if err := saveToFile(newFile, db.head); err != nil {
						fmt.Println("Failed to save file:", err)
					} else {
						fmt.Println("Data saved to", newFile)
					}
				}
			} else {
				fmt.Println("Invalid input. Save cancelled.")
			}

		case "exit":
			db.stopWatcher()
			fmt.Println("Goodbye!")
			return

		default:
			fmt.Println("Unknown command. Try: load, unload, print, save, exit.")
		}
	}
}

// loadCSVRecursive reads CSV line recursively into linked list
func loadCSVRecursive(reader *bufio.Reader) (*Node, error) {
	line, err := reader.ReadString('\n')
	if err != nil {
		if err == io.EOF && len(line) == 0 {
			return nil, nil // end of file, stop recursion
		} else if err != io.EOF {
			return nil, err
		}
	}

	line = strings.TrimSpace(line)
	fields := strings.Split(line, ",")
	node := &Node{fields: fields}
	nextNode, err := loadCSVRecursive(reader)
	if err != nil {
		return nil, err
	}
	node.next = nextNode
	return node, nil
}

// printList prints the linked list rows
func printList(head *Node) {
	current := head
	rowNum := 1
	for current != nil {
		fmt.Printf("Row %d: %s\n", rowNum, strings.Join(current.fields, " | "))
		current = current.next
		rowNum++
	}
}

// saveToFile writes the linked list data to a CSV file
func saveToFile(filename string, head *Node) error {
	file, err := os.Create(filename)
	if err != nil {
		return err
	}
	defer file.Close()

	writer := bufio.NewWriter(file)
	current := head
	for current != nil {
		line := strings.Join(current.fields, ",")
		_, err := writer.WriteString(line + "\n")
		if err != nil {
			return err
		}
		current = current.next
	}
	return writer.Flush()
}

// startWatcher watches the file for changes and reloads on modification
func (db *InMemoryDB) startWatcher() {
	db.watcherMutex.Lock()
	defer db.watcherMutex.Unlock()

	if db.watcherQuit != nil {
		// watcher already running
		return
	}

	db.watcherQuit = make(chan struct{})
	db.watcherWG.Add(1)

	go func() {
		defer db.watcherWG.Done()

		prevInfo, err := os.Stat(db.filename)
		if err != nil {
			fmt.Println("[Watcher] Error getting file info:", err)
			return
		}
		prevModTime := prevInfo.ModTime()

		dir := filepath.Dir(db.filename)
		base := filepath.Base(db.filename)

		for {
			select {
			case <-db.watcherQuit:
				return
			case <-time.After(1 * time.Second):
				files, err := ioutil.ReadDir(dir)
				if err != nil {
					fmt.Println("[Watcher] Error reading directory:", err)
					continue
				}
				for _, f := range files {
					if f.Name() == base {
						modTime := f.ModTime()
						if modTime.After(prevModTime) {
							prevModTime = modTime
							fmt.Println("\n[Watcher] Detected file change. Reloading...")
							file, err := os.Open(db.filename)
							if err != nil {
								fmt.Println("[Watcher] Failed to open file:", err)
								break
							}
							head, err := loadCSVRecursive(bufio.NewReader(file))
							file.Close()
							if err != nil {
								fmt.Println("[Watcher] Failed to reload file:", err)
								break
							}

							db.watcherMutex.Lock()
							db.head = head
							db.watcherMutex.Unlock()
							fmt.Println("[Watcher] Reload complete.")
						}
						break
					}
				}
			}
		}
	}()
}

// stopWatcher stops the watcher goroutine
func (db *InMemoryDB) stopWatcher() {
	db.watcherMutex.Lock()
	defer db.watcherMutex.Unlock()

	if db.watcherQuit != nil {
		close(db.watcherQuit)
		db.watcherWG.Wait()
		db.watcherQuit = nil
	}
}
