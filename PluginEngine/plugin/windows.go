package plugin

import (
	"fmt"
	"log"
	"maps"
	"pluginengine/clients"
	"pluginengine/consts"
	"pluginengine/utils"
	"strconv"
	"strings"
)

func Discover(context map[string]interface{}) {
	defer func() {
		if err := recover(); err != nil {
			log.Println("error in discovery: ", err)

			context[consts.CredentialID] = consts.InvalidCredentials

			context[consts.STATUS] = consts.FAILED

			encodedResult, _ := utils.Encode(context)

			consts.Sender <- encodedResult

			//zmqsend <- context
		}
	}()

	logger := utils.NewLogger("plugin", "discovery")

	result := make(map[string]interface{})

	client := clients.WinRmClient{}

	if credentials, ok := context[consts.CredentialProfiles].([]interface{}); ok {

		for _, credential := range credentials {

			if creds, ok := credential.(map[string]interface{}); ok {

				creds[consts.IP] = context[consts.IP]

				creds[consts.DevicePort] = context[consts.DevicePort]

				creds[consts.TimeOut] = context[consts.TimeOut]

				client.SetContext(creds)

				delete(creds, consts.DevicePort)

				delete(creds, consts.TimeOut)

				delete(creds, consts.IP)
			}

			connectionContext, err := client.EstablishWinRMConnection()

			if err == nil {

				// RUN HOSTNAME command
				command := "Write-Output (\"system.host.name:\" + (hostname))"

				// Execute Command
				output, errorOutput, exitCode, err := client.ExecuteCommand(connectionContext, command)

				output = strings.Trim(output, "\r\n")

				if err != nil || exitCode != 0 {
					if err == nil {

						logger.Error(fmt.Sprint("Error Code : ", exitCode))
						logger.Error(fmt.Sprint("Error in executing command :", errorOutput))

					} else {

						logger.Error(fmt.Sprint("Error in executing command :", err.Error()))
					}

					continue

				} else {

					if id, ok := credential.(map[string]interface{})[consts.CredentialID]; ok {

						context[consts.CredentialID] = id

					}

					resArray := strings.SplitN(output, ":", 2)

					if resArray[0] == consts.HostName {

						result[consts.HostName] = resArray[1]

					}
					break
				}
			}
		}

		logger.Info(fmt.Sprintf("Discovery Request Successfull : %s ", result))

		if len(result) > 0 {

			context[consts.STATUS] = consts.SUCCESS

			context[consts.RESULT] = result

		} else {

			context[consts.CredentialID] = consts.InvalidCredentials

			context[consts.RESULT] = result

			context[consts.STATUS] = consts.FAILED

		}
	}

	encodedResult, _ := utils.Encode(context)

	consts.Sender <- encodedResult

	logger.Info(fmt.Sprintf("Result Sent TO ZMQ Sender"))

	return
}

