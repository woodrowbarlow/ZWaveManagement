/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol.initialization;

import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import me.wbarlow.zwavemanagement.logging.Logger;
import me.wbarlow.zwavemanagement.logging.LoggerFactory;

/**
 * ZWaveNodeSerializer class. Serializes nodes to XML and back again.
 * 
 * @author Jan-Willem Spuij
 * @since 1.4.0
 */
public class ZWaveNodeSerializer {

	private static final Logger logger = LoggerFactory.getLogger(ZWaveNodeSerializer.class);

	/**
	 * Constructor. Creates a new instance of the {@link ZWaveNodeSerializer}
	 * class.
	 */
	public ZWaveNodeSerializer() {
		logger.trace("Initializing ZWaveNodeSerializer.");
		logger.trace("Initialized ZWaveNodeSerializer.");
	}

	/**
	 * Serializes an XML tree of a {@link ZWaveNode}
	 * 
	 * @param node
	 *            the node to serialize
	 */
	public void SerializeNode(ZWaveNode node) {

	}

	/**
	 * Deserializes an XML tree of a {@link ZWaveNode}
	 * 
	 * @param nodeId
	 *            the number of the node to deserialize
	 * @return returns the Node or null in case Serialization failed.
	 */
	public ZWaveNode DeserializeNode(int nodeId) {
		return null;
	}
	
	/**
	 * Deletes the persistence store for the specified node.
	 * 
	 * @param nodeId The node ID to remove
	 * @return true if the file was deleted
	 */
	public boolean DeleteNode(int nodeId) {
		return false;
	}
}
