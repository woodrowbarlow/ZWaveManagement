package me.wbarlow.zwavemanagement.internal;

import java.io.IOException;

import org.openhab.binding.zwave.internal.protocol.*;
import org.openhab.binding.zwave.internal.protocol.event.*;

import me.wbarlow.zwavemanagement.http.WebGUI;
import me.wbarlow.zwavemanagement.logging.LoggerFactory;
import me.wbarlow.zwavemanagement.logging.Logger;

/**
 * This is a worker thread for the Z-Wave demo which runs for as long as the
 * bundle is in an "Active" state. The thread's lifecycle is controlled by the
 * bundle's Activator.
 * @author Woodrow Barlow
 *
 */
public class BundleThread extends Thread implements ZWaveEventListener {

	private volatile boolean active = true;

	private volatile ZWaveController controller;
	private volatile WebGUI gui;

	private boolean networkReady = false;

	private static final Logger logger = LoggerFactory.getLogger(BundleThread.class);

	/**
	 * Called when the bundle is initialized. This sets up the controller.
	 */
	public void initController() {
		try {
			this.controller = new ZWaveController(false, "/dev/ttyUSB0", 15);
			this.controller.initialize();
			this.controller.addEventListener(this);
		}
		catch(SerialInterfaceException e) {
			logger.error("Serial Interface failed to connect. ", e);
			System.exit(-1);
		}
		return;
	}

	/**
	 * Called when the bundle is initialized. This sets up the web interface.
	 * The controller must have already been initialized.
	 */
	public void initInterface() {
		this.gui = new WebGUI(this.controller);
		try { this.gui.start(); }
		catch(IOException e) {
			logger.error("Web Interface failed to start.");
			System.exit(-1);
		}
	}

	/**
	 * This is the thread's main execution loop.
	 * While this is executing, the bundle is in the "Active" state.
	 */
	public void run() {
		try {
			this.initController();
			this.initInterface();
			while (active) {
				try {
					Thread.sleep(5000);
				} catch (Exception e) {
					logger.debug("Thread interrupted " + e.getMessage());
				}
			}
		}
		catch(Exception e) {
			logger.error("An exception was thrown to the top level: ", e);
			e.printStackTrace();
		}
	}

	/**
	 * The bundle is requesting to leave the "Active" state.
	 */
	public void stopThread() {
		this.gui.stop();
		ZWaveController cntr = this.controller;
		if(cntr != null) {
			this.controller = null;
			cntr.close();
			cntr.removeEventListener(this);
		}
		active = false;
	}

	/**
	 * We've registered this class as a Z-Wave "event listener", so when the
	 * controller receives an event, this method would delegate it
	 * appropriately.
	 */
	public void ZWaveIncomingEvent(ZWaveEvent event) {

		logger.info("Incoming Z-Wave event from controller: ", event.getClass().getSimpleName());

		// TODO: the init completed event is not being sent. maybe we're missing a step.
		if(!networkReady && event instanceof ZWaveInitializationCompletedEvent) {
			// the controller has given us a thumbs-up
			logger.trace("Z-Wave Mesh Network is initialized.");
			networkReady = true;
			return;
		}

		if(event instanceof ZWaveCommandClassValueEvent) {
			ZWaveCommandClassValueEvent valueEvent = (ZWaveCommandClassValueEvent) event;
			logger.debug("Received Value Event. Node: " + valueEvent.getNodeId() + ", Command Class: " + valueEvent.getCommandClass() + ", Value: " + valueEvent.getValue());
		}
	}
}
