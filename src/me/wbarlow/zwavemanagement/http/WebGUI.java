package me.wbarlow.zwavemanagement.http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

import org.openhab.binding.zwave.internal.protocol.ConfigurationParameter;
import org.openhab.binding.zwave.internal.protocol.SerialMessage;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.openhab.binding.zwave.internal.protocol.commandclass.*;

import me.wbarlow.zwavemanagement.logging.Logger;
import me.wbarlow.zwavemanagement.logging.LoggerFactory;

/**
 * Extending the NanoHTTPD to provide a web interface suitable for managing
 * Z-Wave devices. This will serve on port 8080 by default.
 *
 * @author Woodrow Barlow
 *
 */
public class WebGUI extends NanoHTTPD {

	private static final Logger logger = LoggerFactory.getLogger(WebGUI.class);
	private ZWaveController controller;
	private ArrayList<String> successMessages = new ArrayList<String>();
	private ArrayList<String> failureMessages = new ArrayList<String>();

	/**
	 * One entry for each command class.
	 * @author Woodrow Barlow
	 */
	public enum Control {

		/*
		 * The node dialogue will iterate through these entries to build the
		 * appropriate control from the HTML files in the res directory. To
		 * implement a new control from a CommandClass, simply add the control
		 * to this list and the HTML file to the res directory (under
		 * "controls"). The HTML filename must be the same as the enum entry
		 * except lowercase.
		 * To process the values from that control, you must add a processing
		 * method here in this file, and call that from the appropriate case
		 * in the switch statement of the serve() method. The processing method
		 * that you create should take a ZWaveNode as the first argument, and a
		 * Map<String,String> as the second argument -- the map will contain
		 * all form inputs from your control (the key for the map will be the
		 * name attribute of the form input).
		 */

		/* in alphabetical order */
		ALARM("Alarm", ZWaveAlarmCommandClass.class),
		ALARMSENSOR("Alarm Sensor", ZWaveAlarmSensorCommandClass.class),
		ASSOCIATION("Association", ZWaveAssociationCommandClass.class),
		BASIC("Basic", ZWaveBasicCommandClass.class),
		BATTERY("Battery", ZWaveBatteryCommandClass.class),
		BINARYSENSOR("Binary Sensor", ZWaveAlarmSensorCommandClass.class),
		BINARYSWITCH("Binary Switch", ZWaveBinarySwitchCommandClass.class),
		CONFIGURATION("Configuration", ZWaveConfigurationCommandClass.class),
		HAIL("Hail", ZWaveHailCommandClass.class),
		MANUFACTURERSPECIFIC("Manufacturer-Specific", ZWaveManufacturerSpecificCommandClass.class),
		METER("Meter", ZWaveMeterCommandClass.class),
		MULTILEVELSENSOR("Multi-Level Sensor", ZWaveMultiLevelSensorCommandClass.class),
		MULTILEVELSWITCH("Multi-Level Switch", ZWaveMultiLevelSwitchCommandClass.class),
		NOOPERATION("No Operation", ZWaveNoOperationCommandClass.class),
		SCENEACTIVATION("Scene Activation", ZWaveSceneActivationCommandClass.class),
		THERMOSTATFANMODE("Thermostat Fan Mode", ZWaveThermostatFanModeCommandClass.class),
		THERMOSTATEFANSTATE("Thermostat Fan State", ZWaveThermostatFanStateCommandClass.class),
		THERMOSTATMODE("Thermostat Mode", ZWaveThermostatModeCommandClass.class),
		THERMOSTATOPERATINGSTATE("Thermostat Operating State", ZWaveThermostatOperatingStateCommandClass.class),
		THERMOSTATSETPOINT("Thermostat Setpoint", ZWaveThermostatSetpointCommandClass.class),
		VERSION("Version", ZWaveVersionCommandClass.class),
		WAKEUP("Wake Up", ZWaveWakeUpCommandClass.class),
		UNKNOWN("Unknown", null);	// special case

		private String friendlyName;											// something that can be displayed to the user
		private Class<? extends ZWaveCommandClass> commandClass;				// the command class associated with this control

		private Control(String friendlyName, Class<? extends ZWaveCommandClass> commandClass) {
			this.friendlyName = friendlyName;
			this.commandClass = commandClass;
		}

