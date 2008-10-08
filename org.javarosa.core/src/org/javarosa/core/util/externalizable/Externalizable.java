package org.javarosa.core.util.externalizable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * Gives objects control over serialization. A replacement for the interfaces
 * <code>Externalizable</code> and <code>Serializable</code>, which are
 * missing in CLDC.
 * 
 * @author <a href="mailto:m.nuessler@gmail.com">Matthias Nuessler</a>
 */
public interface Externalizable {

	/**
	 * @param in
	 * @throws IOException
	 */
	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException;

	/**
	 * @param out
	 * @throws IOException
	 */
	public void writeExternal(DataOutputStream out) throws IOException;

}