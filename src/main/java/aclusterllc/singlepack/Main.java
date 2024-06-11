package aclusterllc.singlepack;


import org.apache.logging.log4j.core.config.Configurator;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;

public class Main {
    public static void main(String[] args) {
        Configurator.initialize(null, "./resources/log4j2.xml");
        Logger logger = LoggerFactory.getLogger(Main.class);

        HelperConfiguration.loadIniConfig();

        MainGui mainGui = new MainGui();
        mainGui.startGui();
        try {
            int initialSleepTime = Integer.parseInt((HelperConfiguration.configIni.getProperty("initial_sleep_time")));
            mainGui.appendToMainTextArea("Waiting "+initialSleepTime+"s.");
            logger.info("Waiting "+initialSleepTime+"s");
            Thread.sleep(initialSleepTime * 1000);
        }
        catch (InterruptedException ex) {
            logger.info("Waiting Error.");
            logger.error(HelperCommon.getStackTraceString(ex));
        }
        mainGui.appendToMainTextArea("Waiting Finished.");

        HelperConfiguration.createDatabaseConnection();
        HelperConfiguration.cleanupDatabase();
        String purge_time= (String) HelperConfiguration.configIni.get("purge_time");
        new Thread(()->{
            while (true){
                LocalTime currentTime= LocalTime.now();
                LocalTime scheduleTime= LocalTime.parse(purge_time);
                int difference=(scheduleTime.toSecondOfDay()- currentTime.toSecondOfDay())*1000;
                if(difference<=0){
                    difference=86400000+difference;
                }
                try {
                    logger.info("Purge Scheduler waiting "+difference+" ms for next schedule");
                    Thread.sleep(difference);
                    logger.info("Purge Scheduler waiting End");
                    HelperConfiguration.cleanupDatabase();
                }
                catch (InterruptedException ex) {
                    logger.error(HelperCommon.getStackTraceString(ex));
                }
            }
        }).start();
        HelperConfiguration.loadDatabaseConfig();
        mainGui.appendToMainTextArea("Database Loading Finished");

        ClientForSMMessageQueueHandler clientForSMMessageQueueHandler=new ClientForSMMessageQueueHandler();
        clientForSMMessageQueueHandler.start();

        ServerForHmi serverForHmi=new ServerForHmi();
        JSONObject machines=(JSONObject)HelperConfiguration.dbBasicInfo.get("machines");
        for (String key : machines.keySet()) {
            ClientForSM clientForSM=new ClientForSM((JSONObject) machines.get(key),clientForSMMessageQueueHandler);
            clientForSM.addObserverSMMessage(mainGui);
            serverForHmi.addObserverHmiMessage(clientForSM);
            clientForSM.start();
        }


        serverForHmi.start();


        System.out.println("Main Started");

    }
}
