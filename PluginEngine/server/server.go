package server

import (
	"fmt"
	zmq "github.com/pebbe/zmq4"
	. "pluginengine/consts"
	"pluginengine/utils"
)

func Start() (receiver *zmq.Socket, sender *zmq.Socket, err error) {

	logger := utils.NewLogger("server", "start")

	defer func() {
		if err := recover(); err != nil {
			logger.Fatal(fmt.Sprintf("Panic Recovery in StartZMQ: %v", err))
		}

	}()

	// Initialize ZeroMQ context
	zmqContext, err := zmq.NewContext()
	if err != nil {

		logger.Fatal(fmt.Sprintf("Error creating ZeroMQ context: %s", err))

		return
	}

	receiver, err = zmqContext.NewSocket(zmq.PULL)

	if err != nil {

		logger.Fatal(fmt.Sprintf("Error creating ZeroMQ socket: %s", err))

		return
	}

	sender, err = zmqContext.NewSocket(zmq.PUSH)

	if err != nil {

		logger.Fatal(fmt.Sprintf("Error creating PUSH socket: %v", err))

		return
	}

	return

}

func Connect(config utils.Config) (err error) {

	logger := utils.NewLogger("server", "connect")

	defer func() {
		if err := recover(); err != nil {
			logger.Fatal(fmt.Sprintf("Panic Recovery in Connect Method: %v", err))
		}
	}()

	pull, push, err := Start()

	if err != nil {
		logger.Fatal(fmt.Sprintf("Error starting server: %v", err))
		return
	}

	go receive(pull, config)

	go send(push, config)

	logger.Info("ZMQ Receiver and Sender Started...")

	return
}

func receive(pull *zmq.Socket, config utils.Config) {

	logger := utils.NewLogger("server", "Receiver")

	defer func() {
		if err := recover(); err != nil {
			logger.Fatal(fmt.Sprintf("Panic Recovery in receive: %v", err))

		}
		receive(pull, config)
	}()

	pullHost := fmt.Sprintf("tcp://%s:%d", config.PublisherHost, config.Pull)

	err := pull.Connect(pullHost)

	if err != nil {
		logger.Fatal(fmt.Sprintf("Error binding ZeroMQ socket: %s", err))
		return
	}

	logger.Info(fmt.Sprintf("Pull Socket Connected to %s address", pullHost))

	for {

		data, err := pull.Recv(0)

		if err != nil {

			logger.Fatal(fmt.Sprintf("Error receiving contexts: %s", err))

			break
		}

		if data != "" {
			Receiver <- data
		}
	}
}

func send(push *zmq.Socket, config utils.Config) {

	logger := utils.NewLogger("server", "Sender")

	defer func() {
		if err := recover(); err != nil {
			logger.Fatal(fmt.Sprintf("Panic Recovery in receive: %v", err))

		}
		send(push, config)
	}()

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
