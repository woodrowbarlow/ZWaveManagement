/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol.serialmessage;

import org.openhab.binding.zwave.internal.protocol.SerialMessage;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.SerialMessage.SerialMessageClass;
import org.openhab.binding.zwave.internal.protocol.SerialMessage.SerialMessagePriority;
import org.openhab.binding.zwave.internal.protocol.SerialMessage.SerialMessageType;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveNetworkEvent;
import me.wbarlow.zwavemanagement.logging.Logger;
import me.wbarlow.zwavemanagement.logging.LoggerFactory;

/**
 * This class processes a serial message from the zwave controller.
 * It queries the controller to see if the node is on its 'failed nodes' list.
 * @author Wez Hunter
 * @since 1.6.0
 */
public class IsFailedNodeMessageClass  extends ZWaveCommandProcessor {
	private static final Logger logger = LoggerFactory.getLogger(IsFailedNodeMessageClass.class);
		
	public SerialMessage doRequest(int nodeId) {
		logger.debug("NODE {}: Requesting IsFailedNode status from controller.", nodeId);
		SerialMessage newMessage = new SerialMessage(nodeId, SerialMessageClass.IsFailedNodeID, SerialMessageType.Request, SerialMessageClass.IsFailedNodeID, SerialMessagePriority.High);
    	byte[] newPayload = { (byte) nodeId };
    	newMessage.setMessagePayload(newPayload);
    	return newMessage;
	}

	@Override
	public boolean handleResponse(ZWaveController zController, SerialMessage lastSentMessage, SerialMessage incomingMessage) {
		int nodeId = lastSentMessage.getMessagePayloadByte(0);

		logger.trace("Handle IsFailedNode Response");

		if(incomingMessage.getMessagePayloadByte(0) != 0x00) {
			zController.notifyEventListeners(new ZWaveNetworkEvent(ZWaveNetworkEvent.Type.FailedNode, nodeId, ZWaveNetworkEvent.State.Success));
			logger.warn("NODE {}: Is currently marked as failed by the controller!", nodeId);
		}
		else {
			logger.debug("NODE {}: Is currently marked as healthy by the controller", nodeId);
		}
		
		transactionComplete = true;

		return true;
	}
}
