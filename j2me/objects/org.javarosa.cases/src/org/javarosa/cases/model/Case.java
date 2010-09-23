/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * 
 */
package org.javarosa.cases.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.util.restorable.Restorable;
import org.javarosa.core.model.util.restorable.RestoreUtils;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMapPoly;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xform.util.XFormAnswerDataParser;

/**
 * NOTE: All new fields should be added to the case class using the "data" class,
 * as it demonstrated by the "userid" field. This prevents problems with datatype
 * representation across versions.
 * 
 * @author Clayton Sims
 * @date Mar 19, 2009 
 *
 */
public class Case implements Persistable, Restorable, IMetaData {
	public static String STORAGE_KEY = "CASE";
	
	private String typeId;
	private String id;
	private String extId;
	private String name;
	
	private boolean closed = false;
	
	private Date dateOpened;
	
	int recordId;

	Hashtable data = new Hashtable();
	
	/**
	 * NOTE: This constructor is for serialization only.
	 */
	public Case() {
		dateOpened = new Date();
	}
	
	public Case(String name, String typeId) {
		setID(-1);
		this.name = name;
		this.typeId = typeId;
		dateOpened = new Date();
	}
	
	/**
	 * @return the typeId
	 */
	public String getTypeId() {
		return typeId;
	}

	/**
	 * @param typeId the typeId to set
	 */
	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}

	/**
	 * @return The name of this case
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param The name of this case
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return True if this case is closed, false otherwise.
	 */
	public boolean isClosed() {
		return closed;
	}

	/**
	 * @param Whether or not this case should be recorded as closed
	 */
	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	/**
	 * @return the recordId
	 */
	public int getID() {
		return recordId;
	}
	
	public void setID (int id) {
		this.recordId = id;
	}
		
	public String getUserId() {
		return (String)data.get(org.javarosa.core.api.Constants.USER_ID_KEY);
	}
	
	public void setUserId(String id) {
		data.put(org.javarosa.core.api.Constants.USER_ID_KEY, id);
	}

	/**
	 * @param id the id to set
	 */
	public void setCaseId(String id) {
		this.id = id;
	}
	
	public String getCaseId() {
		return id;
	}

	public void setExternalId (String extId) {
		this.extId = extId;
	}
	
	public String getExternalId () {
		return extId;
	}
	
	/**
	 * @return the dateOpened
	 */
	public Date getDateOpened() {
		return dateOpened;
	}

	/**
	 * @param dateOpened the dateOpened to set
	 */
	public void setDateOpened(Date dateOpened) {
		this.dateOpened = dateOpened;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
	 */
	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		typeId = ExtUtil.readString(in);
		id = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		extId = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		name = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		closed = ExtUtil.readBool(in);
		dateOpened = (Date)ExtUtil.read(in, new ExtWrapNullable(Date.class), pf);
		recordId = ExtUtil.readInt(in);
		data = (Hashtable)ExtUtil.read(in, new ExtWrapMapPoly(String.class, true), pf);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
	 */
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeString(out, typeId);
		ExtUtil.writeString(out, ExtUtil.emptyIfNull(id));
		ExtUtil.writeString(out, ExtUtil.emptyIfNull(extId));
		ExtUtil.writeString(out, ExtUtil.emptyIfNull(name));
		ExtUtil.writeBool(out, closed);
		ExtUtil.write(out, new ExtWrapNullable(dateOpened));
		ExtUtil.writeNumeric(out, recordId);
		ExtUtil.write(out, new ExtWrapMapPoly(data));

	}
	
	public void setProperty(String key, Object value) {
		this.data.put(key, value);
	}
	
	public Object getProperty(String key) {
		if("case-id".equals(key)) {
			return id;
		}
		return data.get(key);
	}

	public FormInstance exportData() {
		FormInstance dm = RestoreUtils.createDataModel(this);
		RestoreUtils.addData(dm, "case-id", id);
		RestoreUtils.addData(dm, "case-type-id", typeId);
		RestoreUtils.addData(dm, "ext-id", extId);
		RestoreUtils.addData(dm, "name", name);
		RestoreUtils.addData(dm, "dateopened", dateOpened);
		RestoreUtils.addData(dm, "closed", new Boolean(closed));
		
		for (Enumeration e = data.keys(); e.hasMoreElements(); ) {
			String key = (String)e.nextElement();
			RestoreUtils.addData(dm, "other/" + key + "/type", new Integer(RestoreUtils.getDataType(data.get(key))));
			RestoreUtils.addData(dm, "other/" + key + "/data", data.get(key));
		}
				
		return dm;

	}

	public String getRestorableType() {
		return "case";
	}

	public void importData(FormInstance dm) {
		id = (String)RestoreUtils.getValue("case-id", dm);
		typeId = (String)RestoreUtils.getValue("case-type-id", dm);		
		extId = (String)RestoreUtils.getValue("ext-id", dm);		
		name = (String)RestoreUtils.getValue("name", dm);		
		dateOpened = (Date)RestoreUtils.getValue("dateopened", dm);		
        closed = RestoreUtils.getBoolean(RestoreUtils.getValue("closed", dm));
        
        
        // Clayton Sims - Apr 14, 2009 : NOTE: this is unfortunate, but we need 
        // to be able to unparse.
        //XFormAnswerDataSerializer s = new XFormAnswerDataSerializer();
        TreeElement e = dm.resolveReference(RestoreUtils.absRef("other", dm));
        for (int i = 0; i < e.getNumChildren(); i++) {
        	TreeElement child = e.getChildAt(i);
        	String name = child.getName();
        	int dataType = ((Integer)RestoreUtils.getValue("other/"+name+"/type", dm)).intValue();
        	String flatval = (String)RestoreUtils.getValue("other/"+ name+"/data", dm);
        	if(flatval == null) {
        		flatval = "";
        	}
        	IAnswerData interpreted = XFormAnswerDataParser.getAnswerData(flatval, dataType);
        	if(interpreted != null) {
        		Object value = interpreted.getValue();
        		if(dataType == Constants.DATATYPE_CHOICE_LIST) {
        			value = new SelectMultiData((Vector)value);
        		}
        		data.put(name, value);
        	}
        }
	}

	public void templateData(FormInstance dm, TreeReference parentRef) {
		RestoreUtils.applyDataType(dm, "case-id", parentRef, String.class);
		RestoreUtils.applyDataType(dm, "case-type-id", parentRef, String.class);
		RestoreUtils.applyDataType(dm, "ext-id", parentRef, String.class);
		RestoreUtils.applyDataType(dm, "name", parentRef, String.class);
		RestoreUtils.applyDataType(dm, "dateopened", parentRef, Date.class);
		RestoreUtils.applyDataType(dm, "closed", parentRef, Boolean.class);
		
		RestoreUtils.applyDataType(dm, "other/*/type", parentRef, Integer.class);
		
		// other/* defaults to string
	}

	public Hashtable getMetaData() {
		Hashtable h = new Hashtable();
		String[] fields = getMetaDataFields();
		for (int i = 0; i < fields.length; i++) {
			String field = fields[i];
			Object value = getMetaData(field);
			if (value != null) {
				h.put(field, value);
			}
		}
		return h;
	}

	public Object getMetaData(String fieldName) {
		if (fieldName.equals("case-id")) {
			return id;
		} else if (fieldName.equals("case-type")) {
			return typeId;
		} else if (fieldName.equals("external-id")) {
			return extId;
		} else {
			throw new IllegalArgumentException("No metadata field " + fieldName  + " in the case storage system");
		}
	}

	public String[] getMetaDataFields() {
		return new String[] {"case-id", "case-type", "external-id"};
	}

}