		public String getFriendlyName() {
			return this.friendlyName;
		}

		public Class<? extends ZWaveCommandClass> getCommandClass() {
			return this.commandClass;
		}

		/* get the path to the HTML file which represents this control */
		public String getWebpageFilename() {
			return "res/controls/" + toString().toLowerCase() + ".html";
		}

		/* get a control enum entry from a string representing that control */
		public static Control fromString(String s) {
			if(s != null) for(Control c : Control.values())
				if(s.equalsIgnoreCase(c.toString())) return c;
			return UNKNOWN;
		}

		/* get a control enum entry from a command class */
		public static Control fromCommandClass(ZWaveCommandClass cc) {
			for(Control c : Control.values()) {
				Class<? extends ZWaveCommandClass> cClass = c.getCommandClass();
				if(cClass != null && cClass.isInstance(cc)) return c;
			}
			return UNKNOWN;
		}
	}

	/**
	 * Construct a new WebGUI on the given port.
	 * @param controller Must be initialized.
	 * @param port
	 */
	public WebGUI(ZWaveController controller, int port) {
		super(port);
		this.controller = controller;
	}

	/**
	 * Construct a new WebGUI on port 8080.
	 * @param controller Must be initialized.
	 */
	public WebGUI(ZWaveController controller) {
		// let's serve on port 8080
		this(controller, 8080);
	}

	/**
	 * Process any requests from the GET parameters, then build the webpage and
	 * send it to the client as an HTTP response.
	 */
	public Response serve(IHTTPSession session) {

		Method method = session.getMethod();
        String uri = session.getUri();
        logger.info(method + " '" + uri + "' ");
        Map<String, String> parms = session.getParms();

        /* if there is an outstanding request from a control */
        if (parms.get("node") != null && parms.get("control") != null) {

        	// determine which control made the request
        	Control control = Control.fromString(parms.get("control"));

        	// attempt to load the specified node
        	ZWaveNode node = null;
        	try {
        		int nodeId = Integer.valueOf(parms.get("node"));
        		node = this.controller.getNode(nodeId);
        	}
        	catch(NumberFormatException e) {
        		logger.error(control.getFriendlyName() + " Control: Invalid Node Id (must be an integer).");
        		failureMessages.add(control.getFriendlyName() + " Control: Invalid Node Id (must be an integer).");
        	}

        	// if the node wasn't loaded successfully, print an error
        	if(node == null) {
        		logger.error(control.getFriendlyName() + " Control: Node " + parms.get("node") + " not found.");
        		failureMessages.add(control.getFriendlyName() + " Control: Node " + parms.get("node") + " not found.");
        	}
        	// if the node was loaded successfully, call the appropriate processing method for this control
        	else switch(control) {
        		case ASSOCIATION:
        			processAssociationControl(node, parms);
        			break;
        		case BASIC:
        			processBasicControl(node, parms);
        			break;
        		case BINARYSWITCH:
        			processBinarySwitchControl(node, parms);
        			break;
        		case CONFIGURATION:
        			processConfigurationControl(node, parms);
        			break;
        		case MULTILEVELSWITCH:
        			processMultiLevelSwitchControl(node, parms);
        			break;
        		case THERMOSTATFANMODE:
        			processThermostatFanModeControl(node, parms);
        			break;
        		case THERMOSTATMODE:
        			processThermostatModeControl(node, parms);
        			break;
        		case THERMOSTATSETPOINT:
        			processThermostatSetpointControl(node, parms);
        			break;
        		case VERSION:
        			processVersionControl(node, parms);
        			break;
        		case WAKEUP:
        			processWakeUpControl(node, parms);
        			break;
				default:
					logger.error("Processing for control type " + control.getFriendlyName() + " is not yet supported.");
					failureMessages.add("Processing for control type " + control.getFriendlyName() + " is not yet supported.");
					break;
        	}
        }

        /* build the webpage */
        String nodeIdStr = parms.get("node"); // this is allowed to be null
        String msg = buildHeader();
        if(this.controller != null) {
	        msg += buildSelectionMenu(nodeIdStr); // for choosing which node to manipulate
	        msg += buildFeedback();               // for displaying queued failure and success messages
	        msg += buildNodeDialogue(nodeIdStr);  // methods to build each individual control are also called in here
        }
        else {
        	// the web interface probably wouldn't even have launched if this were the case, but just in case...
        	logger.error("There doesn't seem to be a recognized Z-Wave controller connected to the iMG.");
        	failureMessages.add("There doesn't seem to be a recognized Z-Wave controller connected to the iMG.");
        	msg += buildFeedback();
        }
        msg += buildFooter();

        /* send the webpage as a response */
        return new NanoHTTPD.Response(msg);
	}

