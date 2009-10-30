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

package org.javarosa.communication.ui;

import org.javarosa.communication.http.HttpTransportMethod;
import org.javarosa.communication.http.ui.HttpDestinationRetrievalActivity;
import org.javarosa.communication.sms.SmsTransportMethod;
import org.javarosa.communication.sms.ui.SmsDestinationRetrievalActivity;
import org.javarosa.core.api.IModule;
import org.javarosa.core.services.DataCaptureServiceRegistry;

public class CommunicationUIModule implements IModule {

	public void registerModule() {
		HttpTransportMethod http = (HttpTransportMethod)DataCaptureServiceRegistry.instance().getTransportManager().getTransportMethod(new HttpTransportMethod().getId());
		if(http != null) {
			http.setDestinationRetrievalActivity(new HttpDestinationRetrievalActivity());
		}
		SmsTransportMethod sms = (SmsTransportMethod)DataCaptureServiceRegistry.instance().getTransportManager().getTransportMethod(new SmsTransportMethod().getId());
		if(sms != null) {
			sms.setDestinationRetrievalActivity(new SmsDestinationRetrievalActivity());
		}
	}

}
