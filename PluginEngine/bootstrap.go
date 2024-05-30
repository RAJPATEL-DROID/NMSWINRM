package main

import (
	"fmt"
	zmq "github.com/pebbe/zmq4"
	"os"
	"pluginengine/consts"
	"pluginengine/plugin"
	"pluginengine/utils"
)

func main() {

	// Read configuration from config.json
	config, err := utils.ReadConfig("../Config/pluginConfig.json")

	if err != nil {
		fmt.Printf("Error reading config file: %s\n", err)
		os.Exit(1)
	}

	// Set Up Logger
	logger := utils.NewLogger("bootstrap", "gobootstrap")

	//// If Context is not received in the argument
	//if len(os.Args) == 1 {
	//	// error
	//	logger.Fatal(fmt.Sprintf("No context is passed"))
	//
	//	context := make(map[string]interface{}, 1)
	//
	//	errors := make([]map[string]interface{}, 0)
	//
	//	errors = append(errors, utils.Error(consts.ContextMissingCode, consts.ContextMissingError))
	//
	//	context[consts.ERROR] = errors
	//
	//	context[consts.STATUS] = consts.FAILED
	//
	//	context[consts.RESULT] = make([]map[string]interface{}, 0)
	//
	//	result, err := utils.Encode(context)
	//	if err != nil {
	//
	//		logger.Fatal(fmt.Sprintf("Error while encoding context: %v", err))
	//
	//		errors = append(errors, utils.Error(consts.JsonErrorCode, err.Error()))
	//
	//		fmt.Println(context)
	//
	//		return
	//	}
	//
	//	fmt.Println(result)
	//
	//	return
	//}

	// Initialize ZeroMQ context
	zmqContext, err := zmq.NewContext()
	if err != nil {
		logger.Fatal(fmt.Sprintf("Error creating ZeroMQ context: %s", err))
	}
	defer func(zmqContext *zmq.Context) {
		err := zmqContext.Term()
		if err != nil {
			logger.Fatal(fmt.Sprintf("Error terminating ZeroMQ context: %s", err))
		}
	}(zmqContext)

	// Initialize ZeroMQ Pull socket for receiving messages
	pullSocket, err := zmqContext.NewSocket(zmq.PULL)

	if err != nil {
		logger.Fatal(fmt.Sprintf("Error creating ZeroMQ socket: %s", err))
	}

	defer func(socket *zmq.Socket) {
		err := socket.Close()
		if err != nil {
			logger.Fatal(fmt.Sprintf("Error closing ZeroMQ socket: %s", err))
		}
	}(pullSocket)

	// Connect to Pull Socket at Address from Config
	pullAddress := fmt.Sprintf("tcp://%s:%d", config.PublisherHost, config.PullPort)

	err = pullSocket.Connect(pullAddress)

	if err != nil {
		logger.Fatal(fmt.Sprintf("Error binding ZeroMQ socket: %s", err))
	}

	logger.Info(fmt.Sprintf("Push Socket Connected to %s address", pullAddress))

	// Create PUSH socket and connect it to the desired address
	pushSocket, err := zmqContext.NewSocket(zmq.PUSH)
	if err != nil {
		logger.Fatal(fmt.Sprintf("Error creating PUSH socket: %v", err))
	}
	defer func(socket *zmq.Socket) {
		err := socket.Close()
		if err != nil {
			logger.Fatal(fmt.Sprintf("Error closing ZeroMQ socket: %s", err))
		}
	}(pushSocket)

	pushAddress := fmt.Sprintf("tcp://%s:%d", config.PublisherHost, config.PushPort)

	err = pushSocket.Connect(pushAddress)

	if err != nil {
		logger.Fatal(fmt.Sprintf("Error binding PUSH socket: %v", err))
	}

	logger.Info(fmt.Sprintf("Push Socket Connected to %s address", pushAddress))

	logger.Info("ZeroMQ socket connected and listening...")

	logger.Info("Plugin engine initialized...")

	channel := make(chan map[string]interface{})

	defer close(channel)
	// TODO : Receive Context Array From Socket
	for {
		data, err := pullSocket.Recv(0)

		if err != nil {
			logger.Fatal(fmt.Sprintf("Error receiving contexts: %s", err))

			continue
		}

		contexts, err := utils.Decode(data)

		// Error in decoding the context
		if err != nil {
			logger.Fatal(fmt.Sprintf("Error decoding context: %s", err))

			continue
		}

		channel = make(chan map[string]interface{}, len(contexts))

		contextsLength := len(contexts)

		for _, context := range contexts {

			logger.Info(fmt.Sprintf("Context: %s", context))

			// if invalid Request Type -> decrease the count till which we are waiting in for select

			if context[consts.RequestType] == consts.DISCOVERY {

				go plugin.Discover(context, channel)

			} else if context[consts.RequestType] == consts.POLLING {

				go plugin.Collect(context, channel)

			} else {
				// Decrement Counter and also print this error and add the unique identifier
				errors := make([]map[string]interface{}, 0)

				errors = append(errors, utils.Error("Invalid Request Type", consts.InvalidRequestTypeErrorCode))

				context[consts.ERROR] = errors

				context[consts.STATUS] = consts.FAILED

				context[consts.RESULT] = make([]map[string]interface{}, 0)

				// Encode and Print on Command Line
				channel <- context

			}
		}

		for contextsLength > 0 {
			select {
			case result := <-channel:

				encodedResult, _ := utils.Encode(result)

				pushSocket.Send(encodedResult, 0)

				//fmt.Println(encodedResult)
				//
				//fmt.Println(consts.UniqueSeparator)

				contextsLength--

			}
		}

	}

	//// Decode the received context from command line argument
	//contexts, err := utils.Decode(os.Args[1])
	//
	//// Error in decoding the context
	//if err != nil {
	//	logger.Fatal(fmt.Sprintf("Error decoding context: %s", err))
	//
	//	context := make(map[string]interface{}, 1)
	//
	//	errors := make([]map[string]interface{}, 0)
	//
	//	errors = append(errors, utils.Error(consts.DecodeErrorCode, err.Error()))
	//
	//	context[consts.STATUS] = consts.FAILED
	//
	//	context[consts.RESULT] = make([]map[string]interface{}, 0)
	//
	//	result, _ := utils.Encode(context)
	//
	//	fmt.Println(result)
	//
	//	return
	//}

}