	/**
	 * Load a text file, return its contents as a String.
	 * @param filename Relative to this class' package.
	 * @return
	 */
	private String loadFileAsString(String filename) {
		String s = "";
		try {
			StringBuffer fileData = new StringBuffer();
	        BufferedReader reader = new BufferedReader(
	        		new InputStreamReader(WebGUI.class.getResourceAsStream(filename)));
	        char[] buf = new char[1024];
	        int numRead=0;
	        while((numRead=reader.read(buf)) != -1){
	            String readData = String.valueOf(buf, 0, numRead);
	            fileData.append(readData);
	        }
	        reader.close();
	        s += fileData.toString();
		}
		catch(Exception e) {
			logger.error("Unable to load " + filename + " as String. " + e.toString());
		}
		return s;
	}

	private ZWaveCommandClass findCommandClass(ZWaveNode node, Control control) {
		for(ZWaveCommandClass cc : node.getCommandClasses()) {
			if(Control.fromCommandClass(cc) == control) {
				return cc;
			}
		}
		return null;
	}

	/**
	 * The processing function for an association control.
	 * @param node
	 * @param parms Must contain an "action" (String: 'set' or 'remove'), an
	 * "assoc_group" (int), and an "assoc_node" (int).
	 */
	private void processAssociationControl(ZWaveNode node, Map<String,String> parms) {

		ZWaveAssociationCommandClass cc = (ZWaveAssociationCommandClass) findCommandClass(node, Control.ASSOCIATION);
		if(cc == null) {
			logger.error("Association Control: An appropriate command class was not found for this node.");
			failureMessages.add("Association Control: An appropriate command class was not found for node.");
			return;
		}

		if(parms.get("update") != null) {
			logger.error("Association Control: Cache updating not yet supported.");
			failureMessages.add("Association Control: Cache updating not yet supported.");
			// only return if update was the only parameter (besides node and command)
			if(parms.size() <= 3) return;
		}

		String action = parms.get("action");
		if(action == null || (!action.equals("set") && !action.equals("remove"))) {
			logger.error("Association Control: Invalid input (action must be 'set' or 'remove').");
			failureMessages.add("Association Control: Invalid input (action must be 'set' or 'remove').");
			return;
		}
		int groupId, nodeId;
		try {
			groupId = Integer.valueOf(parms.get("assoc_group"));
			nodeId = Integer.valueOf(parms.get("assoc_node"));
		}
		catch(NumberFormatException e) {
			logger.error("Association Control: Invalid input (group id and node id must be integers).");
			failureMessages.add("Association Control: Invalid input (group id and node id must be integers).");
			return;
		}

		SerialMessage msg;
		if(action.equals("set"))
			msg = cc.setAssociationMessage(groupId, nodeId);
		else
			msg = cc.removeAssociationMessage(groupId, nodeId);
		this.controller.sendData(msg);
		logger.info("Association Control: Command sent successfully.");
		successMessages.add("Association Control: Command sent successfully.");

	}

