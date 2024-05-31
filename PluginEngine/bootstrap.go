package main

import (
	"fmt"
	zmq "github.com/pebbe/zmq4"
	"pluginengine/processor"
	"pluginengine/utils"
)

func main() {

	// Read configuration from config.json
	config, err := utils.ReadConfig("../Config/pluginConfig.json")

	if err != nil {
		fmt.Printf("Error reading config file: %s\n", err)
		return
	}

	// Set Up Logger
	logger := utils.NewLogger("bootstrap", "gobootstrap")

	// Initialize ZeroMQ context
	zmqContext, err := zmq.NewContext()
	if err != nil {
		logger.Fatal(fmt.Sprintf("Error creating ZeroMQ context: %s", err))
		return
	}

	pullSocket, err := zmqContext.NewSocket(zmq.PULL)

	if err != nil {
		logger.Fatal(fmt.Sprintf("Error creating ZeroMQ socket: %s", err))
		return
	}

	pullAddress := fmt.Sprintf("tcp://%s:%d", config.PublisherHost, config.PullPort)

	err = pullSocket.Connect(pullAddress)

	if err != nil {
		logger.Fatal(fmt.Sprintf("Error binding ZeroMQ socket: %s", err))
		return
	}

	logger.Info(fmt.Sprintf("Push Socket Connected to %s address", pullAddress))

	pushSocket, err := zmqContext.NewSocket(zmq.PUSH)
	if err != nil {
		logger.Fatal(fmt.Sprintf("Error creating PUSH socket: %v", err))
		return
	}

	defer func(pushSocket *zmq.Socket, pullSocket *zmq.Socket, zmqContext *zmq.Context) {
		err := pushSocket.Close()
		if err != nil {
			logger.Fatal(fmt.Sprintf("Error closing Push socket: %s", err))
		}

		err = pullSocket.Close()

		if err != nil {
			logger.Fatal(fmt.Sprintf("Error Closing Pull Socker ; %s", err))
		}

		err = zmqContext.Term()

		if err != nil {
			logger.Fatal(fmt.Sprintf("Error terminating ZMQ Context: %s", err))
		}

	}(pushSocket, pullSocket, zmqContext)

	pushAddress := fmt.Sprintf("tcp://%s:%d", config.PublisherHost, config.PushPort)

	err = pushSocket.Connect(pushAddress)

	if err != nil {
		logger.Fatal(fmt.Sprintf("Error binding PUSH socket: %v", err))
		return
	}

	logger.Info(fmt.Sprintf("Push Socket Connected to %s address", pushAddress))

	logger.Info("ZeroMQ socket connected and listening...")

	logger.Info("Plugin engine initialized...")

	zmqrecv := make(chan string)

	zmqsend := make(chan string)

	go func(socket *zmq.Socket, zmqrecv chan string) {
		for {

			data, err := socket.Recv(0)

			if err != nil {
				logger.Fatal(fmt.Sprintf("Error receiving contexts: %s", err))

				continue
			}

			zmqrecv <- data
		}

	}(pullSocket, zmqrecv)

	go func(socket *zmq.Socket, zmqsend chan string) {
		for {
			data := <-zmqsend
			_, err := socket.Send(data, 0)
			if err != nil {
				logger.Fatal(fmt.Sprintf("Error Sending Result to Main App %s", err))
			}
		}
	}(pushSocket, zmqsend)

	for {
		select {
		case data := <-zmqrecv:

			go processor.ProcessContext(data, zmqsend)

		}

	}
}
