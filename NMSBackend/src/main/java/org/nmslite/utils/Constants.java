package org.nmslite.utils;

public class Constants
{
    public static final String RUN_API_RESULT = "/run/result/:id";

    private Constants() {
        throw new IllegalStateException("Constant class");
    }

    public static final String ROUTE_PATH="/";

    public static final String CREDENTIAL_ROUTE ="/credential/*";

    public static final String PARAMETER ="/:id";

    public static final String DISCOVERY_ROUTE ="/discovery/*";

    public static final String RUN ="/run/:id";

    public static final String PROVISION_ROUTE ="/provision/*";

    public static final String POLL_TIME = "poll.time";

    public static final String PLUGIN_PROCESS_TIMEOUT =   "plugin.process.timeout";

    public static final Integer OK = 200;

    public static final Integer BAD_REQUEST = 400;

    public static final Integer SUCCESS_CODE = 000;

    public static final Integer INVALID_CREDENTIALS = -1;

    public static final String ID = "id";

    public static final String HTTP_PORT = "http.port";

    public static final String HOST = "http.hostname";

    public static final String USER_DIRECTORY = "user.dir";

    public static final String CONFIG_FILE = "/config/config.json";

    public static final String CONFIG_PATH = System.getProperty(USER_DIRECTORY) + CONFIG_FILE;

    public static final String CREDENTIAL_ID = "credential.id";

    public static final String CREDENTIAL_IDS = "credential.ids";

    public static final String USERNAME = "username";

    public static final String PASSWORD = "password";

    public static final String NAME = "name";

    public static final String DISCOVERY = "discovery";

    public static final String DISCOVERY_ID = "discovery.id";

    public static final String DISCOVERY_IDS = "discovery.ids";

    public static final String DISCOVERY_DATA = "discovery.data";

    public static final String IP = "ip";

    public static final String DEVICE_PORT = "device.port";

    public static final String POLLING = "polling";

    public static final String CREDENTIAL_PROFILES = "credential.profiles";

    public static final String CONTEXT = "context";

    public static final String PROVISION_DEVICES = "provision.devices";

    public static final String RESULT = "result";

    public static final String STATUS = "status";

    public static final String SUCCESS = "success";

    public static final String FAILED = "failed";

    public static final String MESSAGE = "message";

    public static final String ERRORS = "errors";

    public static final String ERROR = "error";

    public static final String MISSING_FIELD = "Fields are Missing in the Request !!";

    public static final String ERROR_CODE = "error.code";

    public static final String ERROR_MESSAGE = "error.message";

    public static final String REQUEST_TYPE = "request.type";

    public static final String EVENT_RUN_DISCOVERY = "event.run.discovery";

    public static final String PLUGIN_APPLICATION_PATH = "/PluginEngine/bootstrap";

    public static final String FILE_PATH = "/home/raj/Work/NMSWINRMLITE/NMSBackend/Result/";

    public static final String INVALID_REQUEST_TYPE = "Invalid Request Type";

    public static final String UNIQUE_SEPARATOR  = "~@@~";

    public static final String PUBLISHER_PORT = "zmq.publisher.port";

    public static final String PUSH_PORT = "zmq.push.port";

    public static final String ZMQ_ADDRESS  = "tcp://*:";

    public static final String RECEIVER_PORT = "receiver.zmq.port";

    public static final String NUM_OF_ROWS = "num.of.rows";
}