	/**
	 * The processing function for a basic control. It seems that all Z-Wave
	 * nodes have a basic control (and a "no operation" control).
	 * @param node
	 * @param parms Must contain a "value" (int).
	 */
	private void processBasicControl(ZWaveNode node, Map<String,String> parms) {

		ZWaveBasicCommandClass cc = (ZWaveBasicCommandClass) findCommandClass(node, Control.BASIC);
		if(cc == null) {
			logger.error("Basic Control: An appropriate command class was not found for this node.");
			failureMessages.add("Basic Control: An appropriate command class was not found for node.");
			return;
		}

		if(parms.get("update") != null) {
			SerialMessage msg = cc.getValueMessage();
			this.controller.sendData(msg);
			logger.info("Basic Control: Cache update command sent successfully.");
			successMessages.add("Basic Control: Cache update command sent successfully.");
			// only return if update was the only parameter (besides node and command)
			if(parms.size() <= 3) return;
		}

		int level;
		try {
			level = Integer.valueOf(parms.get("value"));
		}
		catch(NumberFormatException e) {
			logger.error("Basic Control: Invalid input (value must be an integer).");
			failureMessages.add("Basic Control: Invalid input (value must be an integer).");
			return;
		}

		SerialMessage msg = cc.setValueMessage(level);
		this.controller.sendData(msg);
		logger.info("Basic Control: Command sent successfully.");
		successMessages.add("Basic Control: Command sent successfully.");
	}

	/**
	 * The processing function for a switch control. This will turn a switch on
	 * or off based on the level (1 or 0).
	 * @param node
	 * @param parms Must contain a "value" (int).
	 */
	private void processBinarySwitchControl(ZWaveNode node, Map<String,String> parms) {

		ZWaveBinarySwitchCommandClass cc = (ZWaveBinarySwitchCommandClass) findCommandClass(node, Control.BINARYSWITCH);
		if(cc == null) {
			logger.error("Binary Switch Control: An appropriate command class was not found for this node.");
			failureMessages.add("Binary Switch Control: An appropriate command class was not found for this node.");
			return;
		}

		if(parms.get("update") != null) {
			SerialMessage msg = cc.getValueMessage();
			this.controller.sendData(msg);
			logger.info("Binary Switch Control: Cache update command sent successfully.");
			successMessages.add("Binary Switch Control: Cache update command sent successfully.");
			// only return if update was the only parameter (besides node and command)
			if(parms.size() <= 3) return;
		}

		int level;
		try {
			level = Integer.valueOf(parms.get("value"));
		}
		catch(NumberFormatException e) {
			logger.error("Binary Switch Control: Invalid input (value must be an integer).");
			failureMessages.add("Binary Switch Control: Invalid input (value must be an integer).");
			return;
		}

		SerialMessage msg = cc.setValueMessage(level);
		this.controller.sendData(msg);
		logger.info("Binary Switch Control: Command sent successfully.");
		successMessages.add("Binary Switch Control: Command sent successfully.");
	}

	/**
	 * The processing function for a configuration control. A configuration
	 * value can be an integer, and how this value is interpreted will be
	 * determined by the device.
	 * @param node
	 * @param parms Must contain an "index" (int), "value" (int), and "size"
	 * (int: 1, 2, or 4).
	 */
	private void processConfigurationControl(ZWaveNode node, Map<String,String> parms) {

		ZWaveConfigurationCommandClass cc = (ZWaveConfigurationCommandClass) findCommandClass(node, Control.CONFIGURATION);
		if(cc == null) {
			logger.error("Configuration Control: An appropriate command class was not found for this node.");
			failureMessages.add("Configuration Control: An appropriate command class was not found for this node.");
			return;
		}

		if(parms.get("update") != null) {
			logger.error("Configuration Control: Cache updating not yet supported.");
			failureMessages.add("Configuration Control: Cache updating not yet supported.");
			// only return if update was the only parameter (besides node and command)
			if(parms.size() <= 3) return;
		}

		int index, value, size;
		try {
			index = Integer.valueOf(parms.get("index"));
			value = Integer.valueOf(parms.get("value"));
			size = Integer.valueOf(parms.get("size"));
		}
		catch(NumberFormatException e) {
			logger.error("Configuration Control: Invalid input (index, value, and size must be integers).");
			failureMessages.add("Configuration Control: Invalid input (index, value, and size must be integers).");
			return;
		}

		ConfigurationParameter configuration;
		try {
			configuration = new ConfigurationParameter(index, value, size);
		}
		catch(IllegalArgumentException e) {
			logger.error("Configuration Control: Invalid input - " + e.getLocalizedMessage());
			failureMessages.add("Configuration Control: Invalid input - " + e.getLocalizedMessage());
			return;
		}

		SerialMessage msg = cc.setConfigMessage(configuration);
		this.controller.sendData(msg);
		logger.info("Configuration Control: Command sent successfully.");
		successMessages.add("Configuration Control: Command sent successfully.");
	}

