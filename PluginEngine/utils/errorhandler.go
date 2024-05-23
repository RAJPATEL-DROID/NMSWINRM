package utils

import "pluginengine/consts"

func Error(errorMessage string, errorCode string) map[string]interface{} {

	errorDetails := make(map[string]interface{})

	errorDetails[consts.ErrorCode] = errorCode

	errorDetails[consts.ErrorMessage] = errorMessage

	return errorDetails

}
