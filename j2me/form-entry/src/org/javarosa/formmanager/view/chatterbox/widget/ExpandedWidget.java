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

package org.javarosa.formmanager.view.chatterbox.widget;

import java.io.IOException;
import java.util.Vector;

import javax.microedition.lcdui.Image;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;

import org.javarosa.core.model.FormElementStateListener;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;

import de.enough.polish.multimedia.AudioPlayer;
import de.enough.polish.ui.Container;
import de.enough.polish.ui.ImageItem;
import de.enough.polish.ui.Item;
import de.enough.polish.ui.StringItem;
import de.enough.polish.ui.UiAccess;

public abstract class ExpandedWidget implements IWidgetStyleEditable {

	private StringItem prompt;
	protected Item entryWidget;
	private Container c;
	private Container fullPrompt;
	protected static boolean playAudioIfAvailable = true;

	public ExpandedWidget () {
		reset();
	}

	public void initWidget (FormEntryPrompt fep, Container c) {
		//#style container
		UiAccess.setStyle(c); //it is dubious whether this works properly; Chatterbox.babysitStyles() takes care of this for now
		
		//#style questiontext
		fullPrompt= new Container(false);
		
		//#style prompttext
		prompt = new StringItem(null, null);
		fullPrompt.add(prompt);
		
		entryWidget = getEntryWidget(fep);
		//#style textBox
		UiAccess.setStyle(entryWidget);
		
		c.add(fullPrompt);
		c.add(entryWidget);
		
		this.c = c;
	}
	
	static Player player;
	public static void getAudioAndPlay(FormEntryPrompt fep,SelectChoice select){
		if (!playAudioIfAvailable) return;
		String AudioURI;
		String textID;
		AudioURI = null;
		if (select == null) {		
			if (fep.getAvailableTextForms().contains(FormEntryCaption.TEXT_FORM_AUDIO)) {
				AudioURI = fep.getAudioText();
			} else {
				return;
			}	
		}else{
			textID = select.getTextID();
			if(textID == null || textID == "") return;
			
			if (fep.getSelectTextForms(select).contains(FormEntryCaption.TEXT_FORM_AUDIO)) {
				AudioURI = fep.getSelectChoiceText(select,FormEntryCaption.TEXT_FORM_AUDIO);
			} else {
				return;
			}
		}	
		try {
			Reference audRef = ReferenceManager._().DeriveReference(AudioURI);
			if(player==null || player.getState()!=player.STARTED){
				player = Manager.createPlayer(audRef.getStream(), "audio/x-wav");
				player.start();
			}else{
				System.out.println("Player busy so skipping requested audio for now:"+AudioURI);
			}
			
			System.out.flush();
			} catch (InvalidReferenceException ire) {
				throw new RuntimeException("Invalid Reference Exception when attempting to play audio at URI:"+ AudioURI);
			} catch (IOException ioe) {
				throw new RuntimeException(	"IO Exception (input cannot be read) when attempting to play audio stream with URI:"+ AudioURI);
			} catch (MediaException e) {
				throw new RuntimeException("Media format not supported! Uri: "+ AudioURI);
			}
	}
	
	/**
	 * Checks the boolean playAudioIfAvailable first.
	 */
	public static void getAudioAndPlay(FormEntryPrompt fep){
		getAudioAndPlay(fep,null);
	}
		
	public ImageItem getImageItem(FormEntryPrompt fep){
		Vector AvailForms = fep.getAvailableTextForms();
		String ILabel,IaltText;
		
		if(AvailForms.contains(FormEntryCaption.TEXT_FORM_LONG)){
			ILabel = fep.getLongText();
		}else{
			ILabel = fep.getDefaultText();
		}
		
		if(AvailForms.contains(FormEntryCaption.TEXT_FORM_SHORT)){
			IaltText = fep.getShortText();
		}else{
			IaltText = fep.getDefaultText();
		}
		Image im = getImage(fep.getImageText());
		if(im!=null){
			ImageItem imItem = new ImageItem(null,getImage(fep.getImageText()), ImageItem.LAYOUT_CENTER | ImageItem.LAYOUT_VCENTER, IaltText);
			imItem.setLayout(Item.LAYOUT_CENTER);
			return imItem;
		}else{
			return null;
		}
		
	}
	
