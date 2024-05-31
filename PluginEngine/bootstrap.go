package main

import (
	"fmt"
	zmq "github.com/pebbe/zmq4"
	"pluginengine/consts"
	"pluginengine/plugin"
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

	// Initialize ZeroMQ Pull socket for receiving messages
	pullSocket, err := zmqContext.NewSocket(zmq.PULL)

	if err != nil {
		logger.Fatal(fmt.Sprintf("Error creating ZeroMQ socket: %s", err))
		return
	}

	// Connect to Pull Socket at Address from Config
	pullAddress := fmt.Sprintf("tcp://%s:%d", config.PublisherHost, config.PullPort)

	err = pullSocket.Connect(pullAddress)

	if err != nil {
		logger.Fatal(fmt.Sprintf("Error binding ZeroMQ socket: %s", err))
		return
	}

	logger.Info(fmt.Sprintf("Push Socket Connected to %s address", pullAddress))

	// Create PUSH socket and connect it to the desired address
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

	zmqrecv := make(chan string, 3)

	zmqsend := make(chan string, 3)

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
			select {
			case data := <-zmqsend:
				_, err := socket.Send(data, 0)
				if err != nil {
					logger.Fatal(fmt.Sprintf("Error Sending Result to Main App %s", err))
				}
			}
		}
	}(pushSocket, zmqsend)

	for {
		select {
		case data := <-zmqrecv:

			go func(data string, zmqsend chan string) {

				contexts, err := utils.Decode(data)

				// Error in decoding the context
				if err != nil {
					logger.Fatal(fmt.Sprintf("Error decoding context: %s", err))
					return
				}

				internalChannel := make(chan map[string]interface{}, len(contexts))

				defer close(internalChannel)

				contextsLength := len(contexts)

				for _, context := range contexts {

					logger.Info(fmt.Sprintf("Context: %s", context))

					// if invalid Request Type -> decrease the count till which we are waiting in for select

					if context[consts.RequestType] == consts.DISCOVERY {

						go plugin.Discover(context, internalChannel)

					} else if context[consts.RequestType] == consts.POLLING {

						go plugin.Collect(context, internalChannel)

					} else {
						// Decrement Counter and also print this error and add the unique identifier
						errors := make([]map[string]interface{}, 0)

						errors = append(errors, utils.Error("Invalid Request Type", consts.InvalidRequestTypeErrorCode))

						context[consts.ERROR] = errors

						context[consts.STATUS] = consts.FAILED

						context[consts.RESULT] = make([]map[string]interface{}, 0)

						// Encode and Print on Command Line
						internalChannel <- context
					}
				}

				for contextsLength > 0 {
					select {

					case result := <-internalChannel:

						encodedResult, _ := utils.Encode(result)

						zmqsend <- encodedResult

						contextsLength--
					}
				}

			}(data, zmqsend)
		}

	}
}
