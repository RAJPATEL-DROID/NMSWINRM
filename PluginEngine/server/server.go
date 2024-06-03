package server

import (
	"fmt"
	zmq "github.com/pebbe/zmq4"
	. "pluginengine/globals"
	"pluginengine/utils"
)

func Start() (receiver *zmq.Socket, sender *zmq.Socket, err error) {

	logger := utils.NewLogger("server", "StartZMQ")

	// Initialize ZeroMQ context
	zmqContext, err := zmq.NewContext()
	if err != nil {
		logger.Fatal(fmt.Sprintf("Error creating ZeroMQ context: %s", err))
		return nil, nil, err
	}

	pull, err := zmqContext.NewSocket(zmq.PULL)

	if err != nil {
		logger.Fatal(fmt.Sprintf("Error creating ZeroMQ socket: %s", err))
		return nil, nil, err
	}

	push, err := zmqContext.NewSocket(zmq.PUSH)

	if err != nil {
		logger.Fatal(fmt.Sprintf("Error creating PUSH socket: %v", err))
		return nil, nil, err
	}

	return pull, push, nil

}

func Connect(pull *zmq.Socket, push *zmq.Socket, config utils.Config) {

	go receiver(pull, config)

	go sender(push, config)

}

func sender(push *zmq.Socket, config utils.Config) {

	logger := utils.NewLogger("server", "Sender")

	pushHost := fmt.Sprintf("tcp://%s:%d", config.PublisherHost, config.Push)

	err := push.Connect(pushHost)

	if err != nil {
		logger.Fatal(fmt.Sprintf("Error binding PUSH socket: %v", err))
		return
	}

	logger.Info(fmt.Sprintf("Push Socket Connected to %s address", pushHost))

	for {
		data := <-Sender

		_, err := push.Send(data, 0)

		if err != nil {

			logger.Fatal(fmt.Sprintf("Error Sending Result to Main App %s", err))

			continue

		}
	}

}

func receiver(pull *zmq.Socket, config utils.Config) {

	logger := utils.NewLogger("server", "Receiver")

	pullHost := fmt.Sprintf("tcp://%s:%d", config.PublisherHost, config.Pull)

	err := pull.Connect(pullHost)

	if err != nil {
		logger.Fatal(fmt.Sprintf("Error binding ZeroMQ socket: %s", err))
		return
	}

	logger.Info(fmt.Sprintf("Push Socket Connected to %s address", pullHost))

	for {

		data, err := pull.Recv(0)

		if err != nil {
			logger.Fatal(fmt.Sprintf("Error receiving contexts: %s", err))

			continue
		}

		if data != "" {

			Receiver <- data
		}
	}
}
