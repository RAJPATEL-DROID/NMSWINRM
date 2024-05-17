package clients

import (
	"context"
	"fmt"
	"github.com/masterzen/winrm"
	"pluginengine/consts"
	"pluginengine/utils"
	"time"
)

type WinRmClient struct {
	ip string

	password string

	username string

	port int

	timeout time.Duration

	logger utils.Logger
}

func (client *WinRmClient) SetContext(context map[string]interface{}) {

	client.logger = utils.NewLogger("client", "winrm")

	client.username = utils.ToString(context[consts.UNAME])

	client.password = utils.ToString(context[consts.PASSWORD])

	client.ip = utils.ToString(context[consts.IP])

	if context[consts.DevicePort] != nil {

		client.port = int(context[consts.DevicePort].(float64))

	} else {

		client.port = consts.DefaultPort

	}

	if context[consts.TimeOut] != nil {
		if timeOut, ok := context[consts.TimeOut].(float64); ok {

			context[consts.TimeOut] = time.Duration(timeOut * float64(time.Second))

		}

	} else {
		client.timeout = consts.DefaultTimeOut * time.Second
	}
}

func (client *WinRmClient) EstablishWinRMConnection() (*winrm.Client, error) {

	// Create a new WinRM client
	endpointConfig := winrm.NewEndpoint(client.ip, client.port, false, true, nil, nil, nil, client.timeout)

	client.logger.Info("Creating winRM connection")

	winRmClient, err := winrm.NewClient(endpointConfig, client.username, client.password)

	if err != nil {

		client.logger.Error(fmt.Sprintf("Failed to create WinRM client: %v\n", err))

		return nil, err

	}

	return winRmClient, nil
}

func (client *WinRmClient) ExecuteCommand(winRmClient *winrm.Client, command string) (string, string, int, error) {

	output, errorOutput, errorCode, err := winRmClient.RunPSWithContext(context.Background(), command)

	return output, errorOutput, errorCode, err

}
