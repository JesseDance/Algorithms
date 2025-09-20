package main

import (
	"bufio"
	"fmt"
	"os"
	"strings"
)

func main() {
	reader := bufio.NewReader(os.Stdin)

	fmt.Println("Simple Input Echo Program")
	fmt.Println("Type something and press Enter. Type 'exit' to quit.")

	for {
		fmt.Print("> ")
		input, err := reader.ReadString('\n')
		if err != nil {
			fmt.Println("Error reading input:", err)
			return
		}

		input = strings.TrimSpace(input)

		if input == "exit" {
			fmt.Println("Exiting. Goodbye!")
			break
		}

		fmt.Println("You typed:", input)
	}
}
