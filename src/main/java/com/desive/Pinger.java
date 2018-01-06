package com.desive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * RPi Pinger is made to ping a device repeatably and send an email when the device is not active.
 *
 * Initial use case it to ping a smart IOT device and notify me when the device is not longer active.
 *
 * Arguments:
 *   - Ping Delay
 *   - Update Delay
 *   - Ping Timeout
 *   - Max attempts
 *   - Ip Address
 *   - Email To
 *   - Email From (Gmail)
 *   - Email Password (Gmail)
 *
 *   java -jar pinger.jar 15 1 1000 8 127.0.0.1 to-email@example.com from-email@gmail.com password
 *
 * @author Jack DeSive
 */
public class Pinger {

    private InetAddress NETWORK_ADDRESS;
    private int REACHABLE_COUNT = 0, MAX_UNREACHABLE_ATTEMPTS = 8, // 2 hours at 15 pings a minute
                TOTAL_PINGS = 0, TOTAL_FAILED_PINGS = 0, TOTAL_SUCCESSFUL_PINGS = 0;
    private Logger logger = LoggerFactory.getLogger(Pinger.class);
    private ScheduledExecutorService executor;
    private Notifier notifier;

    private String IP_ADDRESS = "9.9.9.99";
    private String NOTIFIER_TO = "", NOTIFIER_FROM = "", NOTIFIER_PASS = "";
    private int PING_DELAY = 15,
            PING_TIMEOUT = 1000,
            UPDATE_DELAY = 1;

    /**
     * Starts the application
     */
    private void start(String[] args) {

        if(args.length == 8){
            PING_DELAY = Integer.parseInt(args[0]);
            UPDATE_DELAY = Integer.parseInt(args[1]);
            PING_TIMEOUT = Integer.parseInt(args[2]);
            MAX_UNREACHABLE_ATTEMPTS = Integer.parseInt(args[3]);
            IP_ADDRESS = args[4];
            NOTIFIER_TO = args[5];
            NOTIFIER_FROM = args[6];
            NOTIFIER_PASS = args[7];
        }

        this.executor = Executors.newScheduledThreadPool(2);
        this.notifier = new Notifier(NOTIFIER_TO, NOTIFIER_FROM, NOTIFIER_PASS);
        try {
            this.NETWORK_ADDRESS = InetAddress.getByName(IP_ADDRESS);
            logger.info("Pinger is now enabled");
            logger.info("IP: " + IP_ADDRESS);
            reschedulePinger();
            rescheduleUpdateNotification();
            logger.info("Jobs have been scheduled.");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reschedules the ping to run again
     */
    private void reschedulePinger() {
        this.executor.schedule(this::doPingerWork, PING_DELAY, TimeUnit.MINUTES);
    }

    /**
     * Ping logic
     */
    private void doPingerWork() {
        try {
            TOTAL_PINGS++;
            if(NETWORK_ADDRESS.isReachable(PING_TIMEOUT)) {
                TOTAL_SUCCESSFUL_PINGS++;
                if(REACHABLE_COUNT >= MAX_UNREACHABLE_ATTEMPTS){
                    logger.info(IP_ADDRESS + " is reachable again!");
                    this.executor.schedule(() -> this.notifier.send(IP_ADDRESS + " is reachable again!",
                            "The device with IP: " + IP_ADDRESS + " is reachable again!"), 0 , TimeUnit.SECONDS);
                }
                REACHABLE_COUNT = 0;
            }else{
                TOTAL_FAILED_PINGS++;
                REACHABLE_COUNT++;
                if(REACHABLE_COUNT == MAX_UNREACHABLE_ATTEMPTS) {
                    logger.info(IP_ADDRESS + " is unreachable");
                    this.executor.schedule(() -> this.notifier.send(IP_ADDRESS + " is unreachable",
                            "The device with IP: " + IP_ADDRESS + " has become unreachable. You may need to check it out!"), 0 , TimeUnit.SECONDS);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        reschedulePinger();
    }

    /**
     * Reschedules the update notification
     */
    private void rescheduleUpdateNotification() {
        this.executor.schedule(this::doUpdateNotificationWork, UPDATE_DELAY, TimeUnit.DAYS);
    }

    /**
     * Update notification logic
     */
    private void doUpdateNotificationWork() {
        this.executor.schedule(() -> {
            this.notifier.send("Device " + IP_ADDRESS + " update",
                    "Here is your update for device " + IP_ADDRESS + "\n\n" +
                            "Total Pings: " + TOTAL_PINGS + "\n" +
                            "Failed Pings: " + TOTAL_FAILED_PINGS + "\n" +
                            "Successful Pings: " + TOTAL_SUCCESSFUL_PINGS + "\n" +
                            "IP Address: " + IP_ADDRESS + "\n\n" +
                            "Device is currently " + ((REACHABLE_COUNT >= MAX_UNREACHABLE_ATTEMPTS) ? "OFFLINE" : "ONLINE"));
            TOTAL_SUCCESSFUL_PINGS = 0;
            TOTAL_FAILED_PINGS = 0;
            TOTAL_PINGS = 0;
        }, 0, TimeUnit.SECONDS);
        rescheduleUpdateNotification();
    }

    public static void main(String[] args) {
        new Pinger().start(args);
    }

}
