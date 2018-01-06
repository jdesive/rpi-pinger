# RPi Pinger
A simple pinging application made to run on a raspberrypi or similar device. RPi Pinger keeps track of the device its pointed at and will send email notifications when the device is OFFLINE/ONLINE and a annual update email. 

## Parameters
* Ping Delay - *15* (Minutes)
* Update Delay - *1* (Days)
* Ping Timeout - *1000* 
* Max Unreachable Attempts - *8*
* IP - *9.9.9.9*
* To Email - *NONE*
* From Gmail - *NONE*
* Gmail Password - *NONE* 

Example:  
`java -jar pinger.jar 15 1 1000 8 127.0.0.1 to-email@example.com from-email@gmail.com password`
