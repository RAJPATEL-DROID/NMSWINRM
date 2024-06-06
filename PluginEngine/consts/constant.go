package consts

const (
	DefaultPort int = 5985

	DefaultTimeOut = 60

	IP = "ip"

	UNAME = "username"

	PASSWORD = "password"

	CredentialProfiles = "credential.profiles"

	InvalidCredentials = -1

	CredentialID = "credential.id"

	DevicePort = "port"

	RequestType = "request.type"

	TimeOut = "request.timeout.nanosec"

	DISCOVERY = "discovery"

	POLLING = "polling"

	ERROR = "errors"

	ErrorMessage = "error.message"

	ErrorCode = "error.code"

	STATUS = "status"

	SUCCESS = "success"

	FAILED = "failed"

	InvalidRequestTypeErrorCode = "ERR004"

	ConnectionError = "CONNECTION01"

	ExecuteError = "Execute01"

	RESULT = "result"

	HostName = "system.host.name"
)

var (
	Sender = make(chan string)

	Receiver = make(chan string)
)
