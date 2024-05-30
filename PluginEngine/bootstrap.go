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
	config, err := utils.ReadConfig("../Config/config.json")

	if err != nil {
		fmt.Printf("Error reading config file: %s\n", err)
		os.Exit(1)
	}

	// Set Up Logger
	logger := utils.NewLogger("bootstrap", "gobootstrap")

	logger.Info("Plugin engine initialized...")

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

	// Initialize ZeroMQ socket for receiving messages
	socket, err := zmqContext.NewSocket(zmq.REP)

	if err != nil {
		logger.Fatal(fmt.Sprintf("Error creating ZeroMQ socket: %s", err))
	}

	defer func(socket *zmq.Socket) {
		err := socket.Close()
		if err != nil {
			logger.Fatal(fmt.Sprintf("Error closing ZeroMQ socket: %s", err))
		}
	}(socket)

	// Bind socket to ports 5989 and 5990
	err = socket.Bind(fmt.Sprintf("tcp://%s:%s", config.Host, config.Port))
	if err != nil {
		logger.Fatal(fmt.Sprintf("Error binding ZeroMQ socket: %s", err))
	}

	logger.Info("ZeroMQ socket bound and listening...")

	// Decode the received context from command line argument
	contexts, err := utils.Decode(os.Args[1])

	// Error in decoding the context
	if err != nil {
		logger.Fatal(fmt.Sprintf("Error decoding context: %s", err))

		context := make(map[string]interface{}, 1)

		errors := make([]map[string]interface{}, 0)

		errors = append(errors, utils.Error(consts.DecodeErrorCode, err.Error()))

		context[consts.STATUS] = consts.FAILED

		context[consts.RESULT] = make([]map[string]interface{}, 0)

		result, _ := utils.Encode(context)

		fmt.Println(result)

		return
	}

	channel := make(chan map[string]interface{}, len(contexts))

	defer close(channel)

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

			fmt.Println(encodedResult)

			fmt.Println(consts.UniqueSeparator)

			contextsLength--

		}
	}

}