const (
	//CPU Metrics
	cpuMetrics = "(Get-Counter -Counter \"\\Processor(_total)\\% Idle Time\") | Select-Object -ExpandProperty CounterSamples |  Select-Object @{Name='system.cpu.idle.percent';Expression={($_.CookedValue)}} | fl;" +
		"(Get-Counter -Counter \"\\Processor(_Total)\\% Interrupt Time\")| Select-Object -ExpandProperty CounterSamples |  Select-Object @{Name='system.cpu.interrupt.percent';Expression={($_.CookedValue)}}  | format-list;" +
		"Write-Output (\"system.cpu.user.percent:\"  + (Get-Counter -Counter \"\\Processor(_Total)\\% User Time\").CounterSamples.CookedValue);" +
		"Write-Output (\"system.cpu.percent:\" + (100- (Get-Counter -Counter \"\\Processor(_Total)\\% Idle Time\").CounterSamples.CookedValue));" +
		"Write-Output (\"system.cpu.description:\" + (Get-WMIObject -Class Win32_Processor).Description);" +
		"Write-Output (\"systems.cpu.type:\" + (Get-WmiObject -Class Win32_Processor).Name);" +
		"Write-Output (\"system.cpu.cores:\" + (Get-WmiObject -Class Win32_Processor | Measure-Object -Property NumberOfCores -Sum).Sum);"

	// Disk Metrics
	diskMetrics2 = "Write-Output (\"system.disk.free.bytes:\" + (Get-WmiObject -Class Win32_LogicalDisk | Where-Object {$_.DeviceID -eq 'C:'} | Select-Object -ExpandProperty FreeSpace));" +
		"Write-Output (\"system.disk.io.ops.per.second:\" + (Get-Counter -Counter \"\\PhysicalDisk(_Total)\\Disk Reads/sec\").CounterSamples.CookedValue);" +
		"Write-Output (\"system.disk.io.idle.time.percent:\" + (Get-Counter -Counter \"\\LogicalDisk(_Total)\\% Idle Time\").CounterSamples.CookedValue);" +
		"Write-Output (\"system.disk.io.read.bytes.per.sec:\"  + (Get-Counter -Counter \"\\PhysicalDisk(_Total)\\Disk Read Bytes/sec\").CounterSamples.CookedValue);" +
		"Write-Output (\"system.disk.io.queue.length:\" + (Get-Counter -Counter \"\\PhysicalDisk(_total)\\Current Disk Queue Length\").CounterSamples.CookedValue);" +
		"Write-Output (\"system.disk.io.read.ops.per.sec:\" +  (Get-Counter -Counter \"\\PhysicalDisk(_Total)\\Disk Reads/sec\").CounterSamples.CookedValue);"

	diskMetrics1 = "(Get-Counter -Counter \"\\PhysicalDisk(*)\\Disk Writes/sec\") |  Select-Object -ExpandProperty CounterSamples  | Measure-Object -Property CookedValue -Sum | Select-Object @{Name='system.disk.io.write.bytes.per.sec';Expression={($_.Sum)}} | fl;" +
		"Get-WmiObject -Class Win32_LogicalDisk |Select-Object DeviceID, @{Name=\"UsedBytes\"; Expression={[math]::Round(($.Size - $.FreeSpace),3)}} |Measure-Object -Property UsedBytes -Sum  | Select-Object @{Name='system.disk.used.bytes';Expression={($_.Sum)}} | fl;" +
		"(Get-Counter -Counter \"\\PhysicalDisk(*)\\Disk Writes/sec\") | Select-Object -ExpandProperty CounterSamples | Measure-Object -Property CookedValue -Sum | Select-Object @{Name='system.disk.io.write.ops.per.sec';Expression={($_.Sum)}} | fl;" +
		"(Get-Counter -Counter \"\\PhysicalDisk(_total)\\Avg. Disk Bytes/Transfer\") | Select-Object -ExpandProperty CounterSamples  | Select-Object @{Name='system.disk.io.bytes.per.sec';Expression={($_.CookedValue)}}  | fl;" +
		"Get-WmiObject -Class Win32_LogicalDisk | Select-Object -Property @{Label='Total'; expression={($_.Size)}} | Measure-Object -Property Total -Sum | Select-Object @{Name='system.disk.capacity.bytes';Expression={($_.Sum)}}|fl;" +
		"$diskInfo = Get-WmiObject Win32_LogicalDisk\n$totalFreeSpace = ($diskInfo | Measure-Object -Property FreeSpace -Sum).Sum\n$totalSize = ($diskInfo | Measure-Object -Property Size -Sum).Sum\n$totalFreePercentage = ($totalFreeSpace / $totalSize) * 100\nWrite-Output \"system.disk.free.percent: $totalFreePercentage\";" +
		"$diskInfo = Get-CimInstance -Class Win32_LogicalDisk | \n    Select-Object  @{Label='Used'; expression={($_.Size - $_.FreeSpace)}},@{Label='Total'; expression={($_.Size)}} | \n    Measure-Object -Property Used,Total -Sum\n\n$usedSum = $diskInfo | Where-Object { $_.Property -eq 'Used' } | Select-Object -ExpandProperty Sum\n$totalSum = $diskInfo | Where-Object { $_.Property -eq 'Total' } | Select-Object -ExpandProperty Sum\n\n$percentageUsed = ($usedSum / $totalSum) * 100\n\nWrite-Output \"system.disk.used.percent: $percentageUsed\";"

	systemMetrics = "Write-Output (\"system.serial.number:\" + (Get-WmiObject Win32_BIOS).SerialNumber);" +
		"Write-Output (\"started.time.seconds:\" + (((get-date)- (gcim Win32_OperatingSystem).LastBootUpTime).totalSeconds));" +
		"Write-Output (\"system.logical.processors:\" + (Get-WmiObject Win32_ComputerSystem).NumberOfLogicalProcessors);" +
		"Write-Output (\"system.virtual:\" +  (Get-WmiObject Win32_ComputerSystem));" +
		"Write-Output (\"system.os.name:\" + (Get-WmiObject Win32_OperatingSystem).Caption);" +
		"Write-Output (\"system.model:\" + (Get-WmiObject Win32_ComputerSystem).Model);" +
		"Write-Output (\"system.os.service.pack:\" + (Get-WMIObject Win32_OperatingSystem).Version);" +
		"Write-Output (\"system.interrupts.per.sec:\" + (Get-Counter -Counter \"\\Processor(_Total)\\Interrupts/sec\").CounterSamples.CookedValue);" +
		"Write-Output (\"system.vendor:\" + (Get-WmiObject -Class Win32_ComputerSystem).Manufacturer);" +
		"Write-Output (\"system.name:\" + (Get-WmiObject -Class Win32_ComputerSystem).Name);" +
		"Write-Output (\"system.threads:\" + (Get-WmiObject -Class Win32_PerfFormattedData_PerfOS_System).Threads);" +
		"Write-Output (\"system.processor.queue.length:\" + (Get-Counter -Counter \"\\System\\Processor Queue Length\").CounterSamples.CookedValue);" +
		"Write-Output (\"started.time: \"  + ((get-date)- (gcim Win32_OperatingSystem).LastBootUpTime));" +
		"Write-Output (\"system.os.version:\" + (Get-WmiObject -Class Win32_OperatingSystem).Version);" +
		"Write-Output (\"system.physical.processors:\" +(Get-WmiObject -Class Win32_ComputerSystem).NumberOfProcessors);" +
		"Write-Output (\"system.running.processes:\" + (Get-Counter -Counter \"\\System\\Processes\").CounterSamples.CookedValue);" +
		"Write-Output (\"system.context.switches.per.sec:\" +  (Get-WmiObject -Class Win32_PerfFormattedData_PerfOS_System).ContextSwitchesPerSec);"

	networkMetrics = "Get-Counter -Counter \"\\Network Interface(*)\\Output Queue Length\" | Select-Object -ExpandProperty CounterSamples |  Measure-Object -Property CookedValue -Sum | Select-Object @{Name='system.network.output.queue.length';Expression={($_.Sum)}}| fl;" +
		"Get-Counter -Counter \"\\Network Interface(*)\\Packets Sent/sec\"  | Select-Object -ExpandProperty CounterSamples |  Measure-Object -Property CookedValue -Sum | Select-Object @{Name='system.network.out.packets.per.sec';Expression={($_.Sum)}} |fl;" +
		"Get-Counter '\\Network Interface(*)\\Bytes Total/sec' | Select-Object -ExpandProperty CounterSamples | Measure-Object -Property CookedValue -Sum | Select-Object @{Name='system.network.bytes.per.sec';Expression={($_.Sum)}} |fl;" +
		"Write-Output (\"system.network.tcp.retransmissions:\" + (Get-Counter -Counter \"\\TCPv4\\Segments Retransmitted/sec\").CounterSamples.CookedValue);" +
		"Get-Counter \"\\Network Interface(*)\\Bytes Sent/sec\" | Select-Object -ExpandProperty CounterSamples | Measure-Object -Property CookedValue -Sum | Select-Object @{Name='system.network.out.bytes.per.sec';Expression={($_.Sum)}} |fl;" +
		"Get-Counter \"\\Network Interface(*)\\Packets Received Errors\" | Select-Object -ExpandProperty CounterSamples | Measure-Object -Property CookedValue -Sum | Select-Object @{Name='system.network.error.packets';Expression={($_.Sum)}} |fl;" +
		"Get-Counter -Counter \"\\Network Interface(*)\\Packets Received/sec\" | Select-Object -expandProperty CounterSamples  | Measure-Object -Property CookedValue -Sum | Select-Object @{Name='system.network.in.packets.per.sec';Expression={($_.Sum)}} |fl;" +
		"(Get-Counter -Counter \"\\Network Interface(*)\\Bytes Received/sec\" | Select-Object -ExpandProperty CounterSamples | Measure-Object -Property CookedValue -Sum | Select-Object @{Name='system.network.in.bytes.per.sec';Expression={($_.Sum)}} | fl);" +
		"Write-Output (\"system.network.tcp.connections:\" +  (Get-Counter -Counter \"\\TCPv4\\Connections Established\").CounterSamples.CookedValue);"

	memoryMetrics = "Write-Output (\"memory.free.percent:\" + ((Get-WmiObject Win32_OperatingSystem).FreePhysicalMemory / (Get-WmiObject Win32_OperatingSystem).TotalVisibleMemorySize)*100);" +
		"Write-Output (\"system.cache.memory.bytes:\" +  (Get-WmiObject Win32_PerfFormattedData_PerfOS_Memory).CacheBytes);" +
		"Write-Output (\"memory.used.percent:\" + ([Math]::Round(((Get-WmiObject Win32_OperatingSystem).TotalVisibleMemorySize - (Get-WmiObject Win32_OperatingSystem).FreePhysicalMemory) / (Get-WmiObject Win32_OperatingSystem).TotalVisibleMemorySize * 100, 2)));" +
		"Write-Output (\"memory.used.bytes:\" + ((Get-WmiObject Win32_OperatingSystem).TotalVisibleMemorySize - (Get-WmiObject Win32_OperatingSystem).FreePhysicalMemory));" +
		"Write-Output (\"memory.committed.bytes:\" + (Get-Counter -Counter \"\\Memory\\Committed Bytes\").CounterSamples.CookedValue);" +
		"Write-Output (\"system.memory.installed.bytes:\" + (Get-WmiObject -Class Win32_ComputerSystem | Select-Object -ExpandProperty TotalPhysicalMemory));" +
		"Write-Output (\"system.memory.free.bytes: \"  + (Get-Counter -Counter \"\\Memory\\Free & Zero Page List Bytes\").CounterSamples.CookedValue);" +
		"Write-Output (\"system.pages.per.sec: \"  + (Get-Counter -Counter \"\\Memory\\Pages/sec\").CounterSamples.CookedValue);" +
		"Write-Output (\"system.pages.faults.per.sec:\"  + (Get-Counter -Counter \"\\Memory\\Page Faults/sec\").CounterSamples.CookedValue);" +
		"Write-Output (\"system.non.paged.memory.bytes:\" + (Get-Counter -Counter \"\\Memory\\Pool Nonpaged Bytes\").CounterSamples.CookedValue);" +
		"Write-Output (\"system.paged.memory.bytes:\" + (Get-Counter -Counter \"\\Memory\\Pool Paged Bytes\").CounterSamples.CookedValue);" +
		"(Get-Counter -Counter \"\\Memory\\Available Bytes\") | Select-Object -ExpandProperty CounterSamples |  Select-Object @{Name='system.memory.available.bytes';Expression={($_.CookedValue)}} |fl;"
)

