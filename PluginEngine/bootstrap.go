package main

import (
	"fmt"
	. "pluginengine/consts"
	"pluginengine/plugin"
	"pluginengine/server"
	"pluginengine/utils"
)

func main() {

	logger := utils.NewLogger("bootstrap", "gobootstrap")

	// Read configuration from config.json
	config, err := utils.ReadConfig(ConfigPath)

	if err != nil {
		logger.Error(fmt.Sprintf("Error reading config file: %s\n", err))
		return
	}

	defer func() {
		if err := recover(); err != nil {
			logger.Error(fmt.Sprintf("Error in Starting PluginEngine : %s\n", err))
			return
		}
	}()

	// Create ZMQ Context and Create Socket for PUSH-PULL Communication
	// Initialise Go Routines for Receiving and Sending the Messages via ZMQ
	err = server.Connect(config)

	if err != nil {
		logger.Fatal(fmt.Sprintf("Error connecting to server: %s\n", err))
		return
	}

	logger.Info("Plugin engine initialized...")

	process()
}

func process() {

	logger := utils.NewLogger("bootstrap", "process")

	defer func() {
		if err := recover(); err != nil {
			logger.Error(fmt.Sprintf("Error in Processing Context : %s\n", err))

			process()
		}
	}()

	for {
		data := <-Receiver

		//processor.ProcessContext(data, zmqsend)
		contexts, err := utils.Decode(data)

		logger := utils.NewLogger("processor", "processContext")

		// Error in decoding the context
		if err != nil {
			logger.Fatal(fmt.Sprintf("Error decoding context: %s", err))
			return
		}

		for _, context := range contexts {

			logger.Info(fmt.Sprintf("Context: %s", context))

			// if invalid Request Type -> decrease the count till which we are waiting in for select

			if context[RequestType] == DISCOVERY {

				go plugin.Discover(context)

			} else if context[RequestType] == POLLING {

				go plugin.Collect(context)

			} else {
				// Decrement Counter and also print this error and add the unique identifier
				errors := make([]map[string]interface{}, 0)

				errors = append(errors, utils.Error("Invalid Request Type", InvalidRequestTypeErrorCode))

				context[ERROR] = errors

				context[STATUS] = FAILED

				context[RESULT] = make([]map[string]interface{}, 0)

				encodedResult, _ := utils.Encode(context)

				Sender <- encodedResult
			}
		}
	}
}
