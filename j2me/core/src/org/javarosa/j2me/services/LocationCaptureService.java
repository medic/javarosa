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
 *   An interface to describe the supported functions of all types of AudioCapture services
 * 
 *   @author Ndubisi Onuora
 */

package org.javarosa.j2me.services;

import java.util.Vector;

import org.javarosa.j2me.services.exception.LocationServiceException;

public abstract class LocationCaptureService implements DataCaptureService {
	
	public static final int NOT_INITIALISED = 0;
	public static final int READY = 1;
	public static final int WAITING_FOR_FIX = 2;
	public static final int FIX_OBTAINED = 3;
	public static final int FIX_FAILED = 4;
	
	private int state = LocationCaptureService.NOT_INITIALISED;
	
	private Vector listeners = new Vector();
	
	public int getState(){
		return state;
	}
	
	protected void setState(int newState){
		state = newState;
		notifyStateChanged();
	}
	
	
	public void addListener(LocationStateListener listener)
	{
		listeners.addElement(listener);
	}
	
	protected void notifyStateChanged()
	{
		for (int i = 0; i < listeners.size(); i++) {
			((LocationStateListener) this.listeners.elementAt(i)).onChange(getState());
		}
	}
	
	public abstract void reset();

	public abstract Fix getFix() throws LocationServiceException;

	public class Fix {
		private double lat;
		private double lon;
		private double altitude;
		private double accuracy;

		public Fix(double lat, double lon, double altitude, double accuracy) {
			super();
			this.lat = lat;
			this.lon = lon;
			this.altitude = altitude;
			this.accuracy = accuracy;
		}

		public double getLat() {
			return lat;
		}

		public double getLon() {
			return lon;
		}

		public double getAltitude() {
			return altitude;
		}

		public double getAccuracy() {
			return accuracy;
		}

	}
	
	public interface LocationStateListener {
		
		public void onChange(int status);

	}

}