func Collect(context map[string]interface{}) {

	commands := []string{memoryMetrics, cpuMetrics, diskMetrics2, diskMetrics1, systemMetrics, networkMetrics}

	length := len(commands)

	resultChannel := make(chan map[string]interface{}, length)

	defer func() {
		if err := recover(); err != nil {
			log.Println("error in discovery: ", err)

			context[consts.CredentialID] = consts.InvalidCredentials

			context[consts.STATUS] = consts.FAILED

			encodedResult, _ := utils.Encode(context)

			consts.Sender <- encodedResult

			//zmqsend <- context
		}

		close(resultChannel)
	}()

	logger := utils.NewLogger("plugin", "polling")

	logger.Info("Inside the Collect method")

	for _, commands := range commands {

		client := clients.WinRmClient{}

		client.SetContext(context)

		//Establish Connection
		connectionContext, err := client.EstablishWinRMConnection()

		if err != nil {

			context[consts.ERROR] = utils.Error("Failed to create a winRm Client", consts.ConnectionError)

			resultChannel <- context

		}

		go func(context map[string]interface{}, commands string, commandsResult chan map[string]interface{}) {

			results := make(map[string]interface{})

			output, errorOutput, exitCode, err := client.ExecuteCommand(connectionContext, commands)

			if err != nil {

				logger.Error(fmt.Sprint("Error while making connection :  ", err))

				results[consts.ERROR] = utils.Error(err.Error(), consts.ConnectionError)

			} else if exitCode != 0 {

				logger.Error(fmt.Sprintf("Error while making executing command %v\n", commands))

				results[consts.ERROR] = utils.Error(errorOutput, consts.ExecuteError)

			} else {

				result := strings.TrimSpace(output)

				resultSlice := strings.Split(result, "\r\n")

				var cleanedSlice []string

				for _, line := range resultSlice {

					if line != "" {

						cleanedSlice = append(cleanedSlice, line)

					}
				}
				//fmt.Println(len(cleanedSlice))
				logger.Info(fmt.Sprintf("%q", cleanedSlice))

				for _, element := range cleanedSlice {

					metric := strings.SplitN(element, ":", 2)

					metric[0] = strings.TrimSpace(metric[0])

					metric[1] = strings.TrimSpace(metric[1])

					if utils.MetricsMap[metric[0]] == "Count" {

						z, err := strconv.ParseFloat(metric[1], 64)
						if err != nil {

							logger.Error(fmt.Sprintf("Error while converting %s to float", metric[1]))

						}
						results[metric[0]] = z
					} else {

						results[metric[0]] = metric[1]

					}

				}

			}

			commandsResult <- results

		}(context, commands, resultChannel)
	}

	errorContext := make([]map[string]interface{}, 0)

	results := make(map[string]interface{})

	for length > 0 {
		select {
		case result := <-resultChannel:

			if result[consts.ERROR] != nil {

				errorContext = append(errorContext, result[consts.ERROR].(map[string]interface{}))

			} else {

				maps.Copy(results, result)
			}

			length--
		}

	}

	context[consts.ERROR] = errorContext

	context[consts.RESULT] = results

	if len(errorContext) != 0 {

		context[consts.STATUS] = consts.FAILED

	} else {

		context[consts.STATUS] = consts.SUCCESS

	}
	encodedResult, _ := utils.Encode(context)

	consts.Sender <- encodedResult

	logger.Info(fmt.Sprintf("Result Sent TO ZMQ Sender"))
	//zmqsend <- context

	return
}
