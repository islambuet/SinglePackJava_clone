package aclusterllc.singlepack;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

public class ClientForSMMessageHandler {
    static Logger logger = LoggerFactory.getLogger(ClientForSM.class);
    public static int handleMessage_1(Connection connection, JSONObject clientInfo, byte[] dataBytes) throws SQLException{
        String updateQuery = format("UPDATE machines SET `machine_state`=%d, `machine_mode`=%d, `updated_at`=now()  WHERE `machine_id`=%d LIMIT 1",dataBytes[0],dataBytes[1],clientInfo.getInt("machine_id"));
        return HelperDatabase.runUpdateQuery(connection,updateQuery);
    }
    public static JSONObject handleMessage_2(Connection connection, JSONObject clientInfo, byte[] dataBytes) throws SQLException {
        JSONObject inputsInfo= (JSONObject) HelperConfiguration.dbBasicInfo.get("inputs");
        byte []bits=HelperCommon.bitsFromBytes(dataBytes,4);

        int machineId=clientInfo.getInt("machine_id");
        JSONObject inputsCurrentState=HelperDatabase.getInputsStates(connection,machineId);
        String query="";
        for(int i=0;i<bits.length;i++){
            boolean insertHistory=false;
            if(inputsCurrentState.has(machineId+"_"+(i+1))){
                JSONObject inputState= (JSONObject) inputsCurrentState.get(machineId+"_"+(i+1));
                if(inputState.getInt("state")!=bits[i]){
                    query+= format("UPDATE inputs_states SET `state`=%d,`updated_at`=now() WHERE id=%d;",bits[i],inputState.getLong("id"));
                    insertHistory=true;
                }
            }
            else{
                query+= format("INSERT INTO inputs_states (`machine_id`, `input_id`,`state`) VALUES (%d,%d,%d);",machineId,(i+1),bits[i]);
                insertHistory=true;
            }
            if(insertHistory && (inputsInfo.has(machineId+"_"+(i+1))) && (((JSONObject)inputsInfo.get(machineId+"_"+(i+1))).getInt("enable_history")==1)){
                    query+= format("INSERT INTO inputs_states_history (`machine_id`, `input_id`,`state`) VALUES (%d,%d,%d);",machineId,(i+1),bits[i]);
            }
        }
        HelperDatabase.runMultipleQuery(connection,query);
        return inputsCurrentState;

    }
    public static JSONObject handleMessage_3(Connection connection, JSONObject clientInfo, byte[] dataBytes) throws SQLException {
        JSONObject inputsInfo= (JSONObject) HelperConfiguration.dbBasicInfo.get("inputs");
        int machineId=clientInfo.getInt("machine_id");
        int inputId = HelperCommon.bytesToInt(Arrays.copyOfRange(dataBytes, 0, 2));
        int state=dataBytes[2];
        String query = String.format("SELECT * FROM inputs_states WHERE machine_id=%d AND input_id=%d", machineId,inputId);
        JSONArray queryResult=HelperDatabase.getSelectQueryResults(connection,query);
        if(queryResult.length()>0){
            JSONObject inputState=queryResult.getJSONObject(0);
            if(inputState.getInt("state")!=state){
                String query2= format("UPDATE inputs_states SET `state`=%d,`updated_at`=now() WHERE id=%d;",state,inputState.getLong("id"));
                if((inputsInfo.has(machineId+"_"+inputId)) && (((JSONObject)inputsInfo.get(machineId+"_"+inputId)).getInt("enable_history")==1)){
                    query2+= format("INSERT INTO inputs_states_history (`machine_id`, `input_id`,`state`) VALUES (%d,%d,%d);",machineId,inputId,state);
                }
                HelperDatabase.runMultipleQuery(connection,query2);
            }
            return inputState;
        }
        else{
            return new JSONObject();
        }


    }
    public static JSONObject handleMessage_4_5(Connection connection, JSONObject clientInfo, byte[] dataBytes,int messageId) throws SQLException {
        int machineId=clientInfo.getInt("machine_id");
        JSONArray alarmsActive= HelperDatabase.getAlarmsActive(connection,machineId);
        JSONObject jsonAlarmsActive=new JSONObject();
        int alarm_type=0;////messageId=4
        if(messageId==5){
            alarm_type=1;
        }
        for(int i=0;i<alarmsActive.length();i++){
            JSONObject item= (JSONObject) alarmsActive.get(i);
            if(item.getInt("alarm_type")==alarm_type){
                jsonAlarmsActive.put(item.getInt("machine_id")+"_"+item.getInt("alarm_id"),item);
            }

        }
        byte []bits=HelperCommon.bitsFromBytes(dataBytes,4);
        String query="";

        for(int i=0;i<bits.length;i++){
            if(bits[i]==1){
                if(!(jsonAlarmsActive.has(machineId+"_"+(i+1)))){
                    query+= format("INSERT INTO alarms_active (`machine_id`, `alarm_id`,`alarm_type`) VALUES (%d,%d,%d);",machineId,(i+1),alarm_type);
                }
            }
            else{
                if((jsonAlarmsActive.has(machineId+"_"+(i+1)))){
                    JSONObject item= (JSONObject) jsonAlarmsActive.get(machineId+"_"+(i+1));
                    query+= format("INSERT INTO alarms_history (`machine_id`, `alarm_id`,`alarm_type`,`date_active`) VALUES (%d,%d,%d,'%s');",machineId,(i+1),alarm_type,item.get("date_active"));
                    query+=format("DELETE FROM alarms_active where id=%d;",item.getLong("id"));
                }
            }
        }
        HelperDatabase.runMultipleQuery(connection,query);
        return jsonAlarmsActive;

    }
    public static JSONObject handleMessage_6_8_10_12_17_40(Connection connection, JSONObject clientInfo, byte[] dataBytes,int messageId) throws SQLException {
        int machineId=clientInfo.getInt("machine_id");
        JSONObject binsStates=HelperDatabase.getBinsStates(connection,clientInfo.getInt("machine_id"));
        JSONObject bins= (JSONObject) HelperConfiguration.dbBasicInfo.get("bins");
        String columName="pe_blocked";////messageId=6
        if(messageId==8){
            columName="partially_full";
        }
        else if(messageId==10){
            columName="full";
        }
        else if(messageId==12){
            columName="disabled";
        }
        else if(messageId==17){
            columName="tray_missing";
        }
        else if(messageId==40){
            columName="mode";
        }
        byte []bits=HelperCommon.bitsFromBytes(Arrays.copyOfRange(dataBytes, 4, dataBytes.length),4);//0-3 is number of bins which is equal to bits length
        String query="";
        for(int i=0;i<bits.length;i++){
            int bin_id=(i+1);
            //save only from bins
            if(bins.has(machineId+"_"+bin_id)){
                if(binsStates.has(machineId+"_"+bin_id)){
                    JSONObject binStates= (JSONObject) binsStates.get(machineId+"_"+bin_id);
                    if(binStates.getInt(columName)!=bits[i]){
                        query+=format("UPDATE bins_states SET `%s`='%s', `updated_at`=now()  WHERE `id`=%d;",columName,bits[i],binStates.getLong("id"));
                        String unChangedQuery="";
                        for(String key:binStates.keySet()){
                            if(!(key.equals("id")|| key.equals("updated_at")|| key.equals(columName)))
                            {
                                unChangedQuery+=format("`%s`='%s',",key,binStates.getInt(key));
                            }
                        }
                        query+= format("INSERT INTO bins_states_history SET %s `%s`='%s', `updated_at`=now();",unChangedQuery,columName,bits[i]);
                    }
                }
                else{
                    query+= format("INSERT INTO bins_states (`machine_id`, `bin_id`,`%s`) VALUES (%d,%d,%d);",columName,machineId,bin_id,bits[i]);
                    query+= format("INSERT INTO bins_states_history (`machine_id`, `bin_id`,`%s`) VALUES (%d,%d,%d);",columName,machineId,bin_id,bits[i]);
                }
            }

        }
        HelperDatabase.runMultipleQuery(connection,query);
        return binsStates;
    }
    public static JSONObject handleMessage_7_9_11_13_18_41(Connection connection, JSONObject clientInfo, byte[] dataBytes,int messageId) throws SQLException {
        int machineId=clientInfo.getInt("machine_id");
        JSONObject binsStates=HelperDatabase.getBinsStates(connection,clientInfo.getInt("machine_id"));
        JSONObject binStates=new JSONObject();
        JSONObject bins= (JSONObject) HelperConfiguration.dbBasicInfo.get("bins");

        int bin_id = (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 0, 2));
        int state=dataBytes[2];
        String columName="pe_blocked";//messageId=7
        if(messageId==9){
            columName="partially_full";
        }
        else if(messageId==11){
            columName="full";
        }
        else if(messageId==13){
            columName="disabled";
        }
        else if(messageId==18){
            columName="tray_missing";
        }
        else if(messageId==41){
            columName="mode";
        }
        String query="";
        //save only from bins
        if(bins.has(machineId+"_"+bin_id)){
            if(binsStates.has(machineId+"_"+bin_id)){
                binStates= (JSONObject) binsStates.get(machineId+"_"+bin_id);
                if(binStates.getInt(columName)!=state){
                    query+=format("UPDATE bins_states SET `%s`='%s', `updated_at`=now()  WHERE `id`=%d;",columName,state,binStates.getLong("id"));
                    String unChangedQuery="";
                    for(String key:binStates.keySet()){
                        if(!(key.equals("id")|| key.equals("updated_at")|| key.equals(columName)))
                        {
                            unChangedQuery+=format("`%s`='%s',",key,binStates.getInt(key));
                        }
                    }
                    query+= format("INSERT INTO bins_states_history SET %s `%s`='%s', `updated_at`=now();",unChangedQuery,columName,state);
                }
            }
            else{
                query+= format("INSERT INTO bins_states (`machine_id`, `bin_id`,`%s`) VALUES (%d,%d,%d);",columName,machineId,bin_id,state);
                query+= format("INSERT INTO bins_states_history (`machine_id`, `bin_id`,`%s`) VALUES (%d,%d,%d);",columName,machineId,bin_id,state);
            }
        }
        HelperDatabase.runMultipleQuery(connection,query);
        return binStates;
    }
    public static JSONObject handleMessage_14(Connection connection, JSONObject clientInfo, byte[] dataBytes) throws SQLException {
        int machineId=clientInfo.getInt("machine_id");
        JSONObject devicesStates=HelperDatabase.getDevicesStates(connection,machineId);
        byte []bits=HelperCommon.bitsFromBytes(dataBytes,4);
        String query="";
        for(int i=0;i<bits.length;i++){
            //save all devices
            if(devicesStates.has(machineId+"_"+(i+1))){
                JSONObject deviceState= (JSONObject) devicesStates.get(machineId+"_"+(i+1));
                if(deviceState.getInt("state")!=bits[i]){
                    query+= format("UPDATE devices_states SET `state`=%d,`updated_at`=now() WHERE id=%d;",bits[i],deviceState.getLong("id"));
                    query+= format("INSERT INTO devices_states_history (`machine_id`, `device_id`,`state`) VALUES (%d,%d,%d);",machineId,(i+1),bits[i]);
                }
            }
            else{
                query+= format("INSERT INTO devices_states (`machine_id`, `device_id`,`state`) VALUES (%d,%d,%d);",machineId,(i+1),bits[i]);
                query+= format("INSERT INTO devices_states_history (`machine_id`, `device_id`,`state`) VALUES (%d,%d,%d);",machineId,(i+1),bits[i]);
            }
        }
        HelperDatabase.runMultipleQuery(connection,query);
        return devicesStates;
    }
    public static JSONObject handleMessage_15(Connection connection, JSONObject clientInfo, byte[] dataBytes) throws SQLException {
        int machineId=clientInfo.getInt("machine_id");
        JSONObject devicesStates=HelperDatabase.getDevicesStates(connection,machineId);
        int device_id = (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 0, 2));
        int state=dataBytes[2];
        JSONObject deviceState=new JSONObject();

        String query="";
        if(devicesStates.has(machineId+"_"+device_id)){
            deviceState= (JSONObject) devicesStates.get(machineId+"_"+device_id);
            if(deviceState.getInt("state")!=state){
                query+=format("UPDATE devices_states SET `state`='%d', `updated_at`=now()  WHERE `id`=%d;",state,deviceState.getLong("id"));
                query+=format("INSERT INTO devices_states_history (`machine_id`, `device_id`,`state`) VALUES (%d,%d,%d);",machineId,device_id,state);
            }
        }
        else{
            query+= format("INSERT INTO devices_states (`machine_id`, `device_id`,`state`) VALUES (%d,%d,%d);",machineId,device_id,state);
            query+= format("INSERT INTO devices_states_history (`machine_id`, `device_id`,`state`) VALUES (%d,%d,%d);",machineId,device_id,state);
        }
        HelperDatabase.runMultipleQuery(connection,query);
        return deviceState;

    }
    public static JSONObject handleMessage_20(Connection connection, JSONObject clientInfo, byte[] dataBytes)  throws SQLException{
        JSONObject productInfo=new JSONObject();
        int machineId=clientInfo.getInt("machine_id");
        long mailId = HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 0, 4));
        int length = (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 4, 8));
        int width = (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 8, 12));
        int height = (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 12, 16));
        int weight = (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 16, 20));
        int reject_code=dataBytes[20];
        String queryCheckProduct=format("SELECT * FROM products WHERE machine_id=%d AND mail_id=%d;", machineId, mailId);
        JSONArray queryCheckProductResult=HelperDatabase.getSelectQueryResults(connection,queryCheckProduct);
        if(queryCheckProductResult.length()>0){
            productInfo=queryCheckProductResult.getJSONObject(0);
            productInfo.put("length",length);
            productInfo.put("width",width);
            productInfo.put("height",height);
            productInfo.put("weight",length);
            productInfo.put("reject_code",reject_code);
            String query =format("UPDATE products SET length=%d, width=%d, height=%d, weight=%d, reject_code=%d, dimension_at=NOW() WHERE id=%d;",
                     length, width, height, weight, reject_code, productInfo.getLong("id"));

            HelperDatabase.runMultipleQuery(connection,query);
            logger.info("[PRODUCT][20] Product Updated. MailId=" + mailId);
        }
        else{
            logger.error("[PRODUCT][20] Product not found found. MailId="+mailId);
        }
        return productInfo;
    }
    public static JSONObject handleMessage_21(Connection connection, JSONObject clientInfo, byte[] dataBytes) throws SQLException {
        JSONObject productInfo = new JSONObject();

        int machineId = clientInfo.getInt("machine_id");
        long mailId = HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 0, 4));
        int number_of_results = (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 4, 6));

        String queryBarcode = "";
        int bytePos = 6;
        JSONObject barCodeInfo = new JSONObject();
        for (int i = 1; (i < 4) && (i <= number_of_results); i++) {
            barCodeInfo.put("barcode" + i + "_type", dataBytes[bytePos]);
            queryBarcode += format("`barcode%s_type`='%s',", i, dataBytes[bytePos]);
            bytePos++;
            int barcodeLength = (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, bytePos, bytePos + 2));
            bytePos += 2;
            String barcode = new String(Arrays.copyOfRange(dataBytes, bytePos, bytePos + barcodeLength), StandardCharsets.UTF_8);
            //barcode = barcode.replaceAll("\\P{Print}", "");
            barCodeInfo.put("barcode" + i + "_string", barcode);
            queryBarcode += format("`barcode%s_string`='%s',", i, barcode);
            bytePos += barcodeLength;
        }
        int valid_read = 1, no_read = 0, multiple_read = 0, no_code = 0;//if number_of_results=1
        if (number_of_results == 1) {
            String barcode1_string = barCodeInfo.getString("barcode1_string");
            switch (barcode1_string) {
                case "??????????":
                    no_read = 1;
                    valid_read = 0;
                    break;
                case "9999999999":
                    multiple_read = 1;
                    valid_read = 0;
                    break;
                case "0000000000":
                    no_code = 1;
                    valid_read = 0;
                    break;
            }
        }
        else {
            valid_read = 0;
            if (number_of_results == 0) {
                no_code = 1;
            } else {
                multiple_read = 1;
            }
        }
        String query = "";
        String queryCreateNew = "";
        String queryCheckProduct = format("SELECT * FROM products WHERE machine_id=%d AND mail_id=%d;", machineId, mailId);
        JSONArray queryCheckProductResult = HelperDatabase.getSelectQueryResults(connection, queryCheckProduct);

        if (queryCheckProductResult.length() > 0) {
            productInfo = queryCheckProductResult.getJSONObject(0);
            query += format("UPDATE products SET %s`number_of_results`='%s', `barcode_at`=now()  WHERE `id`=%d;", queryBarcode, number_of_results, productInfo.getLong("id"));
        }
        else {
            productInfo.put("mail_id", mailId);
            productInfo.put("machine_id", machineId);
            queryCreateNew += format("INSERT INTO products SET %s`number_of_results`='%s',`machine_id`='%s',`mail_id`='%s', `barcode_at`=now();", queryBarcode, number_of_results, machineId, mailId);
            logger.warn("[PRODUCT][21] Product not found found. Creating New. MailId=" + mailId);
        }
        query += format("UPDATE statistics SET total_read=total_read+1, no_read=no_read+%d, no_code=no_code+%d, multiple_read=multiple_read+%d, valid=valid+%d WHERE machine_id=%d ORDER BY id DESC LIMIT 1;", no_read, no_code, multiple_read, valid_read, machineId);
        query += format("UPDATE statistics_minutely SET total_read=total_read+1, no_read=no_read+%d, no_code=no_code+%d, multiple_read=multiple_read+%d, valid=valid+%d WHERE machine_id=%d ORDER BY id DESC LIMIT 1;", no_read, no_code, multiple_read, valid_read, machineId);
        query += format("UPDATE statistics_hourly SET total_read=total_read+1, no_read=no_read+%d, no_code=no_code+%d, multiple_read=multiple_read+%d, valid=valid+%d WHERE machine_id=%d ORDER BY id DESC LIMIT 1;", no_read, no_code, multiple_read, valid_read, machineId);
        query += format("UPDATE statistics_counter SET total_read=total_read+1, no_read=no_read+%d, no_code=no_code+%d, multiple_read=multiple_read+%d, valid=valid+%d WHERE machine_id=%d ORDER BY id DESC LIMIT 1;", no_read, no_code, multiple_read, valid_read, machineId);

        connection.setAutoCommit(false);
        Statement stmt = connection.createStatement();
        if (queryCreateNew.length() > 0) {
            stmt.executeUpdate(queryCreateNew, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                productInfo.put("id", rs.getLong(1));
            }
            rs.close();
        }
        stmt.execute(query);
        connection.commit();
        connection.setAutoCommit(true);
        stmt.close();
        logger.info("[PRODUCT][21] Product Updated. MailId=" + mailId);

        productInfo.put("number_of_results", number_of_results);
        for (String key : barCodeInfo.keySet()) {
            productInfo.put(key, barCodeInfo.get(key));
        }
        return productInfo;
    }
    public static JSONObject handleMessage_22(Connection connection, JSONObject clientInfo, byte[] dataBytes) throws SQLException {
        JSONObject productInfo=new JSONObject();
        int machine_id = clientInfo.getInt("machine_id");
        long mail_id = HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 0, 4));
        int destination = (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 4, 6));
        int destination_alternate = (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 6, 8));
        int destination_final = (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 8, 10));
        int reason = dataBytes[10];

        String query = "";
        String queryCheckProduct = format("SELECT * FROM products WHERE machine_id=%d AND mail_id=%d;", machine_id, mail_id);
        JSONArray queryCheckProductResult = HelperDatabase.getSelectQueryResults(connection, queryCheckProduct);

        if (queryCheckProductResult.length() > 0) {
            productInfo = queryCheckProductResult.getJSONObject(0);
        }
        else {
            query = format("INSERT INTO products (`machine_id`, `mail_id`) VALUES (%d, %d);", machine_id, mail_id);
            query += format("UPDATE %s SET total_read=total_read+1,no_code=no_code+1 WHERE machine_id=%d ORDER BY id DESC LIMIT 1;", "statistics", machine_id);
            query += format("UPDATE %s SET total_read=total_read+1,no_code=no_code+1 WHERE machine_id=%d ORDER BY id DESC LIMIT 1;", "statistics_hourly", machine_id);
            query += format("UPDATE %s SET total_read=total_read+1,no_code=no_code+1 WHERE machine_id=%d ORDER BY id DESC LIMIT 1;", "statistics_minutely", machine_id);
            query += format("UPDATE %s SET total_read=total_read+1,no_code=no_code+1 WHERE machine_id=%d ORDER BY id DESC LIMIT 1;", "statistics_counter", machine_id);
            logger.warn("[PRODUCT][22] Product not found found. Creating New and Updating Statistics. MailId=" + mail_id);

            HelperDatabase.runMultipleQuery(connection, query);
            JSONArray queryCheckProductResultNew = HelperDatabase.getSelectQueryResults(connection, queryCheckProduct);

            if (queryCheckProductResultNew.length() > 0) {
                productInfo = queryCheckProductResultNew.getJSONObject(0);
            } else {
                logger.error("[PRODUCT][22] Failed to create new Product.MailId=" + mail_id);
                return new JSONObject();
            }

        }
        //Process confirm
        productInfo.put("destination", destination);
        productInfo.put("destination_alternate", destination_alternate);
        productInfo.put("destination_final", destination_final);
        productInfo.put("reason", reason);
        String valueFromProductsQuery = "";
        for (String key : productInfo.keySet()) {
            valueFromProductsQuery += format("`%s`='%s',", key.equals("id") ? "product_id" : key, productInfo.get(key));
        }
        query = format("INSERT INTO products_history SET %s `confirmed_at`=now();", valueFromProductsQuery);
        query += format("DELETE FROM products WHERE id=%d;", productInfo.getLong("id"));

        //process short codes
        JSONObject destBin = null;
        JSONObject destFinalBin = null;
        JSONObject bins = HelperConfiguration.dbBasicInfo.getJSONObject("bins");
        for (String key : bins.keySet()) {
            JSONObject bin = bins.getJSONObject(key);
            if (bin.getInt("sort_manager_id") == destination) {
                destBin = bin;
            }
            if (bin.getInt("sort_manager_id") == destination_final) {
                destFinalBin = bin;
            }
        }
        if (destBin != null && destFinalBin != null) {
            List<Integer> possibleReasons = new ArrayList<>(Arrays.asList(0, 1 ,2, 3, 4, 5, 6, 7, 8, 9, 10,11, 12,13, 14,15, 16, 17, 18,19,20, 21));
            if (possibleReasons.contains(reason)) {
                String scColumn = "sc" + reason;
                String recircUpdate = "";
                String rejectUpdate = "";

                //statistics
                if (destFinalBin.getInt("recirc_bin") == 1) {
                    recircUpdate = " ,recirc=recirc+1";
                } else if (destFinalBin.getInt("reject_bin") == 1) {
                    rejectUpdate = " ,reject=reject+1";
                }

                query += format("UPDATE %s SET %s=%s+1%s%s WHERE machine_id=%d ORDER BY id DESC LIMIT 1;", "statistics", scColumn, scColumn, recircUpdate, rejectUpdate, machine_id);
                query += format("UPDATE %s SET %s=%s+1%s%s WHERE machine_id=%d ORDER BY id DESC LIMIT 1;", "statistics_counter", scColumn, scColumn, recircUpdate, rejectUpdate, machine_id);
                query += format("UPDATE %s SET %s=%s+1%s%s WHERE machine_id=%d ORDER BY id DESC LIMIT 1;", "statistics_hourly", scColumn, scColumn, recircUpdate, rejectUpdate, machine_id);
                query += format("UPDATE %s SET %s=%s+1%s%s WHERE machine_id=%d ORDER BY id DESC LIMIT 1;", "statistics_minutely", scColumn, scColumn, recircUpdate, rejectUpdate, machine_id);

                //bin statistics
                //update short code for all condition destFinalBin
                {
                    query += format("UPDATE %s SET %s=%s+1 WHERE machine_id=%d AND bin_id=%d ORDER BY id DESC LIMIT 1;", "statistics_bins", scColumn, scColumn, machine_id, destFinalBin.getInt("bin_id"));
                    query += format("UPDATE %s SET %s=%s+1 WHERE machine_id=%d AND bin_id=%d ORDER BY id DESC LIMIT 1;", "statistics_bins_counter", scColumn, scColumn, machine_id, destFinalBin.getInt("bin_id"));
                    query += format("UPDATE %s SET %s=%s+1 WHERE machine_id=%d AND bin_id=%d ORDER BY id DESC LIMIT 1;", "statistics_bins_hourly", scColumn, scColumn, machine_id, destFinalBin.getInt("bin_id"));
                }
                if ((destBin.getInt("reject_bin") != 1) && (destBin != destFinalBin)) {
                    query += format("UPDATE %s SET %s=%s+1%s%s WHERE machine_id=%d AND bin_id=%d ORDER BY id DESC LIMIT 1;", "statistics_bins", scColumn, scColumn, recircUpdate, rejectUpdate, machine_id, destBin.getInt("bin_id"));
                    query += format("UPDATE %s SET %s=%s+1%s%s WHERE machine_id=%d AND bin_id=%d ORDER BY id DESC LIMIT 1;", "statistics_bins_counter", scColumn, scColumn, recircUpdate, rejectUpdate, machine_id, destBin.getInt("bin_id"));
                    query += format("UPDATE %s SET %s=%s+1%s%s WHERE machine_id=%d AND bin_id=%d ORDER BY id DESC LIMIT 1;", "statistics_bins_hourly", scColumn, scColumn, recircUpdate, rejectUpdate, machine_id, destBin.getInt("bin_id"));
                }

            }
        }
            //sc code finished
            HelperDatabase.runMultipleQuery(connection, query);
            logger.info("[PRODUCT][22] Product Updated. MailId=" + mail_id);

        return productInfo;
    }
    public static JSONObject handleMessage_42(Connection connection, JSONObject clientInfo, byte[] dataBytes) throws SQLException {
        int machineId=clientInfo.getInt("machine_id");
        JSONObject conveyorsStates=HelperDatabase.getConveyorsStates(connection,clientInfo.getInt("machine_id"));
        JSONObject conveyors= (JSONObject) HelperConfiguration.dbBasicInfo.get("conveyors");

        byte[] stateDataBytes = Arrays.copyOfRange(dataBytes, 4, dataBytes.length);

        String query="";
        for(int i=0;i<stateDataBytes.length;i++) {
            int conveyor_id = (i + 1);
            //save only from conveyors table
            if(conveyors.has(machineId+"_"+conveyor_id)) {
                if (conveyorsStates.has(machineId + "_" + conveyor_id)) {
                    JSONObject conveyorState = (JSONObject) conveyorsStates.get(machineId + "_" + conveyor_id);
                    if (conveyorState.getInt("state") != stateDataBytes[i]) {
                        query += format("UPDATE conveyors_states SET `state`='%s', `updated_at`=now()  WHERE `id`=%d;", stateDataBytes[i], conveyorState.getLong("id"));
                        query += format("INSERT INTO conveyors_states_history (`machine_id`, `conveyor_id`,`state`) VALUES (%d,%d,%d);", machineId, conveyor_id, stateDataBytes[i]);
                    }
                } else {
                    query += format("INSERT INTO conveyors_states (`machine_id`, `conveyor_id`,`state`) VALUES (%d,%d,%d);", machineId, conveyor_id, stateDataBytes[i]);
                    query += format("INSERT INTO conveyors_states_history (`machine_id`, `conveyor_id`,`state`) VALUES (%d,%d,%d);", machineId, conveyor_id, stateDataBytes[i]);
                }
            }
        }

        HelperDatabase.runMultipleQuery(connection,query);
        return conveyorsStates;
    }
    public static JSONObject handleMessage_43(Connection connection, JSONObject clientInfo, byte[] dataBytes) throws SQLException {
        int machineId=clientInfo.getInt("machine_id");
        JSONObject conveyorsStates=HelperDatabase.getConveyorsStates(connection,clientInfo.getInt("machine_id"));
        JSONObject conveyorState=new JSONObject();
        JSONObject conveyors= (JSONObject) HelperConfiguration.dbBasicInfo.get("conveyors");
        int conveyor_id = (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 0, 2));
        int state=dataBytes[2];
        String query="";
        if(conveyors.has(machineId+"_"+conveyor_id)){
            if(conveyorsStates.has(machineId+"_"+conveyor_id)){
                conveyorState= (JSONObject) conveyorsStates.get(machineId+"_"+conveyor_id);
                if(conveyorState.getInt("state")!=state){
                    query+=format("UPDATE conveyors_states SET `state`='%s', `updated_at`=now()  WHERE `id`=%d;",state,conveyorState.getLong("id"));
                    query+= format("INSERT INTO conveyors_states_history (`machine_id`, `conveyor_id`,`state`) VALUES (%d,%d,%d);",machineId,conveyor_id,state);
                }
            }
            else{
                query+= format("INSERT INTO conveyors_states (`machine_id`, `conveyor_id`,`state`) VALUES (%d,%d,%d);",machineId,conveyor_id,state);
                query+= format("INSERT INTO conveyors_states_history (`machine_id`, `conveyor_id`,`state`) VALUES (%d,%d,%d);",machineId,conveyor_id,state);
            }
        }
        HelperDatabase.runMultipleQuery(connection,query);
        return conveyorState;

    }
    public static JSONObject handleMessage_44(Connection connection, JSONObject clientInfo, byte[] dataBytes) throws SQLException {
        JSONObject productInfo=new JSONObject();

        int machineId=clientInfo.getInt("machine_id");
        long mailId = HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 0, 4));
        long sensorId = HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 4, 8));
        int sensorStatus=dataBytes[8];
        logger.info("[PRODUCT][44] sensorId= "+sensorId+". sensorStatus="+sensorStatus+". MailId="+mailId);
        if((sensorId == 1) && (sensorStatus == 1)) {
            String query="";
            String queryOldProduct=format("SELECT * FROM products WHERE machine_id=%d AND mail_id=%d;", machineId, mailId);
            JSONArray previousProductInfo=HelperDatabase.getSelectQueryResults(connection,queryOldProduct);
            if(previousProductInfo.length()>0){
                long oldProductId=previousProductInfo.getJSONObject(0).getLong("id");
                logger.info("[PRODUCT][44] Duplicate Product found. MailId="+mailId+" productId="+oldProductId);
                query+=format("INSERT INTO products_overwritten SELECT * FROM products WHERE id=%d;", oldProductId);
                query+=format("DELETE FROM products WHERE id=%d;", oldProductId);
            }
            connection.setAutoCommit(false);
            Statement stmt = connection.createStatement();
            if(query.length()>0){
                stmt.execute(query);
            }
            query = format("INSERT INTO products (`machine_id`, `mail_id`) VALUES (%d, %d);",machineId, mailId);
            stmt.executeUpdate(query,Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = stmt.getGeneratedKeys();
            if(rs.next())
            {
                productInfo.put("id",rs.getLong(1));
                logger.info("[PRODUCT][44] Inserted New Product MailId="+mailId+" ProductId:"+rs.getLong(1));
            }
            connection.commit();
            connection.setAutoCommit(true);
            rs.close();
            stmt.close();
        }
        productInfo.put("mail_id",mailId);
        return productInfo;
    }
    public static JSONObject handleMessage_46(Connection connection, JSONObject clientInfo, byte[] dataBytes) throws SQLException {
        int machineId=clientInfo.getInt("machine_id");
        JSONObject inductsStates=HelperDatabase.getInductsStates(connection,clientInfo.getInt("machine_id"));
        JSONObject inducts= (JSONObject) HelperConfiguration.dbBasicInfo.get("inducts");

        byte[] stateDataBytes = Arrays.copyOfRange(dataBytes, 4, dataBytes.length);

        String query="";
        for(int i=0;i<stateDataBytes.length;i++){
            int induct_id=(i+1);
            if(inducts.has(machineId+"_"+induct_id)){
                if(inductsStates.has(machineId+"_"+induct_id)){
                    JSONObject inductState= (JSONObject) inductsStates.get(machineId+"_"+induct_id);
                    if(inductState.getInt("state")!=stateDataBytes[i]){
                        query+=format("UPDATE inducts_states SET `state`='%s', `updated_at`=now()  WHERE `id`=%d;",stateDataBytes[i],inductState.getLong("id"));
                        query+= format("INSERT INTO inducts_states_history (`machine_id`, `induct_id`,`state`) VALUES (%d,%d,%d);",machineId,induct_id,stateDataBytes[i]);
                    }
                }
                else{
                    query+= format("INSERT INTO inducts_states (`machine_id`, `induct_id`,`state`) VALUES (%d,%d,%d);",machineId,induct_id,stateDataBytes[i]);
                    query+= format("INSERT INTO inducts_states_history (`machine_id`, `induct_id`,`state`) VALUES (%d,%d,%d);",machineId,induct_id,stateDataBytes[i]);
                }
            }
        }
        HelperDatabase.runMultipleQuery(connection,query);
        return inductsStates;
    }
    public static JSONObject handleMessage_47(Connection connection, JSONObject clientInfo, byte[] dataBytes) throws SQLException {
        int machineId=clientInfo.getInt("machine_id");
        JSONObject inductsStates=HelperDatabase.getInductsStates(connection,clientInfo.getInt("machine_id"));
        JSONObject inductState=new JSONObject();
        JSONObject inducts= (JSONObject) HelperConfiguration.dbBasicInfo.get("inducts");
        int induct_id = (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 0, 2));
        int state=dataBytes[2];
        String query="";
        //if(inducts.has(machineId+"_"+induct_id)){
            if(inductsStates.has(machineId+"_"+induct_id)){
                inductState= (JSONObject) inductsStates.get(machineId+"_"+induct_id);
                if(inductState.getInt("state")!=state){
                    query+=format("UPDATE inducts_states SET `state`='%s', `updated_at`=now()  WHERE `id`=%d;",state,inductState.getLong("id"));
                    query+= format("INSERT INTO inducts_states_history (`machine_id`, `induct_id`,`state`) VALUES (%d,%d,%d);",machineId,induct_id,state);
                }
            }
            else{
                query+= format("INSERT INTO inducts_states (`machine_id`, `induct_id`,`state`) VALUES (%d,%d,%d);",machineId,induct_id,state);
                query+= format("INSERT INTO inducts_states_history (`machine_id`, `induct_id`,`state`) VALUES (%d,%d,%d);",machineId,induct_id,state);
            }
        //}
        HelperDatabase.runMultipleQuery(connection,query);
        return inductState;
    }
    public static void handleMessage_48(Connection connection, JSONObject clientInfo, byte[] dataBytes) throws SQLException {
        int machineId=clientInfo.getInt("machine_id");
        int laneId = (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 0, 4));
        List<Integer> inducts =new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7));

        if(inducts.contains(laneId)) {
            String columnName = "i" + laneId;
            String query= format("UPDATE statistics SET %s=%s+1 WHERE machine_id=%d ORDER BY id DESC LIMIT 1;",columnName,columnName,machineId);
            query+= format("UPDATE statistics_minutely SET %s=%s+1 WHERE machine_id=%d ORDER BY id DESC LIMIT 1;",columnName,columnName,machineId);
            query+= format("UPDATE statistics_hourly SET %s=%s+1 WHERE machine_id=%d ORDER BY id DESC LIMIT 1;",columnName,columnName,machineId);
            query+= format("UPDATE statistics_counter SET %s=%s+1 WHERE machine_id=%d ORDER BY id DESC LIMIT 1;",columnName,columnName,machineId);
            HelperDatabase.runMultipleQuery(connection,query);
        }
        else {
            logger.error("[48] Invalid LaneId: "+laneId);
        }
    }
    public static void handleMessage_49(Connection connection, JSONObject clientInfo, byte[] dataBytes){
        int machineId=clientInfo.getInt("machine_id");
        int motorCount = (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 0, 4));
        for(int i=0;i<motorCount;i++){
            HelperConfiguration.motorsCurrentSpeed.put(machineId+"_"+(i+1),(int)  HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 4+i*2, 6+i*2)));
        }
    }
    public static JSONObject handleMessage_53(Connection connection, JSONObject clientInfo, byte[] dataBytes) throws SQLException {
        int machineId=clientInfo.getInt("machine_id");
        JSONObject outputsStates=HelperDatabase.getOutputsStates(connection,clientInfo.getInt("machine_id"));
        byte []bits=HelperCommon.bitsFromBytes(dataBytes,4);
        String query="";
        for(int i=0;i<bits.length;i++){
            if(outputsStates.has(machineId+"_"+(i+1))){
                JSONObject outputState= (JSONObject) outputsStates.get(machineId+"_"+(i+1));
                if(outputState.getInt("state")!=bits[i]){
                    query+= format("UPDATE outputs_states SET `state`=%d,`updated_at`=now() WHERE id=%d;",bits[i],outputState.getLong("id"));
                }
            }
            else{
                query+= format("INSERT INTO outputs_states (`machine_id`, `output_id`,`state`) VALUES (%d,%d,%d);",machineId,(i+1),bits[i]);
            }
        }
        HelperDatabase.runMultipleQuery(connection,query);
        return outputsStates;
    }
    public static int handleMessage_54(Connection connection, JSONObject clientInfo, byte[] dataBytes) throws SQLException {
        int machine_id=clientInfo.getInt("machine_id");
        int param_id = (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 0, 4));
        int value = (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 4, 8));
        String updateQuery = format("UPDATE parameters SET value=%d,`updated_at`=NOW() WHERE machine_id=%d AND param_id=%d AND value!=%d;",value,machine_id,param_id,value);
        return HelperDatabase.runUpdateQuery(connection,updateQuery);

    }
    public static void handleMessage_55(Connection connection, ClientForSM clientForSM, byte[] dataBytes) throws SQLException {
        int machine_id=clientForSM.clientInfo.getInt("machine_id");
        JSONObject parametersValues=HelperDatabase.getParametersValues(connection,machine_id);
        for(String key:parametersValues.keySet()){
            JSONObject row=parametersValues.getJSONObject(key);
            int paramId = row.getInt("param_id");
            int value = row.getInt("value");
            //messageId==115
            byte[] messageBytes= new byte[]{
                    0, 0, 0, 115, 0, 0, 0, 20,0,0,0,0,
                    (byte) (paramId >> 24),(byte) (paramId >> 16),(byte) (paramId >> 8),(byte) (paramId),
                    (byte) (value >> 24),(byte) (value >> 16),(byte) (value >> 8),(byte) (value)
            };
            clientForSM.sendBytes(messageBytes);
        }
    }
    public static void handleMessage_56(Connection connection, JSONObject clientInfo, byte[] dataBytes){
        int machine_id=clientInfo.getInt("machine_id");
        int counterCount = (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 0, 4));
        for(int i=0;i<counterCount;i++){
            HelperConfiguration.countersCurrentValue.put(machine_id+"_"+(i+1),(int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 4+i*4, 8+i*4)));
        }
    }
    public static void handleMessage_57(Connection connection, JSONObject clientInfo, byte[] dataBytes) throws SQLException {
        int machine_id=clientInfo.getInt("machine_id");
        String query = "UPDATE statistics_oee SET";
        query+=String.format(" current_state= %d,", (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 0, 4)));
        query+=String.format(" average_tput= %d,", (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 4, 8)));
        query+=String.format(" max_3min_tput= %d,", (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 8, 12)));
        query+=String.format(" successful_divert_packages= %d,", (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 12, 16)));
        query+=String.format(" packages_inducted= %d,", (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 16, 20)));
        query+=String.format(" tot_sec_since_reset= %d,", (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 20, 24)));
        query+=String.format(" tot_sec_estop= %d,", (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 24, 28)));
        query+=String.format(" tot_sec_fault= %d,", (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 28, 32)));
        query+=String.format(" tot_sec_blocked= %d,", (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 32, 36)));
        query+=String.format(" tot_sec_idle= %d,", (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 36, 40)));
        query+=String.format(" tot_sec_init= %d,", (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 40, 44)));
        query+=String.format(" tot_sec_run= %d,", (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 44, 48)));
        query+=String.format(" tot_sec_starved= %d,", (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 48, 52)));
        query+=String.format(" tot_sec_held= %d,", (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 52, 56)));
        query+=String.format(" tot_sec_unconstrained= %d,", (int) HelperCommon.bytesToLong(Arrays.copyOfRange(dataBytes, 56, 60)));
        query+=String.format(" last_record= %d,", dataBytes[60]);
        query+=" updated_at=NOW()";
        query+=String.format(" WHERE machine_id=%d ORDER BY id DESC LIMIT 1;", machine_id);
        HelperDatabase.runMultipleQuery(connection,query);

    }
    public static void handleMessage_59(Connection connection, JSONObject clientInfo, byte[] dataBytes) throws SQLException {
        int machine_id=clientInfo.getInt("machine_id");
        JSONObject mpinductsStates=HelperDatabase.getMpinductsStates(connection,clientInfo.getInt("machine_id"));
        int numberOfMpinducts=HelperCommon.bytesToInt(Arrays.copyOfRange(dataBytes, 0, 4));
        String query="";
        for(int i=0;i<numberOfMpinducts;i++){
            byte[] infoDataBytes=Arrays.copyOfRange(dataBytes, 4+i*8,12+i*8);
            int mpinduct_id=HelperCommon.bytesToInt(Arrays.copyOfRange(infoDataBytes, 0, 2));
            int state=HelperCommon.bytesToInt(Arrays.copyOfRange(infoDataBytes, 2, 4));
            byte [] errorsBitData=HelperCommon.bitsFromBytes(Arrays.copyOfRange(infoDataBytes, 4, 6),2);
            int count=HelperCommon.bytesToInt(Arrays.copyOfRange(infoDataBytes, 6, 8));
            String updateQueryPart="";
            updateQueryPart += format("`%s`='%s',","state",state);
            updateQueryPart += format("`%s`='%s',","count",count);
            for(int errorBit=0;errorBit<16;errorBit++){
                updateQueryPart += format("`%s`='%s',","b"+errorBit,errorsBitData[errorBit]);
            }
            if(mpinductsStates.has(machine_id+"_"+mpinduct_id)){
                JSONObject mpinductState= (JSONObject) mpinductsStates.get(machine_id+"_"+mpinduct_id);
                query += format("UPDATE mpinducts_states SET %s `updated_at`=now()   WHERE `id`=%d;", updateQueryPart,mpinductState.getLong("id"));
            }
            else{
                updateQueryPart += format("`%s`='%s',","machine_id",machine_id);
                updateQueryPart += format("`%s`='%s',","mpinduct_id",mpinduct_id);
                query += format("INSERT INTO mpinducts_states SET %s `updated_at`=now();", updateQueryPart);
            }
        }
        HelperDatabase.runMultipleQuery(connection,query);
    }
    public static void handleMessage_60(Connection connection, JSONObject clientInfo, byte[] dataBytes) throws SQLException {
        int machine_id=clientInfo.getInt("machine_id");
        JSONObject mpinductsStates=HelperDatabase.getMpinductsStates(connection,clientInfo.getInt("machine_id"));
        System.out.println(dataBytes.length);
        String query="";
        int mpinduct_id=HelperCommon.bytesToInt(Arrays.copyOfRange(dataBytes, 0, 2));
        int state=HelperCommon.bytesToInt(Arrays.copyOfRange(dataBytes, 2, 4));
        byte [] errorsBitData=HelperCommon.bitsFromBytes(Arrays.copyOfRange(dataBytes, 4, 6),2);
        int count=HelperCommon.bytesToInt(Arrays.copyOfRange(dataBytes, 6, 8));
        String updateQueryPart="";
        updateQueryPart += format("`%s`='%s',","state",state);
        updateQueryPart += format("`%s`='%s',","count",count);
        for(int errorBit=0;errorBit<16;errorBit++){
            updateQueryPart += format("`%s`='%s',","b"+errorBit,errorsBitData[errorBit]);
        }
        if(mpinductsStates.has(machine_id+"_"+mpinduct_id)){
            JSONObject mpinductState= (JSONObject) mpinductsStates.get(machine_id+"_"+mpinduct_id);
            query += format("UPDATE mpinducts_states SET %s `updated_at`=now()   WHERE `id`=%d;", updateQueryPart,mpinductState.getLong("id"));
        }
        else{
            updateQueryPart += format("`%s`='%s',","machine_id",machine_id);
            updateQueryPart += format("`%s`='%s',","mpinduct_id",mpinduct_id);
            query += format("INSERT INTO mpinducts_states SET %s `updated_at`=now();", updateQueryPart);
        }
        HelperDatabase.runMultipleQuery(connection,query);
    }
}