	/**
	 * The processing function for a multi-level switch control.
	 * @param node
	 * @param parms Must contain a "value" (int).
	 */
	private void processMultiLevelSwitchControl(ZWaveNode node, Map<String,String> parms) {

		ZWaveMultiLevelSwitchCommandClass cc = (ZWaveMultiLevelSwitchCommandClass) findCommandClass(node, Control.MULTILEVELSWITCH);
		if(cc == null) {
			logger.error("Multi-Level Switch Control: An appropriate command class was not found for this node.");
			failureMessages.add("Multi-Level Switch Control: An appropriate command class was not found for node.");
			return;
		}

		if(parms.get("update") != null) {
			SerialMessage msg = cc.getValueMessage();
			this.controller.sendData(msg);
			logger.info("Multi-Level Switch Control: Cache update command sent successfully.");
			successMessages.add("Multi-Level Switch Control: Cache update command sent successfully.");
			// only return if update was the only parameter (besides node and command)
			if(parms.size() <= 3) return;
		}

		int level;
		try {
			level = Integer.valueOf(parms.get("value"));
		}
		catch(NumberFormatException e) {
			logger.error("Multi-Level Switch Control: Invalid input (level must be an integer).");
			failureMessages.add("Multi-Level Switch Control: Invalid input (level must be an integer).");
			return;
		}
		if(level < 0 || level > 255) {
			logger.error("Multi-Level Switch Control: Invalid input (level must be between 0 and 255).");
			failureMessages.add("Multi-Level Switch Control: Invalid input (level must be between 0 and 255).");
			return;
		}

		SerialMessage msg = cc.setValueMessage(level);
		this.controller.sendData(msg);
		logger.info("Multi-Level Switch Control: Command sent successfully.");
		successMessages.add("Multi-Level Switch Control: Command sent successfully.");
	}

	/**
	 * The processing function for a thermostat fan mode control.
	 * @param node
	 * @param parms Must contain a "mode" (int).
	 */
	private void processThermostatFanModeControl(ZWaveNode node, Map<String,String> parms) {

		ZWaveThermostatFanModeCommandClass cc = (ZWaveThermostatFanModeCommandClass) findCommandClass(node, Control.THERMOSTATFANMODE);
		if(cc == null) {
			logger.error("Thermostat Fan Mode Control: An appropriate command class was not found for this node.");
			failureMessages.add("Thermostat Fan Mode Control: An appropriate command class was not found for node.");
			return;
		}

		if(parms.get("update") != null) {
			logger.error("Thermostat Fan Mode Control: Cache updating not yet supported.");
			failureMessages.add("Thermostat Fan Mode Control: Cache updating not yet supported.");
			// only return if update was the only parameter (besides node and command)
			if(parms.size() <= 3) return;
		}

		int mode;
		try {
			mode = Integer.valueOf(parms.get("mode"));
		}
		catch(NumberFormatException e) {
			logger.error("Thermostat Fan Mode Control: Invalid input (mode must be an integer).");
			failureMessages.add("Thermostat Fan Mode Control: Invalid input (mode must be an integer).");
			return;
		}

		SerialMessage msg = cc.setValueMessage(mode);
		this.controller.sendData(msg);
		logger.info("Thermostat Fan Mode Control: Command sent successfully.");
		successMessages.add("Thermostat Fan Mode Control: Command sent successfully.");
	}

	/**
	 * The processing function for a thermostat mode control.
	 * @param node
	 * @param parms Must contain a "mode" (int).
	 */
	private void processThermostatModeControl(ZWaveNode node, Map<String,String> parms) {

		ZWaveThermostatModeCommandClass cc = (ZWaveThermostatModeCommandClass) findCommandClass(node, Control.THERMOSTATMODE);
		if(cc == null) {
			logger.error("Thermostat Mode Control: An appropriate command class was not found for this node.");
			failureMessages.add("Thermostat Mode Control: An appropriate command class was not found for node.");
			return;
		}

		if(parms.get("update") != null) {
			logger.error("Thermostat Mode Control: Cache updating not yet supported.");
			failureMessages.add("Thermostat Mode Control: Cache updating not yet supported.");
			// only return if update was the only parameter (besides node and command)
			if(parms.size() <= 3) return;
		}

		int mode;
		try {
			mode = Integer.valueOf(parms.get("mode"));
		}
		catch(NumberFormatException e) {
			logger.error("Thermostat Mode Control: Invalid input (mode must be an integer).");
			failureMessages.add("Thermostat Mode Control: Invalid input (mode must be an integer).");
			return;
		}

		SerialMessage msg = cc.setValueMessage(mode);
		this.controller.sendData(msg);
		logger.info("Thermostat Mode Control: Command sent successfully.");
		successMessages.add("Thermostat Mode Control: Command sent successfully.");
	}

