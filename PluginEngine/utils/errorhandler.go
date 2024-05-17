package utils

import "pluginengine/consts"

//func Error(context map[string]interface{}, errorCode string, errMessage string) {
//
//	var errorArray []map[string]interface{}
//
//	error := make(map[string]interface{})
//
//	error[consts.ERROR] = errMessage
//
//	error[consts.ErrorCode] = errorCode
//
//	error[consts.ErrorMessage] = errMessage
//
//	errorArray = append(errorArray, error)
//
//	context[consts.ERROR] = errorArray
//
//	context[consts.STATUS] = consts.FAILED
//
//	return
//}

func Error(errorMessage string, errorCode string) map[string]interface{} {

	errorDetails := make(map[string]interface{})

	errorDetails[consts.ErrorCode] = errorCode

	errorDetails[consts.ErrorMessage] = errorMessage

	return errorDetails

}
