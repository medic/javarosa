package org.javarosa.services.transport;

import org.javarosa.services.transport.message.TransportMessage;

/**
 * 
 * A Transporter is given a TransportMessage in its constructor and has the
 * ability to send it via the send method
 * 
 * Example Transporter constructor: <code>
 * public SimpleHttpTransporter(SimpleHttpTransportMessage message) {
	this.message = message;
 * }
 * </code>
 * 
 * The TransportService spawns a thread which calls the send method. So
 * exceptions are caught and are recorded via message object methods:
 * <code>setFailureReason()</code> and <code>setStatus()</code>
 * 
 */
public interface Transporter {

	/**
	 * 
	 * (Attempt to) send the message given
	 * 
	 */
	public void send(TransportMessage message);
	
	public Transporter getTransporter();
	
	public void stop();
}
