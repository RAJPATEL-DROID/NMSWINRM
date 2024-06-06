package utils

import (
	"fmt"
	"os"
	"path/filepath"
	"time"
)

const (
	Info  = 2
	Debug = 1
	Trace = 0
)

type Logger struct {
	directory string
	component string
}

var logLevel = 2

func NewLogger(directory, component string) Logger {

	return Logger{
		directory: fmt.Sprintf("../pluginEnginelogs/%s", directory),
		component: component,
	}
}

func write(level, message, directory, component string) {

	currentTime := time.Now()

	// Create the log file name
	fileName := fmt.Sprintf("%s/%s%d-%s.log", directory, currentTime.Format("2006-01-02"), currentTime.Hour(), component)

	err := os.MkdirAll(filepath.Dir(fileName), 0755)
	if err != nil {
		fmt.Println("Error creating directories:", err)
		return
	}

	file, err := os.OpenFile(fileName, os.O_APPEND|os.O_WRONLY|os.O_CREATE, 0644)

	if err != nil {
		fmt.Println("Error opening file:", err)
		return
	}

	defer func(file *os.File) {
		err := file.Close()
		if err != nil {
			fmt.Println("Error closing file:", err)
		}
	}(file)

	_, err = file.WriteString(fmt.Sprintf("%s %s %s\n", currentTime.Format("2006-01-02 15:04:05.999999999"), level, message))
	if err != nil {
		fmt.Println("Error writing to file:", err)
		return
	}
}

func (l *Logger) Info(message string) {

	if logLevel <= Info {

		write("Info", message, l.directory, l.component)
	}
}
func (l *Logger) Error(message string) {

	write("Error", message, l.directory, l.component)

}
func (l *Logger) Debug(message string) {

	if logLevel <= Debug {

		write("Debug", message, l.directory, l.component)
	}
}
func (l *Logger) Trace(message string) {
	if logLevel <= Trace {

		write("Trace", message, l.directory, l.component)
	}
}
func (l *Logger) Fatal(message string) {

	write("Fatal", message, l.directory, l.component)

}
func (l *Logger) Warn(message string) {

	write("Warn", message, l.directory, l.component)

}
