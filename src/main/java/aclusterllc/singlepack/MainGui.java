package aclusterllc.singlepack;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainGui implements ObserverSMMessage {
    public JTextArea mainTextArea;
    private JButton clearButton;
    private JPanel mainPanel;
    private JScrollPane mainScrollPane;
    private JLabel feedLabel;
    public JLabel pingLabel;
    String projectName="SinglePack";
    String projectVersion="1.0.1";
    private JCheckBox chk_log_sm_msg;
    Logger logger = LoggerFactory.getLogger(MainGui.class);

    public MainGui() {
        clearButton.addActionListener(actionEvent -> clearMainTextArea());
        chk_log_sm_msg.addActionListener(actionPerformed-> { HelperConfiguration.logSMMessages=chk_log_sm_msg.isSelected();});
    }

    public void clearMainTextArea() {
        mainTextArea.setText("");
    }
    public void appendToMainTextArea(String message){
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String displayMessage = String.format("[%s] %s",now.format(dateTimeFormatter),message);
        mainTextArea.append(displayMessage+"\r\n");
    }

    public void startGui() {
        logger.info("=====================================");
        logger.info(projectName+" "+projectVersion);
        logger.info("=====================================");

        JFrame frame = new JFrame(projectName+" "+projectVersion);
        if(Integer.parseInt(HelperConfiguration.configIni.getProperty("java_server_minimized"))==1){
            frame.setState(Frame.ICONIFIED);
        }
        frame.setContentPane(this.mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    @Override
    public void processSMMessage(JSONObject jsonMessage, JSONObject info) {
        int messageId=jsonMessage.getInt("messageId");
        ClientForSM clientForSM= (ClientForSM) jsonMessage.get("object");
        if(messageId==30){
            pingLabel.setText("\u26AB");
        }
        else if(messageId==130){
            pingLabel.setText("");
        }
        else{
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            String displayMessage = String.format("[%s] :: %s [%s][M:%s].",now.format(dateTimeFormatter),((JSONObject)HelperConfiguration.systemConstants.get("SM_MESSAGE_ID_NAME")).get(messageId+""),  messageId,clientForSM.clientInfo.get("machine_id"));
//                if(info.has("mainGuiMessage")){
//                    mainTextArea.append(info.getString("mainGuiMessage")+"\r\n");
//                }

            int SCROLL_BUFFER_SIZE = 199;
            int numLinesToTrunk = mainTextArea.getLineCount() - SCROLL_BUFFER_SIZE;
            if (numLinesToTrunk > 0) {
                try {
                    int posOfLastLineToTrunk = mainTextArea.getLineEndOffset(numLinesToTrunk - 1);
                    mainTextArea.replaceRange("", 0, posOfLastLineToTrunk);
                }
                catch (BadLocationException ex) {
                    logger.error(HelperCommon.getStackTraceString(ex));
                }
            }
            mainTextArea.append(displayMessage+"\r\n");
        }


    }
}