	public Image getImage(String URI){
		if(URI != null){
			try {
				return Image.createImage(ReferenceManager._().DeriveReference(URI).getStream()); 
			} catch (IOException e) {
				throw new RuntimeException("ERROR! Cant find image at URI: "+URI);	
			} catch (InvalidReferenceException ire){
				throw new RuntimeException("Invalid Reference for image at: " +URI);
			}
		} else{
			return null;
		}
	}

	private int imageIndex=-1;
	
	private ImageItem imItem;
	
	public void refreshWidget (FormEntryPrompt fep, int changeFlags) {
		if(imItem!=null && imageIndex !=-1){	//replace an already existing image
			imItem = getImageItem(fep);
			if(imItem!=null) fullPrompt.set(imageIndex, imItem);
			imageIndex = fullPrompt.indexOf(imItem);
		}else{
			imItem = getImageItem(fep);
			if(imItem!=null) fullPrompt.add(imItem);
			imageIndex = fullPrompt.indexOf(imItem);
		}
		
		getAudioAndPlay(fep);
		prompt.setText(fep.getLongText());	
		updateWidget(fep);
		
		//don't wipe out user-entered data, even on data-changed event
		IAnswerData data = fep.getAnswerValue();
		if (data != null && changeFlags == FormElementStateListener.CHANGE_INIT) {
			setWidgetValue(data.getValue());
		}
	}

	public IAnswerData getData () {
		return getWidgetValue();
	}

	public void reset () {
		prompt = null;
		entryWidget = null;
	}

	public boolean focus () {
		//do nothing special
		return false;
	}
	
	public int getNextMode () {
		return ChatterboxWidget.NEXT_ON_MANUAL;
	}
	
	public Item getInteractiveWidget () {
		
		return entryWidget;
	}
	
	/* (non-Javadoc)
	 * @see org.javarosa.formmanager.view.chatterbox.widget.IWidgetStyle#getPinnableHeight()
	 */
	public int getPinnableHeight() {
		return prompt.getContentHeight();
	}
	
	protected abstract Item getEntryWidget (FormEntryPrompt prompt);
	protected abstract void updateWidget (FormEntryPrompt prompt);
	protected abstract void setWidgetValue (Object o);
	protected abstract IAnswerData getWidgetValue ();
	
//	private String getCaptureSupport(String media) {
//		StringBuffer sbuf = new StringBuffer(media);
//		String support = System.getProperty("supports." + media + ".capture");
//
//		if ((support != null) && (!support.trim().equals(""))) {
//			sbuf.append(" capture supported");
//			String supportRecording = System.getProperty("supports.recording");
//			if ((supportRecording != null)
//					&& (!supportRecording.trim().equals(""))) {
//				String recordingEncodings = System.getProperty(media
//						+ ".encodings");
//				if ((recordingEncodings != null)
//						&& (!recordingEncodings.trim().equals(""))) {
//					sbuf.append(", recording supported to ");
//					sbuf.append(recordingEncodings);
//				} else {
//					sbuf
//							.append(", recording not supported for this media format");
//				}
//			} else {
//				sbuf.append(", recording not supported");
//			}
//
//			String snapshotEncodings = System.getProperty(media
//					+ ".snapshot.encodings");
//
//			if (snapshotEncodings != null) {
//				sbuf.append(", snapshots supported to ");
//				sbuf.append(snapshotEncodings);
//			}
//		} else {
//			sbuf.append(" capture not supported");
//		}
//		return sbuf.toString();
//	}
}