	/**
	 * The processing function for a thermostat setpoint control.
	 * @param node
	 * @param parms Must contain a "mode" (int), a "scale" (int: 0 or 1), and a
	 * "setpoint" (double).
	 */
	private void processThermostatSetpointControl(ZWaveNode node, Map<String,String> parms) {

		ZWaveThermostatSetpointCommandClass cc = (ZWaveThermostatSetpointCommandClass) findCommandClass(node, Control.THERMOSTATSETPOINT);
		if(cc == null) {
			logger.error("Thermostat Setpoint Control: An appropriate command class was not found for this node.");
			failureMessages.add("Thermostat Setpoint Control: An appropriate command class was not found for node.");
			return;
		}

		if(parms.get("update") != null) {
			logger.error("Thermostat Setpoint Control: Cache updating not yet supported.");
			failureMessages.add("Thermostat Setpoint Control: Cache updating not yet supported.");
			// only return if update was the only parameter (besides node and command)
			if(parms.size() <= 3) return;
		}

		int mode, scale;
		BigDecimal setpoint;
		ZWaveThermostatSetpointCommandClass.SetpointType setpointType;
		try {
			mode = Integer.valueOf(parms.get("mode"));
			scale = Integer.valueOf(parms.get("scale"));
			setpoint = BigDecimal.valueOf(Double.valueOf(parms.get("setpoint")));
		}
		catch(NumberFormatException e) {
			logger.error("Thermostat Setpoint Control: Invalid input (mode and scale must be integers, setpoint must be a number).");
			failureMessages.add("Thermostat Setpoint Control: Invalid input (mode and scale must be integers, setpoint must be a number).");
			return;
		}
		if(scale != 0 && scale != 1) {
			logger.error("Thermostat Setpoint Control: Invalid input (scale must be either 0 or 1).");
			failureMessages.add("Thermostat Setpoint Control: Invalid input (scale must be either 0 or 1).");
			return;
		}
		setpointType = ZWaveThermostatSetpointCommandClass.SetpointType.getSetpointType(mode);
		if(setpointType == null) {
			logger.error("Thermostat Setpoint Control: Invalid input (mode must be 1, 2, or 7-13).");
			failureMessages.add("Thermostat Setpoint Control: Invalid input (mode must be 1, 2, or 7-13).");
			return;
		}

		SerialMessage msg = cc.setMessage(scale, setpointType, setpoint);
		this.controller.sendData(msg);
		logger.info("Thermostat Setpoint Control: Command sent successfully.");
		successMessages.add("Thermostat Setpoint Control: Command sent successfully.");
	}

	/**
	 * The processing function for a version control.
	 * @param node
	 * @param parms
	 */
	private void processVersionControl(ZWaveNode node, Map<String,String> parms) {

		ZWaveVersionCommandClass cc = (ZWaveVersionCommandClass) findCommandClass(node, Control.VERSION);
		if(cc == null) {
			logger.error("Version Control: An appropriate command class was not found for this node.");
			failureMessages.add("Version Control: An appropriate command class was not found for node.");
			return;
		}

		if(parms.get("update") != null) {
			SerialMessage msg = cc.getVersionMessage();
			this.controller.sendData(msg);
			logger.info("Version Control: Cache update command sent successfully.");
			successMessages.add("Version Control: Cache update command sent successfully.");
			// only return if update was the only parameter (besides node and command)
			if(parms.size() <= 3) return;
		}

		// this is where other processing would go, but version is read-only

	}

