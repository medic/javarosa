package org.dimagi.chatscreen;

import java.util.Vector;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import org.dimagi.entity.Question;
import org.dimagi.utils.StringUtils;
import org.dimagi.utils.ViewUtils;
import org.dimagi.view.Component;
import org.dimagi.view.Widget;
import org.dimagi.view.widget.ChoiceList;

/**
 * The Frame component is the basic element of the Chat Screen
 * interface. It displays a Question in the optimal fashion
 * based on its text and widget type.
 * 
 * A Frame can be considered the UI equivilant of a Question object.
 * 
 * @author ctsims
 * @date Aug-08-2007
 *
 */
public class Frame extends Component {
	
	private Question _question;
	
	private boolean _small;
	
	private String _text = "";
	
	private int _labelWidth;
	
	private int _xBufferSize;
	
	private int _yBufferSize;
	
	private Widget _theWidget;
	
	/**
	 * Creates a new Frame for the given Question.
	 * @param theQuestion The question that this frame will represent.	
	 */
	public Frame(Question theQuestion) {
		_question = theQuestion;
		setText();
		setupWidget();
	}
	
	/**
	 * Sets the drawing mode of the frame to either large or small
	 * @param small True if the frame should be drawn in its small form, false otherwise.
	 */
	public void setDrawingModeSmall(boolean small) {
		_small = small;
		setText();
		if(_small) {
			setBackgroundColor(ViewUtils.LIGHT_GREY);
			_theWidget.setVisible(false);
		}
		else {
			setBackgroundColor(ViewUtils.WHITE);
			_theWidget.setVisible(true);
		}
		sizeFrame();
	}
	
	/**
	 * Sets the text of the widget to the proper field of the Question
	 */
	private void setText() {
		if(_small) {
			_text = _question.getShortText();
		}
		else {
			_text = _question.getLongText();
		}
	}
	
	/**
	 * Chooses the Frame's widget, based on what input type the 
	 * question requires.
	 *
	 */
	private void setupWidget() {
		switch(_question.getWidgetType()){
		case(Constants.SINGLE_CHOICE):
			ChoiceList newWidget = new ChoiceList();
			newWidget.setChoiceType(ChoiceList.SINGLE);
			
			for(int i =0 ; i < _question.getInternalArray().length ; ++i) {
				newWidget.addChoice(_question.getInternalArray()[i]);
			}
			
			_theWidget = newWidget;
			break;
		case(Constants.MULTIPLE_CHOICE):
			ChoiceList aWidget = new ChoiceList();
			aWidget.setChoiceType(ChoiceList.MULTI);
			
			for(int i =0 ; i < _question.getInternalArray().length ; ++i) {
				aWidget.addChoice(_question.getInternalArray()[i]);
			}
			
			_theWidget = aWidget;
			break;
		}
		this.add(_theWidget);
	}
	
	/**
	 * Lays out the internal elements of the form to optimal sizes. Also sets the height 
	 * of the frame to the correct size, allowing both the widget and the Label to be 
	 * seen in full.
	 */
	public void sizeFrame() {
		Font theFont = Font.getDefaultFont();
		_xBufferSize = this.getWidth()/10;
		_yBufferSize = _xBufferSize/2;
		
		if(_small) {
			_labelWidth = this.getWidth() - _xBufferSize;
		}
		else {
			_labelWidth = this.getWidth()/3 - _xBufferSize;
		}
		
		Vector splitStrings = StringUtils.splitStringByWords(_text, _labelWidth, theFont);
		
		int numLines = splitStrings.size();
		
		int labelHeight = (theFont.getHeight() * numLines) + _yBufferSize;
		
		_theWidget.setWidth(this.getWidth() - _labelWidth - _xBufferSize);
		
		_theWidget.sizeWidget();
		
		_theWidget.setX(getWidth() - _theWidget.getWidth());
		_theWidget.setY(0);
		
		if(_theWidget.getHeight() < labelHeight || _small) {
			this.setHeight(labelHeight);	
			_theWidget.setHeight(labelHeight);
		}
		else {
			this.setHeight(_theWidget.getHeight());
		}
	}
	
	/**
	 * Manually splits the string that will be displayed, and draws it to the
	 * proper place in the frame
	 * @param g the graphic canvas
	 */
	public void drawInternal(Graphics g) {
		
		Font theFont = g.getFont();
		
		Vector splitStrings;
		
		splitStrings = StringUtils.splitStringByWords(_text, _labelWidth, theFont);
		
		g.setColor(ViewUtils.BLACK);
		g.drawRect(0, 0, this.getWidth(), this.getHeight());
		
		for(int i = 0; i < splitStrings.size(); ++i) {
			String stringPiece = (String)splitStrings.elementAt(i);
			g.drawString(stringPiece,_xBufferSize/2 ,
					_yBufferSize/2 + theFont.getHeight()*(i),
					Graphics.TOP|Graphics.LEFT);
		}
	}
}
