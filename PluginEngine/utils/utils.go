package utils

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"os"
	"strconv"
)

func Decode(encodedString string) ([]map[string]interface{}, error) {
	var logger = NewLogger("utils", "Decode")

	decodedBytes, err := base64.StdEncoding.DecodeString(encodedString)

	if err != nil {
		logger.Fatal(fmt.Sprint("Error decoding base64 encoded string : ", err))

		return []map[string]interface{}{}, err
	}

	var jsonContexts []map[string]interface{}

	err = json.Unmarshal(decodedBytes, &jsonContexts)

	if err != nil {

		logger.Fatal(fmt.Sprint("Error unmarshal string to json : ", err))

		return []map[string]interface{}{}, err
	}

	return jsonContexts, nil
}

func Encode(resultMap map[string]interface{}) (string, error) {
	var logger = NewLogger("utils", "Encode")

	jsonBytes, err := json.Marshal(resultMap)

	if err != nil {

		logger.Fatal(fmt.Sprint("Error in Marshalling Map to Json : ", err))

		return "", err
	}

	encodedResult := base64.StdEncoding.EncodeToString(jsonBytes)

	return encodedResult, nil
}

func ToString(data any) string {

	switch data.(type) {

	case string:

		return data.(string)

	case int:

		return strconv.Itoa(data.(int))

	case float64:

		return strconv.FormatFloat(data.(float64), 'f', -1, 64)

	case bool:

		return strconv.FormatBool(data.(bool))

	case []interface{}:

		return ToString(data.([]interface{}))

	case map[string]interface{}:

		return ToString(data.(map[string]interface{}))

	case nil:

		return "null"
	default:

		return fmt.Sprintf("%v", data)
	}
}

// Config struct to hold configuration values
type Config struct {
	PublisherHost string `json:"host.ip"`
	Push          int    `json:"zmq.push.port"`
	Pull          int    `json:"zmq.pull.port"`
}

func ReadConfig(filename string) (Config, error) {
	var logger = NewLogger("utils", "readConfig")

	var config Config
	file, err := os.Open(filename)
	if err != nil {

		logger.Fatal(fmt.Sprint("Error opening file : ", err))

		return config, err
	}
	defer file.Close()

	decoder := json.NewDecoder(file)
	err = decoder.Decode(&config)
	if err != nil {

		logger.Fatal(fmt.Sprint("Error decoding file : ", err))

		return config, err
	}

	return config, nil
}

var MetricsMap = map[string]string{
	"system.cpu.idle.percent":            "Count",
	"system.cpu.interrupt.percent":       "Count",
	"system.cpu.user.percent":            "String",
	"system.cpu.percent":                 "Count",
	"system.cpu.description":             "String",
	"systems.cpu.type":                   "String",
	"system.cpu.cores":                   "Count",
	"system.disk.free.bytes":             "Count",
	"system.disk.io.ops.per.second":      "Count",
	"system.disk.io.idle.time.percent":   "Count",
	"system.disk.io.read.bytes.per.sec":  "Count",
	"system.disk.io.queue.length":        "Count",
	"system.disk.io.read.ops.per.sec":    "Count",
	"system.disk.io.write.bytes.per.sec": "Count",
	"system.disk.used.bytes":             "Count",
	"system.disk.io.time.percent":        "Count",
	"system.disk.io.write.ops.per.sec":   "Count",
	"system.disk.io.bytes.per.sec":       "Count",
	"system.disk.capacity.bytes":         "Count",
	"system.disk.free.percent":           "Count",
	"system.disk.used.percent":           "Count",
	"system.serial.number":               "String",
	"started.time.seconds":               "Count",
	"system.logical.processors":          "Count",
	"system.virtual":                     "String",
	"system.os.name":                     "String",
	"system.model":                       "String",
	"system.os.service.pack":             "String",
	"system.interrupts.per.sec":          "Count",
	"system.vendor":                      "String",
	"system.name":                        "String",
	"system.threads":                     "Count",
	"system.processor.queue.length":      "Count",
	"started.time":                       "String",
	"system.os.version":                  "String",
	"system.physical.processors":         "Count",
	"system.running.processes":           "Count",
	"system.context.switches.per.sec":    "Count",
	"system.network.output.queue.length": "Count",
	"system.network.out.packets.per.sec": "Count",
	"system.network.bytes.per.sec":       "Count",
	"system.network.tcp.retransmissions": "Count",
	"system.network.out.bytes.per.sec":   "Count",
	"system.network.error.packets":       "Count",
	"system.network.in.packets.per.sec":  "Count",
	"system.network.in.bytes.per.sec":    "Count",
	"system.network.tcp.connections":     "Count",
	"memory.free.percent":                "Count",
	"system.cache.memory.bytes":          "Count",
	"memory.used.percent":                "Count",
	"memory.used.bytes":                  "Count",
	"memory.committed.bytes":             "Count",
	"system.memory.installed.bytes":      "Count",
	"system.memory.free.bytes":           "Count",
	"system.pages.per.sec":               "Count",
	"system.pages.faults.per.sec":        "Count",
	"system.non.paged.memory.bytes":      "Count",
	"system.paged.memory.bytes":          "Count",
	"system.memory.available.bytes":      "Count",
}
