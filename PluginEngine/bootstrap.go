package main

import (
	"fmt"
	"pluginengine/consts"
	. "pluginengine/globals"
	"pluginengine/plugin"
	"pluginengine/server"
	"pluginengine/utils"
)

func main() {
	// Set Up Logger
	logger := utils.NewLogger("bootstrap", "gobootstrap")

	// Read configuration from config.json
	config, err := utils.ReadConfig("../config/context.json")

	if err != nil {
		logger.Fatal(fmt.Sprintf("Error reading config file: %s\n", err))
		return
	}

	// Create ZMQ Context and Create Socket for PUSH-PULL Communication
	err = server.Connect(config)

	if err != nil {
		logger.Fatal(fmt.Sprintf("Error connecting to server: %s\n", err))
		return
	}

	logger.Info("Plugin engine initialized...")

	for {
		select {
		case data := <-Receiver:

			go func(data string, sender chan string) {

				contexts, err := utils.Decode(data)

				logger := utils.NewLogger("bootstrap", "contextprocessor")

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

						internalChannel <- context
					}
				}

				for contextsLength > 0 {

					result := <-internalChannel

					encodedResult, _ := utils.Encode(result)

					sender <- encodedResult

					contextsLength--
				}

				return

			}(data, Sender)

		}

	}
}