	/**
	 * The processing function for a thermostat setpoint control.
	 * @param node
	 * @param parms Must contain an "interval" (int).
	 */
	private void processWakeUpControl(ZWaveNode node, Map<String,String> parms) {

		ZWaveWakeUpCommandClass cc = (ZWaveWakeUpCommandClass) findCommandClass(node, Control.WAKEUP);
		if(cc == null) {
			logger.error("Wake-Up Control: An appropriate command class was not found for this node.");
			failureMessages.add("Wake-Up Control: An appropriate command class was not found for node.");
			return;
		}

		if(parms.get("update") != null) {
			SerialMessage msg = cc.getIntervalCapabilitiesMessage();
			this.controller.sendData(msg);
			msg = cc.getIntervalMessage();
			this.controller.sendData(msg);
			logger.error("Wake-Up Control: Cache updating not yet supported.");
			failureMessages.add("Wake-Up Control: Cache updating not yet supported.");
			// only return if update was the only parameter (besides node and command)
			if(parms.size() <= 3) return;
		}

		int interval;
		try {
			interval = Integer.valueOf(parms.get("interval"));
		}
		catch(NumberFormatException e) {
			logger.error("Wake-Up Control: Invalid input (interval must be an integer).");
			failureMessages.add("Wake-Up Control: Invalid input (interval must be an integer).");
			return;
		}

		SerialMessage msg = cc.setInterval(interval);
		this.controller.sendData(msg);
		logger.info("Wake-Up Control: Command sent successfully.");
		successMessages.add("Wake-Up Control: Command sent successfully.");
	}

	private String buildCSS() {

		String s = "<style type='text/css'>\n";
		s += loadFileAsString("res/css/main.css");
		s += "\n</style>\n";

		return s;
	}

	/**
	 * Builds everything that comes before the dynamic content.
	 * @return
	 */
	private String buildHeader() {

		String s = "<html>\n" +
			"<head>\n" +
			"<title>Z-Wave Web Management Interface</title>\n";
		s += buildCSS();
        s += "</head>\n" +
        	"<body>\n" +
        	"<h1><a href='/'>Z-Wave Web Management Interface</a></h1>\n";

		return s;
	}

	/**
	 * Builds the feedback dialogue. This displays any failure and success
	 * messages that were recorded since the last page load, then clears the
	 * message queues.
	 * Success messages are displayed in a span with class "success".
	 * Failure messages are displayed in a span with class "failure".
	 * @return
	 */
	private String buildFeedback() {

		String s = "<div id='feedback'>\n";
		for(String failureMessage : failureMessages) {
			s += "  <span class='failure'>" + failureMessage + "</span> <br>\n";
		}
		for(String successMessage : successMessages) {
			s += "  <span class='success'>" + successMessage + "</span> <br>\n";
		}
		s += "</div>\n";

		failureMessages.clear();
		successMessages.clear();

		return s;
	}

	/**
	 * Builds the node selection dialogue.
	 * @param nodeIdStr The currently selected node (or null).
	 * @return
	 */
	private String buildSelectionMenu(String nodeIdStr) {

		if(this.controller.getNodes().size() == 0)
			return "<span>There are no visible nodes on this Z-Wave network.</span> <br>\n";

		String s =
			"<div id='menu'>\n" +
			"<form action='?' method='get' class='selection control'>\n" +
			"  <fieldset>\n" +
			"    <legend>Select a Node</legend>\n" +
			"    <label for='selection_node'>Node:</label>\n" +
			"    <select name='node' id='selection_node'>\n";

		for(ZWaveNode node : this.controller.getNodes()) {
			String id = String.valueOf(node.getNodeId());
			String name = node.getName();
			if(name == null || name.equals("")) name = "Z-Wave Node " + id;
			if(id.equals(nodeIdStr)) s += "      <option value='" + id + "' selected>" + name + "</option>\n";
			else s += "      <option value='" + id + "'>" + name + "</option>\n";
		}

		if(nodeIdStr != null && nodeIdStr.equals("controller"))
			s += "      <option value='controller' selected>Z-Wave USB Controller</option>\n";
		else
			s += "      <option value='controller'>Z-Wave USB Controller</option>\n";
		s +=
			"    </select>\n" +
			"    <input type='submit' id='selection_submit' value='Go'>\n" +
			"  </fieldset>\n" +
			"</form>\n" +
			"</div>\n";

		return s;
	}

