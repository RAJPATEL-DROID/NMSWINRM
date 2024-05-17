package main

import (
	"fmt"
	"os"
	"pluginengine/consts"
	"pluginengine/plugin"
	"pluginengine/utils"
)

func main() {

	// Set Up Logger
	logger := utils.NewLogger("bootstrap", "gobootstrap")

	logger.Info("Plugin engine initialized...")

	// If Context is not received in the argument
	if len(os.Args) == 1 {
		// error
		logger.Fatal(fmt.Sprintf("No context is passed"))

		context := make(map[string]interface{}, 1)

		errors := make([]map[string]interface{}, 0)

		errors = append(errors, utils.Error(consts.ContextMissingCode, consts.ContextMissingError))

		context[consts.ERROR] = errors

		context[consts.STATUS] = consts.FAILED

		context[consts.RESULT] = make([]map[string]interface{}, 0)

		result, err := utils.Encode(context)
		if err != nil {

			logger.Fatal(fmt.Sprintf("Error while encoding context: %v", err))

			errors = append(errors, utils.Error(consts.JsonErrorCode, err.Error()))

			fmt.Println(context)

			return
		}

		fmt.Println(result)

		return
	}

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
