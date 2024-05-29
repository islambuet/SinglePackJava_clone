package aclusterllc.singlepack;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class HelperConfiguration {
    public static final Properties configIni = new Properties();
    public static boolean logSMMessages=false;
    static Logger logger = LoggerFactory.getLogger(HelperConfiguration.class);
    private static HikariDataSource hikariDataSource;
    public static final JSONObject dbBasicInfo = new JSONObject();
    public static final JSONObject countersCurrentValue = new JSONObject();
    public static final JSONObject motorsCurrentSpeed = new JSONObject();
    public static final Map<String, Integer> machinesConnectionStatus  = new HashMap<>();
    public static final JSONObject systemConstants = new JSONObject();
    public static void loadIniConfig(){
     //loading file config
        try {
            configIni.load(new FileInputStream("./resources/config.ini"));
        }
        catch (IOException ex)
        {
            logger.info("File Config Read failed.");
            logger.error(HelperCommon.getStackTraceString(ex));

            System.exit(0);
        }
        setSystemConstants();
    }
    public static void setSystemConstants(){
        JSONObject SM_MESSAGE_ID_NAME  = new JSONObject();

        SM_MESSAGE_ID_NAME.put("1", "System State Message");
        SM_MESSAGE_ID_NAME.put("2", "Inputs Message");
        SM_MESSAGE_ID_NAME.put("3", "Input Change Message");
        SM_MESSAGE_ID_NAME.put("4", "Errors Message");
        SM_MESSAGE_ID_NAME.put("5", "Jams Message");
        SM_MESSAGE_ID_NAME.put("6", "ConfirmationPEBlocked Message");
        SM_MESSAGE_ID_NAME.put("7", "ConfirmationPEBlocked change Message");
        SM_MESSAGE_ID_NAME.put("8", "BinPartiallyFull Message");
        SM_MESSAGE_ID_NAME.put("9", "BinPartiallyFull change Message");
        SM_MESSAGE_ID_NAME.put("10", "BinFull Message");
        SM_MESSAGE_ID_NAME.put("11", "BinFull change Message");
        SM_MESSAGE_ID_NAME.put("12", "BinDisabled Message");
        SM_MESSAGE_ID_NAME.put("13", "BinDisabled change Message");
        SM_MESSAGE_ID_NAME.put("14", "Devices Connected Message");
        SM_MESSAGE_ID_NAME.put("15", "Device Connected Change Message");
        SM_MESSAGE_ID_NAME.put("16", "Sync Response");
        SM_MESSAGE_ID_NAME.put("17", "TrayMissing Message");
        SM_MESSAGE_ID_NAME.put("18", "TrayMissing change Message");
        SM_MESSAGE_ID_NAME.put("20", "Dimension Message");
        SM_MESSAGE_ID_NAME.put("21", "Barcode Result");
        SM_MESSAGE_ID_NAME.put("22", "ConfirmDestination Message");
        SM_MESSAGE_ID_NAME.put("30", "Ping Response");
        SM_MESSAGE_ID_NAME.put("40", "BinMode Message");
        SM_MESSAGE_ID_NAME.put("41", "BinModechange Message");
        SM_MESSAGE_ID_NAME.put("42", "ConveyorState Message");
        SM_MESSAGE_ID_NAME.put("43", "ConveyorStateChange Message");
        SM_MESSAGE_ID_NAME.put("44", "SensorHit Message");
        SM_MESSAGE_ID_NAME.put("45", "Event Message");
        SM_MESSAGE_ID_NAME.put("46", "InductLineState Message");
        SM_MESSAGE_ID_NAME.put("47", "InductLineStateChange Message");
        SM_MESSAGE_ID_NAME.put("48", "PieceInducted Message");
        SM_MESSAGE_ID_NAME.put("49", "Motor Speed Message");
        SM_MESSAGE_ID_NAME.put("50", "EStop Message");
        SM_MESSAGE_ID_NAME.put("51", "Machine Stopped Message");
        SM_MESSAGE_ID_NAME.put("52", "Belt Status Message");
        SM_MESSAGE_ID_NAME.put("53", "Outputs Message");
        SM_MESSAGE_ID_NAME.put("54", "Param Value Message");
        SM_MESSAGE_ID_NAME.put("55", "RequestParams Message");
        SM_MESSAGE_ID_NAME.put("56", "Counter Message");
        SM_MESSAGE_ID_NAME.put("57", "OEEData Message");
        SM_MESSAGE_ID_NAME.put("58", "Shutdown Message");
        SM_MESSAGE_ID_NAME.put("59", "MPInduct Message");
        SM_MESSAGE_ID_NAME.put("60", "MPInductChange  Message");
        SM_MESSAGE_ID_NAME.put("101", "Request Inputs State Message");
        SM_MESSAGE_ID_NAME.put("102", "Request Errors Message");
        SM_MESSAGE_ID_NAME.put("103", "Request Jams Message");
        SM_MESSAGE_ID_NAME.put("105", "Request ConfirmationPEBlocked Message");
        SM_MESSAGE_ID_NAME.put("106", "Request BinPartiallyFull Message");
        SM_MESSAGE_ID_NAME.put("107", "Request BinFull Message");
        SM_MESSAGE_ID_NAME.put("108", "Request BinDisabled Message");
        SM_MESSAGE_ID_NAME.put("109", "Request DevicesConnected State Message");
        SM_MESSAGE_ID_NAME.put("110", "Request BinMode Message");
        SM_MESSAGE_ID_NAME.put("111", "SetBinMode Message");
        SM_MESSAGE_ID_NAME.put("112", "Request ConveyorState Message");
        SM_MESSAGE_ID_NAME.put("113", "Request TrayMissing Message");
        SM_MESSAGE_ID_NAME.put("114", "Request InductLineState Message");
        SM_MESSAGE_ID_NAME.put("115", "Set Param Value Message");
        SM_MESSAGE_ID_NAME.put("116", "Sync Request");
        SM_MESSAGE_ID_NAME.put("120", "SetMode Message");
        SM_MESSAGE_ID_NAME.put("123", "Device Command Message");
        SM_MESSAGE_ID_NAME.put("124", "SortMailpiece Message");
        SM_MESSAGE_ID_NAME.put("125", "Authorization To Start Message");
        SM_MESSAGE_ID_NAME.put("127", "Request MPInduct Message");
        SM_MESSAGE_ID_NAME.put("130", "Ping Request");
        systemConstants.put("SM_MESSAGE_ID_NAME",SM_MESSAGE_ID_NAME);

        JSONObject SYSTEM_STATES  = new JSONObject();
        SYSTEM_STATES.put("0", "Not Ready");
        SYSTEM_STATES.put("1", "Ready");
        SYSTEM_STATES.put("2", "Starting");
        SYSTEM_STATES.put("3", "Running");
        SYSTEM_STATES.put("4", "Stopping");
        systemConstants.put("SYSTEM_STATES",SYSTEM_STATES);

        JSONObject SYSTEM_MODES  = new JSONObject();
        SYSTEM_MODES.put("0", "Auto");
        SYSTEM_MODES.put("1", "Manual");
        systemConstants.put("SYSTEM_MODES",SYSTEM_MODES);
    }
    public static void loadDatabaseConfig(){
        createDatabaseConnection();
        try {
            Connection connection=getConnection();
            String query = "SELECT * FROM alarms";
            dbBasicInfo.put("alarms",HelperDatabase.getSelectQueryResults(connection,query,new String[] { "machine_id", "alarm_id", "alarm_type"}));
            query = "SELECT * FROM bins";
            dbBasicInfo.put("bins",HelperDatabase.getSelectQueryResults(connection,query,new String[] { "machine_id", "bin_id"}));
            query = "SELECT * FROM bins_states_colors ORDER BY ordering DESC";
            dbBasicInfo.put("bins_states_colors",HelperDatabase.getSelectQueryResults(connection,query));
            query = "SELECT * FROM boards";
            dbBasicInfo.put("boards",HelperDatabase.getSelectQueryResults(connection,query,new String[] { "machine_id", "board_id"}));
            query = "SELECT * FROM boards_io";
            dbBasicInfo.put("boards_io",HelperDatabase.getSelectQueryResults(connection,query,new String[] { "id"}));
            query = "SELECT * FROM conveyors";
            dbBasicInfo.put("conveyors",HelperDatabase.getSelectQueryResults(connection,query,new String[] { "machine_id", "conveyor_id"}));
            query = "SELECT * FROM devices";
            dbBasicInfo.put("devices",HelperDatabase.getSelectQueryResults(connection,query,new String[] { "machine_id", "device_id"}));
            query = "SELECT * FROM estop_locations";
            dbBasicInfo.put("estop_locations",HelperDatabase.getSelectQueryResults(connection,query,new String[] { "machine_id", "location_id"}));
            query = "SELECT * FROM events";
            dbBasicInfo.put("events",HelperDatabase.getSelectQueryResults(connection,query,new String[] { "machine_id", "event_id"}));
            query = "SELECT * FROM inducts";
            dbBasicInfo.put("inducts",HelperDatabase.getSelectQueryResults(connection,query,new String[] { "machine_id", "induct_id"}));
            query = "SELECT * FROM inputs";
            dbBasicInfo.put("inputs",HelperDatabase.getSelectQueryResults(connection,query,new String[] { "machine_id", "input_id"}));

            query = "SELECT * FROM machines";
            JSONObject machines=HelperDatabase.getSelectQueryResults(connection,query,new String[] { "machine_id"});
            dbBasicInfo.put("machines",machines);
            for (String machineId : machines.keySet()) {
                machinesConnectionStatus.put(machineId,0);
                for(int i=0;i<32;i++){
                    countersCurrentValue.put(machineId+"_"+(i+1),0);
                }
            }
            query = "SELECT * FROM motors";
            JSONObject motors=HelperDatabase.getSelectQueryResults(connection,query,new String[] { "machine_id", "motor_id"});
            dbBasicInfo.put("motors",motors);
            for (String motorKey : motors.keySet()) {
                motorsCurrentSpeed.put(motorKey,0);
            }
            query = "SELECT * FROM mpinducts";
            dbBasicInfo.put("mpinducts",HelperDatabase.getSelectQueryResults(connection,query,new String[] { "machine_id", "mpinduct_id"}));

            query = "SELECT * FROM mpinducts_labels ORDER BY ordering DESC";
            dbBasicInfo.put("mpinducts_labels",HelperDatabase.getSelectQueryResults(connection,query));

            query = "SELECT * FROM parameters";
            dbBasicInfo.put("parameters",HelperDatabase.getSelectQueryResults(connection,query,new String[] { "machine_id", "param_id"}));

            query = "SELECT * FROM scs";
            dbBasicInfo.put("scs",HelperDatabase.getSelectQueryResults(connection,query,new String[] { "value"}));
            query = "SELECT * FROM sensors";
            dbBasicInfo.put("sensors",HelperDatabase.getSelectQueryResults(connection,query,new String[] { "machine_id", "sensor_id"}));

            query = "INSERT IGNORE INTO statistics (machine_id) SELECT DISTINCT machine_id FROM machines WHERE NOT EXISTS (SELECT * FROM statistics);" +
                    "INSERT IGNORE INTO statistics_minutely (machine_id) SELECT DISTINCT machine_id FROM machines WHERE NOT EXISTS (SELECT * FROM statistics_minutely);" +
                    "INSERT IGNORE INTO statistics_hourly (machine_id) SELECT DISTINCT machine_id FROM machines WHERE NOT EXISTS (SELECT * FROM statistics_hourly);" +
                    "INSERT IGNORE INTO statistics_counter (machine_id) SELECT DISTINCT machine_id FROM machines WHERE NOT EXISTS (SELECT * FROM statistics_counter);" +
                    "INSERT IGNORE INTO statistics_oee (machine_id) SELECT DISTINCT machine_id FROM machines WHERE NOT EXISTS (SELECT * FROM statistics_oee);" +
                    "INSERT IGNORE INTO statistics_bins (machine_id,bin_id) SELECT DISTINCT machine_id,bin_id FROM bins WHERE NOT EXISTS (SELECT * FROM statistics_bins);" +
                    "INSERT IGNORE INTO statistics_bins_counter (machine_id,bin_id) SELECT DISTINCT machine_id,bin_id FROM bins WHERE NOT EXISTS (SELECT * FROM statistics_bins_counter);" +
                    "INSERT IGNORE INTO statistics_bins_hourly (machine_id,bin_id) SELECT DISTINCT machine_id,bin_id FROM bins WHERE NOT EXISTS (SELECT * FROM statistics_bins_hourly);";
            HelperDatabase.runMultipleQuery(connection,query);
        }
        catch (Exception ex) {
            logger.error("[Database] Failed To get Data from database.Closing Java Program.");
            logger.error(HelperCommon.getStackTraceString(ex));
            System.exit(0);
        }
    }
    public static void createDatabaseConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        }
        catch (ClassNotFoundException ex) {
            logger.info("[Database] Mysql Driver Not Found.Closing Java Program.");
            logger.error(HelperCommon.getStackTraceString(ex));
            System.exit(0);
        }
        HikariConfig config = new HikariConfig();
        String jdbcUrl = String.format("jdbc:mysql://%s:3306/%s" +
                        "?allowPublicKeyRetrieval=true" +
                        "&useSSL=false" +
                        "&useUnicode=true" +
                        "&characterEncoding=utf8" +
                        "&allowMultiQueries=true",
                configIni.getProperty("db_host"),
                configIni.getProperty("db_name"));

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(configIni.getProperty("db_username"));
        config.setPassword(configIni.getProperty("db_password"));
        config.setMaximumPoolSize(30);
        config.setConnectionTimeout(300000);
        config.setLeakDetectionThreshold(300000);
        config.addDataSourceProperty( "cachePrepStmts" , "true" );
        config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
        config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
        try{
            hikariDataSource = new HikariDataSource(config);
            logger.info("[Database] Connected.");
        }
        catch (Exception ex){
            logger.error("[Database] Connection Failed.Closing Java Program.");
            logger.error(HelperCommon.getStackTraceString(ex));
            System.exit(0);
        }
    }
    public static Connection getConnection(){
        try {
            return hikariDataSource.getConnection();
        }
        catch (SQLException ex) {
            logger.error("[Database] Connection Failed.Closing Java Program.");
            logger.error(HelperCommon.getStackTraceString(ex));
            System.exit(0);
            return null;
        }
    }
}