	/**
	 * Builds the controller information dialogue. This will display information
	 * about the controller and allow some settings to be adjusted.
	 * @return
	 */
	private String buildControllerDialogue() {

		String s =
			"<div class='controller dialogue'>\n" +
			"  <h2>Z-Wave Controller: Information and Settings</h2>\n" +
			"  <ul>\n" +
			"    <li><span>Controller Device Type:</span> " + this.controller.getControllerType().toString() + "</li>\n" +
			"    <li><span>Serial API Version:</span> " + this.controller.getSerialAPIVersion() + "</li>\n" +
			"    <li><span>Z-Wave Version:</span> " + this.controller.getZWaveVersion() + "</li>\n" +
			"  </ul>\n" +
			"</div>\n";

		return s;
	}

	/**
	 * Builds the node dialogue, including all controls for that node.
	 * If passed null as a node ID, nothing is built (and an empty string is
	 * returned).
	 * @param nodeIdStr The currently selected node (or null).
	 * @return
	 */
	private String buildNodeDialogue(String nodeIdStr) {

		/* special cases */
		if(nodeIdStr == null) return "";
		else if(nodeIdStr.equals("controller"))
			return buildControllerDialogue();

		/* attempt to read the nodeId as an int */
		int nodeId;
		try {
			nodeId = Integer.valueOf(nodeIdStr);
		}
		catch(NumberFormatException e) {
			/* if it's not an int, print an error and return */
			if(nodeIdStr != null) {
				logger.error("Node Selection: Invalid input. Node ID must be an integer.");
				failureMessages.add("Node Selection: Invalid input. Node ID must be an integer.");
			}
			return "";
		}

		/* attempt to fetch the node with the given id */
		ZWaveNode node = this.controller.getNode(nodeId);
		/* if the node doesn't exist (it may have gone to sleep or disconnected
		 * in the interim), print an error and return */
		if(node == null) {
			logger.error("Node Selection: Node " + nodeIdStr + " not found.");
			failureMessages.add("Node Selection: Node " + nodeIdStr + " not found.");
			return "";
		}

		/* if the node has a name, use that. otherwise, use "Z-Wave Node 1" */
		String nodeName = node.getName();
		if(nodeName == null || nodeName.equals("")) nodeName = "Z-Wave Node " + nodeIdStr;

		String ret =
				"<div class='node dialogue'>\n" +
	        	"  <h2>" + nodeName + "</h2>\n";

		if(node.getCommandClasses().size() == 0)
			ret += "<span>This node does not have any available controls.</span> <br>\n";

		/* go through each command class, then build controls as appropriate */
		for(ZWaveCommandClass cc : node.getCommandClasses()) {
			Control control = Control.fromCommandClass(cc);
			if(control == Control.NOOPERATION) continue;
			HashMap<String,String> vars = new HashMap<String,String>();
			vars.put("nodeid", nodeIdStr);
			vars.put("friendlycontrolname", control.getFriendlyName());
			vars.put("controlname", control.toString().toLowerCase());
			String s = buildControl(control, vars);
			ret += s;
		}

	    ret += "</div>\n";

		return ret;
	}

	/**
	 * Build a control. This involves fetching the appropriate HTML file, then
	 * replacing the appropriate variables in that file. If the appropriate HTML
	 * file is not found, unknown.html will be loaded instead.
	 * @param control
	 * @param vars a key-value map containing all variables to be replaced.
	 * Most controls require values for "nodeid", "friendlycontrolname", and
	 * "controlname".
	 * @return
	 */
	private String buildControl(Control control, Map<String,String> vars) {
		String filename = control.getWebpageFilename();
		String s = loadFileAsString(filename);
		if(s.equals("")) s = loadFileAsString("res/controls/unknown.html");
		for(Map.Entry<String, String> var : vars.entrySet())
			s = s.replaceAll("%" + var.getKey().toUpperCase() + "%", var.getValue());
		return s;
	}

	/**
	 * Builds everything that comes after the dynamic content.
	 * @return
	 */
	private String buildFooter() {

		String s = "</body>\n" +
			"</html>\n";

		return s;
	}

}
