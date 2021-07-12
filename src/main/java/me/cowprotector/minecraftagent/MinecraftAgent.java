package me.cowprotector.minecraftagent;
// GRB, 20210712, implement a server/proxy to process requests being sent to or received from
// a Minecraft server equipped with a MQTT interface.

// most mqtt code copied from the Paho example app found at:
// https://github.com/eclipse/paho.mqtt.java/tree/master/org.eclipse.paho.sample.mqttv3app/src/main/java/org/eclipse/paho/sample/mqttv3app

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.io.IOException;
import java.sql.Timestamp;

public class MinecraftAgent implements MqttCallback {

    public static void main(String[] args) {

        String broker = "tcp://localhost:1883";
        String clientId = "MinecraftAgent";
        boolean cleanSession = true;			// Non durable subscriptions
        boolean quietMode 	 = false;			// log debug to console
        String password = null;
        String userName = null;
        String topic 	= "minecraft";
        int qos 		= 2;

        // TODO process command args and/or config file

        // With a valid set of arguments, the real work of
        // driving the client API can begin
        try {
            MinecraftAgent MAclient = new MinecraftAgent(broker, clientId, cleanSession, quietMode, userName, password);
            MAclient.subscribe(topic,qos);
        } catch(MqttException me) {
            // Display full details of any exception that occurs
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }

    // Private instance variables
    private MqttClient client;
    private MqttConnectOptions conOpt;

    private String 	brokerUrl;
    private boolean quietMode;
    private boolean clean;
    private String  password;
    private String  userName;

    public MinecraftAgent(String brokerUrl, String clientId, boolean cleanSession, boolean quietMode, String userName, String password) throws MqttException {
        this.brokerUrl = brokerUrl;
        this.quietMode = quietMode;
        this.clean 	   = cleanSession;
        this.password = password;
        this.userName = userName;

        String tmpDir = System.getProperty("java.io.tmpdir");
        MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);

        try {
            conOpt = new MqttConnectOptions();
            conOpt.setCleanSession(clean);
            if(password != null ) {
                conOpt.setPassword(this.password.toCharArray());
            }
            if(userName != null) {
                conOpt.setUserName(this.userName);
            }

            // Construct an MQTT blocking mode client
            client = new MqttClient(this.brokerUrl,clientId, dataStore);

            // Set this wrapper as the callback handler
            client.setCallback(this);

        } catch (MqttException e) {
            e.printStackTrace();
            log("Unable to set up client: "+e.toString());
            System.exit(1);
        }

    }

    /**
     * Subscribe to a topic on an MQTT server
     * Once subscribed this method waits for the messages to arrive from the server
     * that match the subscription.
     * @param topicName to subscribe to (can be wild carded)
     * @param qos the maximum quality of service to receive messages at for this subscription
     * @throws MqttException
     */
    public void subscribe(String topicName, int qos) throws MqttException {

        // Connect to the MQTT server
        client.connect(conOpt);
        log("Connected to "+brokerUrl+" with client ID "+client.getClientId());

        // Subscribe to the requested topic
        // The QoS specified is the maximum level that messages will be sent to the client at.
        // For instance if QoS 1 is specified, any messages originally published at QoS 2 will
        // be downgraded to 1 when delivering to the client but messages published at 1 and 0
        // will be received at the same level they were published at.
        log("Subscribing to topic \""+topicName+"\" qos "+qos);
        client.subscribe(topicName, qos);
    }

    private void log(String message) {
        if (!quietMode) {
            System.out.println(message);
        }
    }

    //
    // MqttCallback interface
    //

    public void messageArrived(String topic, MqttMessage message) throws MqttException {
        // Called when a message arrives from the server that matches any
        // subscription made by the client
        String msg = new String(message.getPayload());
        String time = new Timestamp(System.currentTimeMillis()).toString();
        log("Time:\t" +time +
                "  Topic:\t" + topic +
                "  Message:\t" + msg +
                "  QoS:\t" + message.getQos());

        // process the request!
        String args[] = msg.split(":");
        switch(args[0].toLowerCase()) {
            case "facebook":
            case "fb":
                log("Processing Facebook request: "+args[1]);
                break;
            case "twitter":
            case "tw":
                log("Processing Twitter request: "+args[1]);
                break;
            default:
                log("Unknown request");
                break;
        }
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
        log("Delivery complete");
    }

    public void connectionLost(Throwable cause) {
        // Called when the connection to the server has been lost.
        // An application may choose to implement reconnection
        // logic at this point. This sample simply exits.
        log("Connection to " + brokerUrl + " lost!" + cause);
        System.exit(1);
    }
}
