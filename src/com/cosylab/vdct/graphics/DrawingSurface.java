package com.cosylab.vdct.graphics;

/**
 * Copyright (c) 2002, Cosylab, Ltd., Control System Laboratory, www.cosylab.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. 
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution. 
 * Neither the name of the Cosylab, Ltd., Control System Laboratory nor the names
 * of its contributors may be used to endorse or promote products derived 
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.awt.print.*;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.event.*;

import com.cosylab.vdct.*;
import com.cosylab.vdct.db.*;
import com.cosylab.vdct.dbd.*;
import com.cosylab.vdct.vdb.*;
import com.cosylab.vdct.undo.*;
import com.cosylab.vdct.util.StringUtils;
import com.cosylab.vdct.events.*;
import com.cosylab.vdct.events.commands.*;
import com.cosylab.vdct.graphics.objects.*;
import com.cosylab.vdct.inspector.*;
import com.cosylab.vdct.graphics.popup.*;
import com.cosylab.vdct.graphics.printing.Page;

/**
 * Insert the type's description here.
 * Creation date: (10.12.2000 13:19:16)
 * @author Matej Sekoranja
 */
public final class DrawingSurface extends Decorator implements Pageable, Printable, MouseInputListener {

	class Hiliter extends Thread {
		//private VisibleObject object;
		private int interval;
		private boolean terminated;

		public Hiliter(/*VisibleObject object,*/ int interval) {
			//this.object=object;
			this.interval = interval;
		}

		public void run() {
			while (!terminated) {

				ViewState view = ViewState.getInstance();
				if ((view == viewGroup.getLocalView())
					&& view.getBlinkingObjects().size() > 0) {
					view.setBlinkState(!view.isBlinkState());
					drawOnlyHilitedOnce = true;
					repaint();
				}

				try {
					this.sleep(interval);
				} catch (InterruptedException e) {
				};

			}
		}

		public void terminate() {
			terminated = true;
		}
	}
	private static DrawingSurface instance = null;
	// decorator support (viewport size)
	private int x0 = 0;
	private int y0 = 0;
	private int width = 0;
	private int height = 0;
	// double buffering already enabled by panel
	// adding another buffer
	private Image canvasImage;
	private Dimension canvasSize;
	private Graphics canvasGraphics;
	// navigator buffer
	private Image navigatorImage;
	private Dimension navigatorSize;
	private Graphics navigatorGraphics;
	private ViewState navigatorView;
	private Rectangle navigator;
	private Rectangle navigatorRect;
	// drawing switches
	// does not refersh devices only redraws previous buffer image
	private boolean fastDrawing = false;
	private boolean fastDrawingOnce = false;
	// does not refersh devices only draw a hilited object (executed only once)
	private boolean drawOnlyHilitedOnce = false;
	// also draws hilited objects
	private boolean alsoDrawHilitedOnce = false;
	// also force one redraw (unconditional)
	private boolean forceRedraw = false;
	// does not redraws navigator image
	private boolean blockNavigatorRedrawOnce = false;
	// current view
	//private ViewState view;
	// ... and history
	// !!!private Stack viewStateHistory = null;

	// view group
	private Group viewGroup;
	// special mouse-action switches
	private final static int NO_OPERATION = -1;
	private final static int OBJECT_SELECTION = 0;
	private final static int ZOOM_SELECTION = 1;
	private final static int ORIGIN_MOVE = 2;
	private final static int NAVIGATOR_MOVE = 3;
	private final static int LINK_OPERATION = 4;
	private final static int OBJECT_MOVE = 5;
	private int mouseOperation = NO_OPERATION;
	// cursors
	private Cursor currentCursor = null;
	private Cursor previousCursor = null;
	// cursors defs
	private Cursor defaultCursor = null;
	private Cursor handCursor = null;
	private Cursor hourCursor = null;
	// mickeys
	private int pressedX, pressedY;
	private int draggedX, draggedY;
	// modification
	private boolean modified = false;
	// linking
	private LinkSource tmplink = null;
	private Hiliter hiliter;
	
	private ComposedAction undoAction = null;
	
	private static final String newRecordString = "New record...";
	private static final String newTemplesString = "New template instance";
	private static final String addPortString = "Add port";
	private static final String newPortString = "New port...";
	private static final String newMacroString = "New macro...";
	private static final String newLineString = "New line";
	private static final String newBoxString = "New box";
	private static final String newTextBoxString = "New textbox";
	private static final String templatePropertiesString = "Template properties...";

	private Line grLine = null;
	private Box grBox = null;
	private TextBox grTextBox = null;


	private Stack viewStack = null;	
	private Stack templateStack = null;	
	
/**
 * DrawingSurface constructor comment.
 */
public DrawingSurface() {

	instance = this;
	
	setModified(false);

	ViewState view = new ViewState();
	view.setWidth(Constants.VIRTUAL_WIDTH);
	view.setHeight(Constants.VIRTUAL_HEIGHT);
	view.setScale(1.0);
	ViewState.setInstance(view);

	viewStack = new Stack();
	templateStack = new Stack();

	initializeNavigator();
 		
	currentCursor = defaultCursor = Cursor.getDefaultCursor();
	handCursor = new Cursor(Cursor.HAND_CURSOR);
	hourCursor = new Cursor(Cursor.WAIT_CURSOR);

	MouseEventManager.getInstance().subscribe("WorkspacePanel", this);

	CommandManager commandManager = CommandManager.getInstance();
	DSGUIInterface guimenu = new DSGUIInterface(this);
	commandManager.addCommand("GetVDBManager", new GetVDBManager(guimenu));
	commandManager.addCommand("GetGUIMenuInterface", new GetGUIInterface(guimenu));
	commandManager.addCommand("LinkCommand", new LinkCommand(this));
	commandManager.addCommand("GetPrintableInterface", new GetPrintableInterface(this));
	commandManager.addCommand("RepaintWorkspace", new RepaintCommand(this));

}
/**
 * Insert the method's description here.
 * Creation date: (22.4.2001 18:44:03)
 * @param action com.cosylab.vdct.undo.ActionObject
 */
public void addAction(ActionObject action) {
	UndoManager.getInstance().addAction(action);
}
/**
 * Insert the method's description here.
 * Creation date: (1.5.2001 17:32:20)
 */
public void baseView() {
	ViewState view = ViewState.getInstance();
	view.setScale(1.0);
	view.setRx((view.getWidth()-view.getViewWidth())/2);
	view.setRy((view.getHeight()-view.getViewHeight())/2);
	forceRedraw=true;
	updateWorkspaceScale();
	repaint();	
}
/**
 * Insert the method's description here.
 * Creation date: (25.12.2000 17:10:05)
 * @param g java.awt.Graphics
 */
private void copyCanvasImage(Graphics g) {
	if (canvasImage==null) redraw(g);
	else g.drawImage(canvasImage, x0, y0, null);
}
/**
 * Insert the method's description here.
 * Creation date: (26.12.2000 21:48:47)
 * @param g java.awt.Graphics
 */
private void copyNavigatorImage(Graphics g) {
	if (navigatorImage!=null)
		g.drawImage(navigatorImage, navigator.x, navigator.y, null);
}
/**
 * Insert the method's description here.
 * Creation date: (25.12.2000 22:48:10)
 */
private void createNavigatorImage() {

	if (blockNavigatorRedrawOnce)
	{
		blockNavigatorRedrawOnce = false;
		return;
	}
	
	ViewState view = ViewState.getInstance();
	
	if ((navigatorImage==null) || 
		(navigatorSize.width!=navigator.width) || 
		(navigatorSize.height!=navigator.height)) {
	
		if (navigator.width > 0 && navigator.height > 0)
		    navigatorImage = getWorkspacePanel().createImage(navigator.width, 
			    											 navigator.height);
	    if (navigatorImage==null) return;
	    navigatorSize = new Dimension(navigator.width, navigator.height);
	    navigatorGraphics = navigatorImage.getGraphics();

	    double xscale = navigator.width/(double)view.getWidth();
		double yscale = navigator.height/(double)view.getHeight();
		double nscale = Math.min(xscale, yscale);
	
		navigatorView.setScale(nscale);
	}

	ViewState.setInstance(navigatorView);
		
	navigatorGraphics.setColor(Constants.BACKGROUND_COLOR);
	navigatorGraphics.fillRect(1, 1, navigator.width - 2, navigator.height - 2);
	navigatorGraphics.setColor(Color.lightGray);
	navigatorGraphics.drawRect(0, 0, navigator.width - 1, navigator.height - 1);
	viewGroup.paintComponents(navigatorGraphics, false, isFlat());

	ViewState.setInstance(view);

	recalculateNavigatorPosition();

}
/**
 * Insert the method's description here.
 * Creation date: (10.12.2000 13:19:16)
 */
public void draw(Graphics g) {

	ViewState view = ViewState.getInstance();

	// forceReadraw does not force really
	if (fastDrawing || fastDrawingOnce || drawOnlyHilitedOnce || !forceRedraw) {
		// copy devices buffer
		copyCanvasImage(g);
		fastDrawingOnce=false;
	} else {
		forceRedraw=false;
		redraw(g);
	}

	
	// fix offset
/*	int prevRX = view.getRx(); int prevRY = view.getRy();
	view.setRx(prevRX-x0); view.setRy(prevRY-y0);*/
	double prevDRX = view.getDrx(); double prevDRY = view.getDry();
	view.setDrx(prevDRX-x0); view.setDry(prevDRY-y0);

	// draw selected
	Enumeration selected = view.getSelectedObjects().elements();
	while (selected.hasMoreElements())
		((VisibleObject)selected.nextElement()).paint(g, true);	

	// draw selected
	if (view.isBlinkState()) {
		Enumeration blinking = view.getBlinkingObjects().elements();
		while (blinking.hasMoreElements())
			((VisibleObject)blinking.nextElement()).paint(g, true);	
	}
		
	
	if ((mouseOperation==ZOOM_SELECTION) ||
		(mouseOperation==OBJECT_SELECTION)) {

		int px = Math.min(pressedX, draggedX);
		int py = Math.min(pressedY, draggedY);
		int w = Math.abs(pressedX-draggedX);
		int h = Math.abs(pressedY-draggedY);
		
		g.setColor(Color.red);
		g.drawRect(px+view.getX0(), py+view.getY0(), w, h);
	}
	else if (mouseOperation==LINK_OPERATION) {
	}
	else if (drawOnlyHilitedOnce || alsoDrawHilitedOnce) {
		
		// hilite object
		if (view.getHilitedObject()!=null) view.getHilitedObject().paint(g, true);

		drawOnlyHilitedOnce=alsoDrawHilitedOnce=false;
	}

	// restore offset
	view.setDrx(prevDRX); view.setDry(prevDRY);
	//view.setRx(prevRX); view.setRy(prevRY);

	if (Settings.getInstance().getNavigator())
		drawNavigator(g);	
}
/**
 * Insert the method's description here.
 * Creation date: (27.12.2000 15:02:15)
 */
private void drawNavigator(Graphics g) {
	if (navigatorImage==null) return;

	ViewState view = ViewState.getInstance();
	
	// draw navigator image && position
	copyNavigatorImage(g);

	Rectangle currentClip = g.getClipBounds();
	
	g.setClip(navigator.x, navigator.y, 
		  	  navigator.width, navigator.height);
	
	g.setColor(Color.red);
	g.drawRect(navigatorRect.x,
			   navigatorRect.y, 
			   navigatorRect.width, 
			   navigatorRect.height);

	g.setColor(Color.blue);
	g.drawRect(navigator.x,
			   navigator.y, 
			   (int)(view.getWidth()*navigatorView.getScale()-1), 
			   (int)(view.getHeight()*navigatorView.getScale())-1);
	
	// add lock rectangle if necessary
	final int min = 8;
	if ((navigatorRect.width<min) || 
		(navigatorRect.height<min)) {
		g.setColor(Color.lightGray);
		g.drawRect(navigatorRect.x-min,
				   navigatorRect.y-min, 
				   navigatorRect.width+2*min, 
				   navigatorRect.height+2*min);
	}
	
	g.setClip(currentClip);
}
/**
 * Insert the method's description here.
 * Creation date: (11.12.2000 16:23:31)
 */
public int getComponentHeight() {
	if (getComponent()==null) return height;
	else return getComponent().getComponentHeight();
}
/**
 * Insert the method's description here.
 * Creation date: (11.12.2000 16:23:02)
 * @return int
 */
public int getComponentWidth() {
	if (getComponent()==null) return width;
	else return getComponent().getComponentWidth();
}
/**
 * Insert the method's description here.
 * Creation date: (3.5.2001 16:17:39)
 * @return com.cosylab.vdct.graphics.DrawingSurface
 */
public static DrawingSurface getInstance() {
	return instance;
}
	/**
	 * Returns the number of pages in the set.
	 * To enable advanced printing features,
	 * it is recommended that <code>Pageable</code>
	 * implementations return the true number of pages 
	 * rather than the
	 * UNKNOWN_NUMBER_OF_PAGES constant.
	 * @return the number of pages in this <code>Pageable</code>.
	 */
public int getNumberOfPages() {

	PageFormat pageFormat = com.cosylab.vdct.graphics.printing.Page.getPageFormat();
	
	int pageWidth = (int)pageFormat.getImageableWidth();
	int pageHeight = (int)pageFormat.getImageableHeight();

	ViewState view = ViewState.getInstance();

	// screen shot
	double screen2printer = 0;
	
	switch (Page.getPrintMode()) {
		case Page.TRUE_SCALE:
			// 1:1 ratio
			screen2printer = 72.0/getWorkspacePanel().getToolkit().getScreenResolution();
			break;
			
		case Page.USER_SCALE:
			screen2printer = 72.0/getWorkspacePanel().getToolkit().getScreenResolution();
			screen2printer *= Page.getUserScale();
			break;
			
		case Page.FIT_SCALE:
			// fit to paper
			double xscale = pageWidth/(double)view.getViewWidth();
			double yscale = pageHeight/(double)view.getViewHeight();
			screen2printer = Math.min(xscale, yscale)*view.getScale();
			break;
	}
	
	double converter = screen2printer/view.getScale();
	int w = (int)(view.getViewWidth()*converter);
	int h = (int)(view.getViewHeight()*converter);

	if (w==0 || h==0)
		return 0;

	int nCol = Math.max((int)Math.ceil((double)w/pageWidth), 1);
	int nRow = Math.max((int)Math.ceil((double)h/pageHeight), 1);

	return nCol * nRow;
}
	/**
	 * Returns the <code>PageFormat</code> of the page specified by
	 * <code>pageIndex</code>.
	 * @param pageIndex the zero based index of the page whose
	 *            <code>PageFormat</code> is being requested
	 * @return the <code>PageFormat</code> describing the size and
	 *		orientation.
	 * @exception <code>IndexOutOfBoundsException</code>
	 *          the <code>Pageable</code> does not contain the requested
	 *		page.
	 */
public java.awt.print.PageFormat getPageFormat(int pageIndex) throws java.lang.IndexOutOfBoundsException {
	return com.cosylab.vdct.graphics.printing.Page.getPageFormat();
}
/**
 * Insert the method's description here.
 * Creation date: (3.2.2001 23:39:28)
 * @return int
 */
public int getPressedX() {
	return pressedX;
}
/**
 * Insert the method's description here.
 * Creation date: (3.2.2001 23:39:28)
 * @return int
 */
public int getPressedY() {
	return pressedY;
}
	/**
	 * Returns the <code>Printable</code> instance responsible for
	 * rendering the page specified by <code>pageIndex</code>.
	 * @param pageIndex the zero based index of the page whose
	 *            <code>Printable</code> is being requested
	 * @return the <code>Printable</code> that renders the page.
	 * @exception <code>IndexOutOfBoundsException</code>
	 *            the <code>Pageable</code> does not contain the requested
	 *		  page.
	 */
public java.awt.print.Printable getPrintable(int pageIndex) throws java.lang.IndexOutOfBoundsException {
	return this;
}
/**
 * Insert the method's description here.
 * Creation date: (1.5.2001 17:55:20)
 * @return com.cosylab.vdct.graphics.ViewState
 */
public ViewState getView() {
	return viewGroup.getLocalView();
}
/**
 * Insert the method's description here.
 * Creation date: (3.2.2001 22:58:19)
 * @return com.cosylab.vdct.graphics.objects.Group
 */
public com.cosylab.vdct.graphics.objects.Group getViewGroup() {
	return viewGroup;
}
/**
 * Insert the method's description here.
 * Creation date: (25.12.2000 17:31:15)
 * @return javax.swing.JComponent
 */
public javax.swing.JComponent getWorkspacePanel() {
	NullCommand pm = (NullCommand)CommandManager.getInstance().getCommand("NullCommand");
	if (pm!=null) return pm.getComponent();
	else return null;
}
/**
 * Insert the method's description here.
 * Creation date: (26.12.2000 22:18:24)
 */
private void initializeNavigator() {
	navigatorSize = new Dimension(-1, -1);
 	navigator = new Rectangle(0, 0, 0, 0);
 	navigatorRect = new Rectangle(0, 0, 0, 0);
	navigatorView = new ViewState();
/*	navigatorView.setWidth(Integer.MAX_VALUE);
	navigatorView.setHeight(Integer.MAX_VALUE);
	navigatorView.setViewWidth(Integer.MAX_VALUE);
	navigatorView.setViewHeight(Integer.MAX_VALUE);*/
}
/**
 * Insert the method's description here.
 * Creation date: (8.1.2001 18:04:49)
 */
public void initializeWorkspace() {
	
	if (isModified())
		reloadTemplate(Group.getEditingTemplateData());

	UndoManager.getInstance().reset();

	InspectorManager.getInstance().disposeAllInspectors();

	// !!! call all destory() in objects?!
	//if (Group.getRoot()!=null)
	//	Group.getRoot().getLookupTable().clear();

	// clear all stacks
	viewStack.clear();
	templateStack.clear();
	Group.setEditingTemplateData(null);

	setModified(false);
	Group group = new Group(null);
	group.setAbsoluteName("");
	group.setLookupTable(new Hashtable());
	Group.setRoot(group);
	moveToGroup(group);
	
	//Console.getInstance().flush();
	
	UndoManager.getInstance().setMonitor(true);
	blockNavigatorRedrawOnce = false;
	createNavigatorImage();
	baseView();
}
/**
 * Insert the method's description here.
 * Creation date: (1.5.2001 17:51:29)
 * @return boolean
 */
public boolean isFlat() {
	return viewGroup.getLocalView().isFlat();
}
/**
 * Insert the method's description here.
 * Creation date: (1.2.2001 14:24:51)
 * @return boolean
 */
public boolean isModified() {
	return modified;
}
/**
 * Insert the method's description here.
 * Creation date: (3.2.2001 13:31:09)
 */
public void linkCommand(VisibleObject linkObject, LinkSource linkData) {
	if (tmplink==null) {
		
		// start linking (store source field reference)
		tmplink = linkData;
		
		VisibleObject fld = (VisibleObject)Group.getRoot().getLookupTable().get(linkData.getFullName());
		if (fld!=null)
			// field found in lookup table (it is a special link field - not owned by linkManager container)
			ViewState.getInstance().setAsBlinking(fld);
		else
			ViewState.getInstance().setAsBlinking(linkObject);
		hiliter = new Hiliter(1000);
		hiliter.start();
	}
	else {
		if (!(tmplink instanceof VDBPort) &&
				(tmplink.getType() != DBDConstants.DBF_INLINK) &&
			  	(tmplink.getType() != DBDConstants.DBF_OUTLINK) &&
				(tmplink.getType() != DBDConstants.DBF_FWDLINK))
			{
				// VAR->PORT
				if (linkData instanceof VDBTemplatePort)
					tmplink.setValue(linkData.getFullName());
			}
		else if (linkData==null && linkObject instanceof Record)
			tmplink.setValue(((Record)linkObject).getName());		// fwd link to manager
		else 
			tmplink.setValue(linkData.getFullName());
			
		stopLinking();
	}
}
	/**
	 * Invoked when the mouse has been clicked on a component.
	 */
public void mouseClicked(MouseEvent e) {
	ViewState view = ViewState.getInstance();
	// check for drags!
	int cx = e.getX()-view.getX0();
	int cy = e.getY()-view.getY0();
	if ((cx>0) && (cy>0) && (cx<width) && (cy<height)) {
//		System.out.println("mouseClicked ("+cx+", "+cy+")");

		VisibleObject hilited = view.getHilitedObject();

		if (hilited!=null) {			

			// linking support
			if (tmplink!=null) {
				if (hilited instanceof Record) {
					linkCommand(hilited, null);
					return;
				}
				else if (hilited instanceof EPICSVarLink) {
					EPICSVarLink varlink = (EPICSVarLink)hilited;
					//if ((tmplink.getType()!=DBDConstants.DBF_FWDLINK) ||		// !!! proc
					//	varlink.getFieldData().getName().equalsIgnoreCase("PROC")) {
						linkCommand((LinkManagerObject)varlink.getParent(), varlink.getFieldData());
						return;
					//}
				}
				// support port -> EPICSLinkOut
				else if (tmplink instanceof VDBPort && hilited instanceof EPICSLinkOutIn) {
						EPICSLinkOutIn link = (EPICSLinkOutIn)hilited;
						linkCommand((LinkManagerObject)link.getParent(), link.getFieldData());
						return;
					//}
				}
			}
			
			if (e.isControlDown() || (e.getClickCount()==1)) {
				if (!e.isControlDown()) view.deselectAll();			// deselect all
				if (hilited instanceof Selectable) {				// invert selection
					if (view.isSelected(hilited))
						view.deselectObject(hilited);
					else
						view.setAsSelected(hilited);
					repaint();
				}
			}
			else if (e.getClickCount()>=2) {


				if (hilited instanceof Selectable) {				// invert selection
					if (view.isSelected(hilited))
						view.deselectObject(hilited);
					else
						view.setAsSelected(hilited);
				}

				// shift for template
				if (e.isShiftDown() && hilited instanceof Template) {
						descendIntoTemplate((Template)hilited);
				}
				else if (hilited instanceof Inspectable) 
					InspectorManager.getInstance().requestInspectorFor((Inspectable)hilited);
				else if (hilited instanceof Connector)
				{
					Connector con = (Connector)hilited;
					if (/*con.getMode()==OutLink.INVISIBLE_MODE &&*/ con.getInput()!=null)
					{
						centerObject((VisibleObject)con.getInput());			// !!! now is always true, but...
					}
					else if (con.getOutput()!=null /*&& con.getOutput().getMode()==OutLink.INVISIBLE_MODE*/)
					{
						centerObject((VisibleObject)con.getOutput());			// !!! now is always true, but...
					}
				} 
				else {
					if (hilited instanceof Group) {
						moveToGroup((Group)hilited);
					}
				}
			}
		}
		else {
			pressedX=cx; pressedY=cy;

			if((e.getClickCount() >= 1) && (grLine != null))
			{
				grLine = null;

					// cancel action
					if (e.getButton() == MouseEvent.BUTTON3)
						UndoManager.getInstance().undo();
			}
			else if((e.getClickCount() >= 1) && (grBox != null))
			{
				grBox = null;

					// cancel action
					if (e.getButton() == MouseEvent.BUTTON3)
						UndoManager.getInstance().undo();
			}
			else if((e.getClickCount() >= 1) && (grTextBox != null))
			{
				grTextBox.setBorder(TextBox.getCurrentBorder());
				grTextBox.showChangeTextDialog();
	
				grTextBox = null;

					// cancel action
					if (e.getButton() == MouseEvent.BUTTON3)
						UndoManager.getInstance().undo();
			}
			else if (e.getClickCount()==1 && e.getButton()==MouseEvent.BUTTON3)
			{
				showPopup(e);
			}
			else if (e.getClickCount()>=2)
			{
				// !!! to be changed...
				if(VisualDCT.getInstance().getLineButtonEnabled())
					createLine();
				else if(VisualDCT.getInstance().getBoxButtonEnabled())
					createBox();
				else if(VisualDCT.getInstance().getTextBoxButtonEnabled())
					createTextBox();
				else
					CommandManager.getInstance().execute("ShowNewDialog");
			}
			else if (view.deselectAll()) 
				repaint();					// deselect all
		}
		

		stopLinking();
			
	}
}

/**
 * Insert the method's description here.
 * Creation date: (1.5.2001 17:51:29)
 */
private void showPopup(MouseEvent e)
{
	ActionListener al = new ActionListener()
	{
		public void actionPerformed(ActionEvent e) {
			String action = e.getActionCommand();
			if (action.equals(newRecordString))
				CommandManager.getInstance().execute("ShowNewDialog");
			else if (action.equals(newPortString))
				createPort(null);
			else if (action.equals(newMacroString));
			else if (action.equals(newLineString))
				createLine();
			else if (action.equals(newBoxString))
				createBox();
			else if (action.equals(newTextBoxString))
				createTextBox();
			else if (action.equals(templatePropertiesString))
				InspectorManager.getInstance().requestInspectorFor(Group.getEditingTemplateData());
			else
				createTemplateInstance(null, action, true);
		}
	
	};
					
	ActionListener al2 = new ActionListener()
	{
		public void actionPerformed(ActionEvent e) {
			String action = e.getActionCommand();
			createPort((VDBPort)Group.getEditingTemplateData().getPorts().get(action));			
		}
	
	};

	JPopupMenu popUp = new JPopupMenu();
	
	JMenuItem item = new JMenuItem(newRecordString);
	item.addActionListener(al);
	popUp.add(item);
	
	JMenu templatesMenu = new JMenu(newTemplesString);
	popUp.add(templatesMenu);
	
	// add templates
	Enumeration templates = VDBData.getTemplates().keys();
	while (templates.hasMoreElements())
	{
		String key = templates.nextElement().toString();
		VDBTemplate t = (VDBTemplate)VDBData.getTemplates().get(key);
		JMenuItem item2 = new JMenuItem(t.getDescription());
		item2.setActionCommand(key);
		item2.addActionListener(al);
		templatesMenu.add(item2);
	
		// do not allow circular dependencies
		if (!templateStack.isEmpty() && templateStack.contains(t))
			item2.setEnabled(false);
	}	
	
	if (templatesMenu.getItemCount()==0)
		templatesMenu.setEnabled(false);
	
	if (Group.getEditingTemplateData()!=null)
	{

		popUp.add(new JSeparator());
	
		// generate visual port
		JMenu addPortMenu = new JMenu(addPortString);
		popUp.add(addPortMenu);
		
		// add templates
		Enumeration ports = Group.getEditingTemplateData().getPortsV().elements();
		while (ports.hasMoreElements())
		{
			VDBPort port = (VDBPort)ports.nextElement();
			if (port.getVisibleObject()!=null) continue; // do not add ports with already visible rep.
			JMenuItem item2 = new JMenuItem(port.getName());
			item2.setActionCommand(port.getName());
			item2.addActionListener(al2);
			addPortMenu.add(item2);
		}
		
		if (addPortMenu.getItemCount()==0)
			addPortMenu.setEnabled(false);
	
	
		// add port
		JMenuItem portMenuItem = new JMenuItem(newPortString);
		portMenuItem.addActionListener(al);
		popUp.add(portMenuItem);
	
		JMenuItem macroMenuItem = new JMenuItem(newMacroString);
		macroMenuItem.setEnabled(false);
		macroMenuItem.addActionListener(al);
		popUp.add(macroMenuItem);

	}

	popUp.add(new JSeparator());
	
	JMenuItem lineMenuItem = new JMenuItem(newLineString);
	lineMenuItem.addActionListener(al);
	popUp.add(lineMenuItem);
	
	JMenuItem boxMenuItem = new JMenuItem(newBoxString);
	boxMenuItem.addActionListener(al);
	popUp.add(boxMenuItem);
	
	JMenuItem textboxMenuItem = new JMenuItem(newTextBoxString);
	textboxMenuItem.addActionListener(al);
	popUp.add(textboxMenuItem);

	// every file is a template
	//if (isTemplateMode())
	{
		popUp.add(new JSeparator());
		JMenuItem templatePropertiesMenuItem = new JMenuItem(templatePropertiesString);
		templatePropertiesMenuItem.addActionListener(al);
		popUp.add(templatePropertiesMenuItem);
	}
	
	// add plugin items
	PopUpMenu.addPluginItems(popUp, null);

	popUp.show(getWorkspacePanel(), e.getX(), e.getY());
}

private void createTextBox()
{
	GetVDBManager manager =
		(GetVDBManager)CommandManager.getInstance().getCommand("GetVDBManager");

	grTextBox = manager.getManager().createTextBox();
	UndoManager.getInstance().addAction(new CreateAction(grTextBox));
}

private void createBox()
{
	GetVDBManager manager =
		(GetVDBManager)CommandManager.getInstance().getCommand("GetVDBManager");

	grBox = manager.getManager().createBox();
	UndoManager.getInstance().addAction(new CreateAction(grBox));
}

private void createLine()
{
	GetVDBManager manager =
		(GetVDBManager)CommandManager.getInstance().getCommand("GetVDBManager");

	grLine = manager.getManager().createLine();
	UndoManager.getInstance().addAction(new CreateAction(grLine));
}
	/**
	 * Invoked when a mouse button is pressed on a component and then 
	 * dragged.  Mouse drag events will continue to be delivered to
	 * the component where the first originated until the mouse button is
	 * released (regardless of whether the mouse position is within the
	 * bounds of the component).
	 */
public void mouseDragged(MouseEvent e) {
	ViewState view = ViewState.getInstance();
	int px = e.getX()-view.getX0();
	int py = e.getY()-view.getY0();
	if ((px>0) && (py>0) && (px<width) && (py<height)) {
		switch (mouseOperation) {

			case NAVIGATOR_MOVE : 
//System.out.println("Dragged: NAVIGATOR_MOVE");
				if (navigator.contains(e.getX(), e.getY()))	{
					int dx = (int)((px-pressedX)/navigatorView.getScale()*view.getScale()); 
					int dy = (int)((py-pressedY)/navigatorView.getScale()*view.getScale());
					if (view.moveOrigin(dx, dy)) {
						recalculateNavigatorPosition();
						repaint();
						pressedX=px; pressedY=py;
					}
				}
				break;
			case ORIGIN_MOVE : {
//System.out.println("Dragged: ORIGIN_MOVE");
				int dx = px-pressedX;
				int dy = py-pressedY;
				if (view.moveOrigin(dx, dy)) {
					recalculateNavigatorPosition();
					blockNavigatorRedrawOnce = true;
					repaint();
					pressedX=px; pressedY=py;
				}
				break; }
			
			case OBJECT_MOVE : {
//System.out.println("Dragged: OBJECT_MOVE");
				int rdx = px-draggedX;
				int rdy = py-draggedY;
				
				int dx = (int)(rdx/view.getScale()); 
				int dy = (int)(rdy/view.getScale());

				Movable hilitedObject = (Movable)view.getHilitedObject();
				
				if (view.isSelected(hilitedObject)) {
					if (moveSelection(dx, dy)) {			// move selection
						blockNavigatorRedrawOnce = true;	/// !!! performance
						repaint();
						if (dx!=0) draggedX=px-(int)(rdx-dx*view.getScale());
						if (dy!=0) draggedY=py-(int)(rdy-dy*view.getScale());
					}
				}
				else if (hilitedObject.move(dx, dy)) {
					alsoDrawHilitedOnce = true;
					blockNavigatorRedrawOnce = true;	/// !!! performance
					repaint();
					if (dx!=0) draggedX=px-(int)(rdx-dx*view.getScale());
					if (dy!=0) draggedY=py-(int)(rdy-dy*view.getScale());
				}
				break; }
				
			case OBJECT_SELECTION : 
			case ZOOM_SELECTION : 
//System.out.println("Dragged: OBJECT_SELECTION/ZOOM_SELECTION");
 			    draggedX=px; draggedY=py;
 			    repaint();
			    break;
		    
			case LINK_OPERATION :
//System.out.println("Dragged: LINK_OPERATION");
 			    draggedX=px; draggedY=py;
				break;
		
		}
	
	}

	// panning
	else if ((mouseOperation==OBJECT_MOVE) ||
			 (mouseOperation==OBJECT_SELECTION) ||
			 (mouseOperation==ZOOM_SELECTION))
	{
		int dx = 0;
		int dy = 0;
		
		if (px<0) 
			dx = -view.getGridSize();
		else if (px>width) 
			dx = view.getGridSize();
		
		if (py<0)
			dy = -view.getGridSize();
		else if (py>height)
			dy = view.getGridSize();
		
		if (view.moveOrigin(dx, dy))
		{
			if (mouseOperation==OBJECT_MOVE)
			{
				int mdx = (int)(dx/view.getScale()); 
				int mdy = (int)(dy/view.getScale());
				
				Movable hilitedObject = (Movable)view.getHilitedObject();
				if (view.isSelected(hilitedObject))
				{
					moveSelection(mdx, mdy);
				}
				else
				{
					hilitedObject.move(mdx, mdy);
					alsoDrawHilitedOnce=true;
				}
				pressedX+=dx; pressedY+=dy;
			}
			else /*if ((mouseOperation==OBJECT_SELECTION) ||
					(mouseOperation==ZOOM_SELECTION))*/
			{
				pressedX-=dx; pressedY-=dy;
				forceRedraw=true;
				fastDrawing = false;
			}
			
			recalculateNavigatorPosition();
			blockNavigatorRedrawOnce = true;
			repaint();
		}
	}
}
	/**
	 * Invoked when the mouse enters a component.
	 */
public void mouseEntered(MouseEvent e) {}
	/**
	 * Invoked when the mouse exits a component.
	 */
public void mouseExited(MouseEvent e) {}
	/**
	 * Invoked when the mouse button has been moved on a component
	 * (with no buttons no down).
	 */
public void mouseMoved(MouseEvent e)
{
	ViewState view = ViewState.getInstance();
	int cx = e.getX()-view.getX0();
	int cy = e.getY()-view.getY0();
	double scale = view.getScale();

	if(grLine != null)
	{
		grLine.getEndVertex().setX((int)((cx + view.getRx()) / scale));
		grLine.getEndVertex().setY((int)((cy + view.getRy()) / scale));
		
		repaint();
	}
	else if(grBox != null)
	{
		grBox.getEndVertex().setX((int)((cx + view.getRx()) / scale));
		grBox.getEndVertex().setY((int)((cy + view.getRy()) / scale));
		
		repaint();
	}
	else if(grTextBox != null)
	{
		grTextBox.getEndVertex().setX((int)((cx + view.getRx()) / scale));
		grTextBox.getEndVertex().setY((int)((cy + view.getRy()) / scale));
		
		repaint();
	}
	else
	{
		if ((cx>0) && (cy>0) && (cx<width) && (cy<height))
		{
			VisibleObject spotted = viewGroup.hiliteComponentsCheck(cx+view.getRx(), cy+view.getRy());
			if (ViewState.getInstance().setAsHilited(spotted))
			{
			drawOnlyHilitedOnce=true;
			repaint();
		}
	}
}
}
	/**
	 * Invoked when a mouse button has been pressed on a component.
	 */
public void mousePressed(MouseEvent e) {
	ViewState view = ViewState.getInstance();
	int px = e.getX()-view.getX0();
	int py = e.getY()-view.getY0();
	if ((px>0) && (py>0) && (px<width) && (py<height)) {
		boolean leftButtonPush = (e.getModifiers() & InputEvent.BUTTON1_MASK) != 0;
		boolean rightButtonPush  = (e.getModifiers() & InputEvent.BUTTON3_MASK) != 0;

// 		System.out.println("mousePressed("+leftButtonPush+","+rightButtonPush+")");
		
		VisibleObject hilitedObject = ViewState.getInstance().getHilitedObject();
		if (leftButtonPush && !rightButtonPush) {

			if (navigator.contains(e.getX(), e.getY())) {		// navigator
//System.out.println("Pressed: NAVIGATOR_MOVE");
				mouseOperation = NAVIGATOR_MOVE;

				int rx = (int)((e.getX()-navigatorRect.width/2.0-navigator.x)/navigatorView.getScale()*view.getScale());
				int ry = (int)((e.getY()-navigatorRect.height/2.0-navigator.y)/navigatorView.getScale()*view.getScale());

				if (view.moveOrigin(rx-view.getRx(), ry-view.getRy())) {
					recalculateNavigatorPosition();
					repaint();
					pressedX=px; pressedY=py;
				}
				
			}
			else if (e.isShiftDown()) {				// drag-move
//System.out.println("Pressed: ORIGIN_MOVE");
				mouseOperation = ORIGIN_MOVE;
				setCursor(handCursor);
				pressedX=px; pressedY=py;
			}
			else if (hilitedObject==null) {			// object selection
//System.out.println("Pressed: OBJECT_SELECTION");
				mouseOperation = OBJECT_SELECTION;
				pressedX=draggedX=px;
				pressedY=draggedY=py;
				fastDrawing=true;
			}			
			else if (hilitedObject instanceof Movable) {  // move object
//System.out.println("Pressed: OBJECT_MOVE");
				mouseOperation = OBJECT_MOVE;
				pressedX=draggedX=px; 
				pressedY=draggedY=py;
			}
			
		}
		else if (rightButtonPush && !leftButtonPush) {

			if (e.isShiftDown() && 
				(hilitedObject instanceof Rotatable)) {
					((Rotatable)hilitedObject).rotate();
					alsoDrawHilitedOnce=true;
					repaint();
			}
			else
			if (hilitedObject==null) {					// zoom selection
//System.out.println("Pressed: ZOOM_SELECTION");
				mouseOperation = ZOOM_SELECTION;
				pressedX=draggedX=px;
				pressedY=draggedY=py;
				fastDrawing=true;
			}			
			else if (hilitedObject instanceof Popupable) {

				// linking support
				if (hilitedObject instanceof LinkManagerObject) {
					//((LinkManagerObject)hilitedObject).setTarget((tmplink!=null));
					// show all for VBDPorts
					((LinkManagerObject)hilitedObject).setTargetLink(tmplink);
				}
				
				PopUpMenu.getInstance().show(
					(Popupable)hilitedObject,
					getWorkspacePanel(),
					e.getX(), e.getY()
				);

			}
		}
	
	}
}
	/**
	 * Invoked when a mouse button has been released on a component.
	 */
public void mouseReleased(MouseEvent e) {
	ViewState view = ViewState.getInstance();
	int px = e.getX()-view.getX0();
	int py = e.getY()-view.getY0();

	//boolean leftButtonPush = (e.getModifiers() & InputEvent.BUTTON1_MASK) != 0;
	//boolean rightButtonPush  = (e.getModifiers() & InputEvent.BUTTON3_MASK) != 0;
	//System.out.println("mouseReleased("+leftButtonPush+","+rightButtonPush+")");

	VisibleObject hilitedObject = ViewState.getInstance().getHilitedObject();
	//System.out.println("hilited: "+hilitedObject);

	switch (mouseOperation) {

		case ORIGIN_MOVE : 
//System.out.println("Released: ORIGIN_MOVE");
			restoreCursor();
			forceRedraw=true;
			repaint(); // jic
			break;
				
		case OBJECT_MOVE :
//System.out.println("Released: OBJECT_MOVE");
			createNavigatorImage();

			int dx = px-draggedX;
			int dy = py-draggedY;
			dx = (int)(dx/view.getScale()); 
			dy = (int)(dy/view.getScale());

			if (!(dx==0 && dy==0))
				if (view.isSelected(hilitedObject)) 
					moveSelection(dx, dy);
				else
					((Movable)hilitedObject).move(dx, dy);

			dx = (int)((px-pressedX)/view.getScale()); 
			dy = (int)((py-pressedY)/view.getScale());
			if (!(dx==0 && dy==0))
				if (view.isSelected(hilitedObject))
				{
					ComposedAction composedAction = new ComposedAction();
					Enumeration selected = view.getSelectedObjects().elements();
						while (selected.hasMoreElements())
							composedAction.addAction(new MoveAction((Movable)selected.nextElement(), dx, dy));
					UndoManager.getInstance().addAction(composedAction);
				}
				else
					addAction(new MoveAction((Movable)hilitedObject, dx, dy));
			
			/*ViewState.getInstance().setAsHilited(null);
		
			int cx = e.getX()-view.getX0();
			int cy = e.getY()-view.getY0();
			if (viewGroup.hiliteComponentsCheck(cx+view.getRx(), cy+view.getRy()))*/

			forceRedraw=true;
			//alsoDrawHilitedOnce=true;
			repaint();
			break;
			
		case OBJECT_SELECTION : 
//System.out.println("Released: OBJECT_SELECTION");
		
			fastDrawing=false;
			selectArea(pressedX, pressedY, 
			   		   draggedX, draggedY);
 		    repaint();
		    break;

		case ZOOM_SELECTION : 
//System.out.println("Released: ZOOM_SELECTION");
		
			fastDrawing=false;
			dx = (int)(Math.abs(pressedX-draggedX)/view.getScale());
			dy = (int)(Math.abs(pressedY-draggedY)/view.getScale());
			if ((dx>Constants.RECORD_HEIGHT) &&	(dy>Constants.RECORD_HEIGHT))
				zoomArea(pressedX, pressedY, 
				   		 draggedX, draggedY);
 		    repaint();
		    break;
		    
		case LINK_OPERATION :
//System.out.println("Released: LINK_OPERATION");

			fastDrawing=false;
			// link !!!
			repaint();
			break;
		
	}

	//System.out.println(" hilited: "+hilitedObject);
	mouseOperation = NO_OPERATION;
}
 		
/**
 * Insert the method's description here.
 * Creation date: (28.1.2001 18:39:57)
 */
public void moveLevelUp() {
	if (viewGroup.getParent()!=null) {
		moveToGroup((Group)viewGroup.getParent());
	}
	else if (!viewStack.isEmpty())
	{
		ascendFromTemplate();	
	}
}
/**
 * Insert the method's description here.
 * Creation date: (27.12.2000 13:03:48)
 * @return boolean
 * @param dx int
 * @param dy int
 */
private boolean moveSelection(int dx, int dy) {
	boolean ok = true;
	ViewState view = ViewState.getInstance();

	Enumeration selected = view.getSelectedObjects().elements();
	while (selected.hasMoreElements() && ok)
		ok = ((Movable)selected.nextElement()).checkMove(dx, dy);

	selected = view.getSelectedObjects().elements();
	while (selected.hasMoreElements() && ok)
		((Movable)selected.nextElement()).move(dx, dy);
		
	return ok;
}
/**
 * Insert the method's description here.
 * Creation date: (6.1.2001 22:35:40)
 * @param file java.io.File
 */
public boolean open(File file) throws IOException {
	return open(file, false);
}
/**
 * jh
 * Creation date: (6.1.2001 22:35:40)
 */
public void checkForIncodedDBDs(File file) throws IOException
{
	
	Console.getInstance().println();
	
	Vector dbds = com.cosylab.vdct.DataProvider.getInstance().getDBDs();
	String[] dbd = DBResolver.resolveIncodedDBDs(file.getAbsolutePath());
	if (dbd!=null)
	for (int i=0; i<dbd.length; i++)
	{
		File f = new File(dbd[i]);
		if (f.exists())
		{
			// skip if already loaded
			if (!dbds.contains(f))
			{
				Console.getInstance().println("Loading DBD file: '"+f.getAbsolutePath()+"'.");
				openDBD(f, com.cosylab.vdct.DataProvider.getInstance().getDbdDB()!=null);
			}
			else
				Console.getInstance().println("DBD file '"+f.getAbsolutePath()+"' is already loaded.");
		}
		else 
		{
			Console.getInstance().println("DBD file not found: '"+f.getAbsolutePath()+"'.");
			
			// add anyway
			if (!dbds.contains(f))
				dbds.addElement(f);
		}
	}
		
}

/**
 * SEPARATE DOWN CODE TO METHODS
 * Creation date: (6.1.2001 22:35:40)
 * @param file java.io.File
 */
public boolean open(File file, boolean importDB) throws IOException {

	///
	/// DBD managment
	///

	// clean list of unexisting DBDs
	File[] dbds = new File[com.cosylab.vdct.DataProvider.getInstance().getDBDs().size()];
	com.cosylab.vdct.DataProvider.getInstance().getDBDs().toArray(dbds);
	for (int i=0; i<dbds.length; i++)
		if (!dbds[i].exists())
			com.cosylab.vdct.DataProvider.getInstance().getDBDs().removeElement(dbds[i]);

	// check for in-coded DBDs
	checkForIncodedDBDs(file);


	///
	/// load DB if we have DBD 
	///

	DBDData dbdData = com.cosylab.vdct.DataProvider.getInstance().getDbdDB();
	if (dbdData != null)
	{
		setCursor(hourCursor);

		// prepare workspace
		if (!importDB)
			initializeWorkspace();
		UndoManager.getInstance().setMonitor(false);

		// load
		DBData dbData = null;
		/*if (getAppletBase()!=null) 	// applet
			try
			{
				dbData = DBResolver.resolveDBasURL(new java.net.URL(getVDCTFrame().getAppletBase(), file.getAbsolutePath()));
			} catch (java.net.MalformedURLException e) { Console.getInstance().println(e); }
		else  */

		try
		{
			dbData = DBResolver.resolveDB(file.getAbsolutePath());
		} 
		catch(Exception e)
		{
			Console.getInstance().println(e);
		}

		// check for sucess
		if ((dbData == null) || !dbdData.consistencyCheck(dbData))
		{
			restoreCursor();
			return false;
		}

		// check is DTYP fields are defined before any DBF_INPUT/DBF_OUTPUT fields...
		DBData.checkDTYPfield(dbData, dbdData);

		/*VDBData vdbData =*/ VDBData.generateVDBData(dbdData, dbData);

		//applyVisualData(importDB, viewGroup, dbData, vdbData);

		if (!importDB)
		{
			// every file is a template
			
			
			/*
			// if template file start editing template
			if (dbData.getRecords().isEmpty() &&
				dbData.getGroups().isEmpty() &&
				dbData.getTemplateInstances().isEmpty() &&
				!dbData.getTemplates().isEmpty())
			{
				// find 'first' template defined in this file (not via includes)
				VDBTemplate template = null;
				Enumeration te = dbData.getTemplates().keys();
				while (te.hasMoreElements())
				{
					template = (VDBTemplate)VDBData.getTemplates().get(te.nextElement());
					if (template!=null)
					{
						if (template.getFileName().equals(file.getAbsolutePath()))
							break;
						else
							template=null;
					}		
				}
				
				// found
				if (template!=null)
				{
					Group.setRoot(template.getGroup());
					Group.setEditingTemplateData(template);
					templateStack.push(template);

					InspectorManager.getInstance().updateObjectLists();
					moveToGroup(template.getGroup());
				}
			}
			*/
			
				// find 'first' template defined in this file (not via includes)
				VDBTemplate template = (VDBTemplate)VDBData.getTemplates().get(dbData.getTemplateData().getId());
				
				// found
				if (template!=null)
				{
					Group.setRoot(template.getGroup());
					Group.setEditingTemplateData(template);
					templateStack.push(template);

					InspectorManager.getInstance().updateObjectLists();
					moveToGroup(template.getGroup());
				}
			
			setModified(false);
			viewGroup.unconditionalValidateSubObjects(isFlat());
		}

		// !!!
		VisualDCT.getInstance().updateLoadLabel();	
		//updateWorkspaceGroup();

		blockNavigatorRedrawOnce = false;
		createNavigatorImage();
		forceRedraw = true;
		repaint();
		restoreCursor();

		UndoManager.getInstance().setMonitor(true);


		///!!! put somewhere in try-finally block
		// free db memory	    
		dbData = null;
		System.gc();

		return true;
	} 
	else
		return false;
}

/**
 * Insert the method's description here.
 */
public static void applyVisualData(boolean importDB, Group group, DBData dbData, VDBData vdbData)
{
	if (vdbData==null)
		return;
	
	// apply visual-data && generate visual object, group hierarchy
	try {

		Group rootGroup = Group.getRoot();

		ArrayList blackList = null;
		if (importDB)
			blackList = new ArrayList();

		// add records

		EPICSLink field;
		DBFieldData dbField;
		Enumeration e2;

		Record record;
		DBRecordData dbRec = null;
		VDBRecordData vdbRec = null;

		// not all inter-template links were created (not all fields were loaded in the lookup table)
		ArrayList tobeUpdated = new ArrayList();
		// add template instances and apply their visual data
		VDBTemplateInstance vdbTemplate;
		DBTemplateInstance dbTemplate;



		// add records, template instances and entries
		Enumeration e = vdbData.getStructure().elements();
		while (e.hasMoreElements())
		{
			Object obj = e.nextElement();
			if (obj instanceof VDBRecordData)
			{

				vdbRec = (VDBRecordData)obj;
				dbRec = (DBRecordData) dbData.getRecords().get(vdbRec.getName());
				// check if record already exists
				if ((record = (Record) rootGroup.findObject(vdbRec.getName(), true))
					!= null) {
					Console.getInstance().println(
						"Record "
							+ vdbRec.getName()
							+ " already exists - this definition will be ignored.");
					blackList.add(record);
					continue;
				}
	
				record = new Record(null, vdbRec, dbRec.getX(), dbRec.getY());
				record.setRight(dbRec.isRotated());
				record.setColor(dbRec.getColor());
				record.setDescription(dbRec.getDescription());
	
				// add fields (preserve visual order)
				e2 = dbRec.getVisualFieldsV().elements();
				while (e2.hasMoreElements()) {
					dbField = (DBFieldData) (e2.nextElement());
					if (dbField.isHasAdditionalData()) {
						field = record.initializeLinkField(vdbRec.getField(dbField.getName()));
						field.setColor(dbField.getColor());
						field.setRight(dbField.isRotated());
						field.setDescription(dbField.getDescription());
					}
				}
	
				group.addSubObject(vdbRec.getName(), record, true);

			}
			else if (obj instanceof VDBTemplateInstance)
			{
				vdbTemplate = (VDBTemplateInstance)obj;
				
				dbTemplate = (DBTemplateInstance)dbData.getTemplateInstances().get(vdbTemplate.getName());
	
				VDBTemplate template = (VDBTemplate)vdbData.getTemplates().get(dbTemplate.getTemplateId());
				if (template==null)
				{
					/*// already issued
					Console.getInstance().println(
						"Template instance "+dbTemplate.getTemplateID()+" cannot be created since "
							+ dbTemplate.getTemplateClassID()
							+ " does not exist - this definition will be ignored.");*/
					continue;
				}			
				
				VDBTemplateInstance templateInstance = (VDBTemplateInstance)vdbData.getTemplateInstances().get(dbTemplate.getTemplateInstanceId());
				if (templateInstance==null)
				{
					Console.getInstance().println(
						"Template instance "+dbTemplate.getTemplateInstanceId()+" does not exist - this definition will be ignored.");
					continue;
				}			
	
				Template templ = new Template(null, templateInstance);
				group.addSubObject(dbTemplate.getTemplateInstanceId(), templ, true);
	
				//templ.setDescription(dbTemplate.getDescription());
				//templ.setDescription(template.getDescription());
				templ.setColor(dbTemplate.getColor());
				templ.move(dbTemplate.getX(), dbTemplate.getY());
				//templ.forceValidation();
				
				tobeUpdated.add(templ);
			}
			else if (obj instanceof DBEntry)
			{
				group.getStructure().addElement(obj);
			}
			
		}

		// add groups (if not already created) and apply visual data

		Group grp;
		DBGroupData dbGrp;
		e = dbData.getGroups().elements();
		while (e.hasMoreElements()) {
			dbGrp = (DBGroupData) (e.nextElement());
			grp = (Group) rootGroup.findObject(dbGrp.getName(), true);
			if (importDB && (grp != null)) {
				Console.getInstance().println(
					"Group "
						+ dbGrp.getName()
						+ " already exists - this definition will be ignored.");
				continue;
			}
			if (grp == null)
				grp = rootGroup.createGroup(dbGrp.getName());
			grp.setColor(dbGrp.getColor());
			grp.setDescription(dbGrp.getDescription());
			grp.setX(dbGrp.getX()); grp.setY(dbGrp.getY());
		}


		// update template instances links
		Iterator i = tobeUpdated.iterator();
		while (i.hasNext())
			((Template)i.next()).manageLinks();

		// add links, connectors
		
		// !!!! fix templates links, connectors

		int pos;
		String recordName;
		String fieldName;
		String target;

		OutLink outlink;
		InLink inlink;
		Connector connector;
		DBConnectorData connectorData;
		DBLinkData dbLink;
		e = dbData.getLinks().elements();
		while (e.hasMoreElements())
		{
			dbLink = (DBLinkData) (e.nextElement());

			pos = dbLink.getFieldName().lastIndexOf(Constants.FIELD_SEPARATOR);
			recordName = dbLink.getFieldName().substring(0, pos);
			fieldName = dbLink.getFieldName().substring(pos + 1);

			record = (Record) rootGroup.findObject(recordName, true);
			if (record != null)
			{
				if (importDB)
				{
					if (blackList.contains(record))
					{
						Console.getInstance().println(
							"Link "
								+ dbLink.getFieldName()
								+ " already exists - this definition will be ignored.");
						continue;
					}
				}

				// field has to be already created
				Object unknownTarget = record.getSubObjects().get(fieldName);
				if (unknownTarget instanceof OutLink)
				{
					outlink = (OutLink) unknownTarget;
					
					target = dbLink.getTargetID();
					pos = target.lastIndexOf(EPICSLinkOut.LINK_SEPARATOR);
					if (pos >= 0) // connectors
					{
						while ((connectorData = (DBConnectorData) dbData.getConnectors().get(target))!= null)
						{
							connector = new Connector(target, record, null, null);
							connector.setColor(connectorData.getColor());
							connector.setDescription(connectorData.getDescription());
							connector.setX(connectorData.getX());
							connector.setY(connectorData.getY());
							connector.setMode(connectorData.getMode());
							record.addSubObject(connector.getID(), connector);
							connector.setOutput(outlink, null);
							outlink.setInput(connector);

							outlink = connector;
							target = connectorData.getTargetID();
						}
					}

					// is in lookup table ?
					InLink templateLink = (InLink)Group.getRoot().getLookupTable().get(target);
					if (templateLink!=null)
					{
						templateLink.setOutput(outlink, null);
						outlink.setInput(templateLink);
					}
					else if ((pos = target.lastIndexOf(Constants.FIELD_SEPARATOR)) >= 0)
					{
						recordName = target.substring(0, pos);
						fieldName = target.substring(pos + 1);

						// remove process
						pos = fieldName.indexOf(' ');
						if (pos>0)
							fieldName = fieldName.substring(0, pos);

						record = (Record) rootGroup.findObject(recordName, true);
						if (record != null)
						{
							// field has to be already created
							Object unknown = record.getSubObjects().get(fieldName);
							if (unknown instanceof InLink)
							{
								inlink = (InLink)unknown;
								inlink.setOutput(outlink, null);
								outlink.setInput(inlink);
							}
						}
					}
					else
					{
						Record targetRecord = null;
						if ((targetRecord = (Record) rootGroup.findObject(target, true)) != null)
						{
							// VAL or forward link
							VDBFieldData fieldData = (VDBFieldData) record.getRecordData().getField(fieldName);		// existance already checked
							if (fieldData.getType() == DBDConstants.DBF_FWDLINK)
							{
								// forward link
								targetRecord.setOutput(outlink, null);
								outlink.setInput(targetRecord);
							} 
							else
							{
								// VAL
								// field has to be already created
								inlink = (InLink) record.getSubObjects().get("VAL");
								if (inlink != null)
								{
									inlink.setOutput(outlink, null);
									outlink.setInput(inlink);
								}

							}
						}
					}
				}
			}
		}

		// lines 
		DBLine dbLine;
		e = dbData.getLines().elements();
		while (e.hasMoreElements())
		{
			dbLine = (DBLine)e.nextElement();
			Line line = new Line(dbLine.getName(), null, dbLine.getX(), dbLine.getY(), dbLine.getX2(), dbLine.getY2());
			line.setDashed(dbLine.isDashed());
			line.setStartArrow(dbLine.isStartArrow());
			line.setEndArrow(dbLine.isEndArrow());
			line.setColor(dbLine.getColor());
			rootGroup.addSubObject(line.getName(), line, true);
		}

		// boxes 
		DBBox dbBox;
		e = dbData.getBoxes().elements();
		while (e.hasMoreElements())
		{
			dbBox = (DBBox)e.nextElement();
			Box box = new Box(dbBox.getName(), null, dbBox.getX(), dbBox.getY(), dbBox.getX2(), dbBox.getY2());
			box.setIsDashed(dbBox.isDashed());
			box.setColor(dbBox.getColor());
			rootGroup.addSubObject(box.getName(), box, true);
		}

		// textboxes 
		DBTextBox dbTextBox;
		e = dbData.getTextboxes().elements();
		while (e.hasMoreElements())
		{
			dbTextBox = (DBTextBox)e.nextElement();
			TextBox textbox = new TextBox(dbTextBox.getName(), null, dbTextBox.getX(), dbTextBox.getY(), dbTextBox.getX2(), dbTextBox.getY2());
			textbox.setBorder(dbTextBox.getBorder());
			
			Font font = FontMetricsBuffer.getInstance().getFont(dbTextBox.getFontName(), dbTextBox.getFontSize(), dbTextBox.getFontStyle());
			textbox.setFont(font);
			
			textbox.setDescription(dbTextBox.getDescription());
			textbox.setColor(dbTextBox.getColor());
			rootGroup.addSubObject(textbox.getName(), textbox, true);
		}

	} catch (Exception e) {
		Console.getInstance().println("Error occured while applying visual data!");
		e.printStackTrace();
	}

	group.initializeLayout();
	// call this after ports/macros fields are initialized !!
	//group.manageLinks(true);
}

/**
 * Insert the method's description here.
 * Creation date: (6.1.2001 22:35:40)
 * @param file java.io.File
 */
public boolean openDBD(File file) throws IOException {
	return openDBD(file, false);
}
/**
 * Insert the method's description here.
 * Creation date: (6.1.2001 22:35:40)
 * @param file java.io.File
 */
public boolean openDBD(File file, boolean importDBD) throws IOException {

	if (DataProvider.getInstance().getLoadedDBDs().contains(file))
	{
		Console.getInstance().println();
		Console.getInstance().println("o) DBD file '"+file.getAbsolutePath()+"' is already loaded.");
		return true;
	}

	setCursor(hourCursor);

	com.cosylab.vdct.dbd.DBDData dbdData = null;
	if (importDBD)
		dbdData = DataProvider.getInstance().getDbdDB();
		
/*	if (getAppletBase()!=null) 	//applet support !!!
		try {
			dbdData = DBDResolver.resolveDBDasURL(dbdData, new java.net.URL(getAppletBase(), file.getAbsolutePath()));
		} catch (java.net.MalformedURLException e) {
			Console.getInstance().println(e); 
		}
	else */
	 	dbdData = DBDResolver.resolveDBD(dbdData, file.getAbsolutePath());

	if (dbdData==null) {
		restoreCursor();
		return false;
	}
	
	if (!importDBD)
	{
		DataProvider.getInstance().setDbdDB(dbdData);
		if (viewGroup==null) initializeWorkspace();
		createNavigatorImage();
	}

	// add to list of DBDs
	DataProvider.getInstance().getLoadedDBDs().addElement(file.getAbsoluteFile());
	DataProvider.getInstance().getDBDs().addElement(file.getAbsoluteFile());

	// !!!
	VisualDCT.getInstance().updateLoadLabel();	

	restoreCursor();
	return true;
}

/**
 * Prints the page at the specified index into the specified 
 * {@link Graphics} context in the specified
 * format.  A <code>PrinterJob</code> calls the 
 * <code>Printable</code> interface to request that a page be
 * rendered into the context specified by 
 * <code>graphics</code>.  The format of the page to be drawn is
 * specified by <code>pageFormat</code>.  The zero based index
 * of the requested page is specified by <code>pageIndex</code>. 
 * If the requested page does not exist then this method returns
 * NO_SUCH_PAGE; otherwise PAGE_EXISTS is returned.
 * The <code>Graphics</code> class or subclass implements the
 * {@link PrinterGraphics} interface to provide additional
 * information.  If the <code>Printable</code> object
 * aborts the print job then it throws a {@link PrinterException}.
 * @param graphics the context into which the page is drawn 
 * @param pageFormat the size and orientation of the page being drawn
 * @param pageIndex the zero based index of the page to be drawn
 * @return PAGE_EXISTS if the page is rendered successfully
 *         or NO_SUCH_PAGE if <code>pageIndex</code> specifies a
 *	       non-existent page.
 * @exception java.awt.print.PrinterException
 *         thrown when the print job is terminated.
 */
public int print(java.awt.Graphics graphics, java.awt.print.PageFormat pageFormat, int pageIndex) throws java.awt.print.PrinterException {
	
	graphics.translate((int)pageFormat.getImageableX(),
					   (int)pageFormat.getImageableY());

	int pageWidth = (int)pageFormat.getImageableWidth();
	int pageHeight = (int)pageFormat.getImageableHeight();

	//graphics.clipRect(0, 0, pageWidth, pageHeight);

	//System.out.println(pageFormat.getOrientation()==PageFormat.LANDSCAPE);
	//if (true) throw new java.awt.print.PrinterException();
	
	/*------------------------------------------------------------*/
	
	ViewState view = ViewState.getInstance();

	
	double screen2printer = 0;
	switch (Page.getPrintMode()) {
		case Page.TRUE_SCALE:
			// 1:1 ratio
			screen2printer = 72.0/getWorkspacePanel().getToolkit().getScreenResolution();
			break;
			
		case Page.USER_SCALE:
			screen2printer = 72.0/getWorkspacePanel().getToolkit().getScreenResolution();
			screen2printer *= Page.getUserScale();
			break;
			
		case Page.FIT_SCALE:
			// fit to paper
			double xscale = pageWidth/(double)view.getViewWidth();
			double yscale = pageHeight/(double)view.getViewHeight();
			screen2printer = Math.min(xscale, yscale)*view.getScale();
			break;
	}
	
	double converter = screen2printer/view.getScale();
	int w = (int)(view.getViewWidth()*converter);
	int h = (int)(view.getViewHeight()*converter);

	if (w==0 || h==0)
		return NO_SUCH_PAGE;

	if (Page.getPrintMode()==Page.FIT_SCALE)
	{
		// center (surely one page)
		graphics.translate((pageWidth-w)/2,
						   (pageHeight-h)/2);
	}

		

	int nCol = Math.max((int)Math.ceil((double)w/pageWidth), 1);
	int nRow = Math.max((int)Math.ceil((double)h/pageHeight), 1);
	int maxNumPage = nCol * nRow;

	if (pageIndex>=maxNumPage)
		return NO_SUCH_PAGE;

	int iCol = pageIndex % nCol;
	int iRow = pageIndex / nCol;
	int x = iCol * pageWidth;
	int y = iRow * pageHeight;

	int imageWidth = Math.min(pageWidth, w-x);
	int imageHeight = Math.min(pageHeight, h-y);
	
	graphics.clipRect(0, 0, imageWidth, imageHeight);
	
	int rx = view.getRx(); int ry = view.getRy();
	double scale = view.getScale();

	try
	{
		
		view.setScale(screen2printer);
	
		view.setRx((int)(rx*converter+x));
		view.setRy((int)(ry*converter+y));
	
		if (Settings.getInstance().getShowGrid())
		{

			graphics.setColor(Constants.GRID_COLOR);
			int gridSize = view.getGridSize();
			int sx = view.getGridSize() - view.getRx() % gridSize;
			int y0 = view.getGridSize() - view.getRy() % gridSize;
			int xsteps = view.getViewWidth() / gridSize + 1;
			int ysteps = view.getViewHeight() / gridSize + 1;
	
			if (gridSize >= 15)
			// crosses
			for (int gy=0; gy < ysteps; gy++) {
				int x0 = sx;
				for (int gx=0; gx < xsteps; gx++) {
					graphics.drawLine(x0-1, y0-1, x0+1, y0+1);
					graphics.drawLine(x0-1, y0+1, x0+1, y0-1);
					x0+=gridSize;
				}
				y0+=gridSize;
			}
			else
			// dots
			for (int gy=0; gy < ysteps; gy++) {
				int x0 = sx;
				for (int gx=0; gx < xsteps; gx++) {
					graphics.drawLine(x0, y0, x0, y0);
					x0+=gridSize;
				}
				y0+=gridSize;
			}
		}


		// change color sheme 
		loadBlackOnWhiteColorScheme();

		viewGroup.paintComponents(graphics, false, isFlat());

		// restore color sheme 
		loadWhiteOnBlackColorScheme();
		
	}
	catch (Exception e)
	{
	}
	finally
	{
		view.setScale(scale);
		view.setRx(rx); view.setRy(ry);
	}
	


	/*------------------------------------------------------------*/
	
	System.gc();	
	return PAGE_EXISTS;
}
/**
 * Insert the method's description here.
 * Creation date: (27.12.2000 15:04:17)
 */
public void recalculateNavigatorPosition() {
	ViewState view = ViewState.getInstance();

	double ratio = navigatorView.getScale()/view.getScale();
	/// bug here
	navigatorRect.x = (int)(view.getRx()*ratio) + navigator.x;
	navigatorRect.y = (int)(view.getRy()*ratio) + navigator.y;
	navigatorRect.width = (int)(view.getViewWidth()*ratio);
	navigatorRect.height = (int)(view.getViewHeight()*ratio);
}
/**
 * Insert the method's description here.
 * Creation date: (25.12.2000 17:06:55)
 * @param g java.awt.Graphics
 */
private void redraw(Graphics g) {

	if (Settings.getInstance().getNavigator())
		createNavigatorImage();

	ViewState view = ViewState.getInstance();
		
	if ((canvasImage==null) || 
		(canvasSize.width!=width) || 
		(canvasSize.height!=height)) {
	
	    canvasImage = getWorkspacePanel().createImage(width, height);
	    if (canvasImage==null) return;
	    canvasSize = new Dimension(width, height);
	    canvasGraphics = canvasImage.getGraphics();

		// free old image memory	    
	    System.gc();
	}

	int origX = 0;
	int origY = 0;
	int w = width;
	int h = height;

	canvasGraphics.setColor(Color.gray);
	if (view.getRx()<0)
	{
		origX = -view.getRx();
		canvasGraphics.fillRect(0, 0, origX, height);
	}
	
	if (view.getRy()<0)
	{
		origY = -view.getRy();
		canvasGraphics.fillRect(origX, 0, width, origY);
	}
		
	if ((view.getRx()+view.getViewWidth())>(int)(view.getScale()*view.getWidth()))
	{
		w += (int)(view.getScale()*view.getWidth()) - view.getRx() - view.getViewWidth();
		canvasGraphics.fillRect(w, origY, width, height);
	}
	
	if ((view.getRy()+view.getViewHeight())>(int)(view.getScale()*view.getHeight()))
	{
		h += (int)(view.getScale()*view.getHeight()) - view.getRy() - view.getViewHeight();
		canvasGraphics.fillRect(origX, h, width, height);
	}

	canvasGraphics.setColor(Constants.BACKGROUND_COLOR);
	canvasGraphics.fillRect(origX, origY, w, h);

	if (Settings.getInstance().getShowGrid())
	{

		canvasGraphics.setColor(Constants.GRID_COLOR);
		int gridSize = view.getGridSize();
		int sx = origX;
		if (view.getRx()>0)
			sx += (view.getGridSize() - view.getRx()) % gridSize;
			
		int y0 = origY;
		if (view.getRy()>0)
			y0 += (view.getGridSize() - view.getRy()) % gridSize;
			
//		int xsteps = view.getViewWidth() / gridSize + 1;
//		int ysteps = view.getViewHeight() / gridSize + 1;
		int xsteps = (w-origX+1) / gridSize + 1;
		int ysteps = (h-origY+1) / gridSize + 1;

		if (gridSize >= 15)
		// crosses
		for (int y=0; y < ysteps; y++) {
			int x0 = sx;
			for (int x=0; x < xsteps; x++) {
				canvasGraphics.drawLine(x0-1, y0-1, x0+1, y0+1);
				canvasGraphics.drawLine(x0-1, y0+1, x0+1, y0-1);
				x0+=gridSize;
			}
			y0+=gridSize;
		}
		else
		// dots
		for (int y=0; y < ysteps; y++) {
			int x0 = sx;
			for (int x=0; x < xsteps; x++) {
				canvasGraphics.drawLine(x0, y0, x0, y0);
				x0+=gridSize;
			}
			y0+=gridSize;
		}

	}

	viewGroup.paintComponents(canvasGraphics, false, isFlat());

	if (canvasGraphics!=null) copyCanvasImage(g);
	
}
/**
 * Insert the method's description here.
 * Creation date: (25.12.2000 14:59:05)
 */
public void repaint() {
	// set clip to x0, y0, width, height !!!?
	forceRedraw = true;	
	getWorkspacePanel().repaint();
}
/**
 * Insert the method's description here.
 * Creation date: (10.12.2000 13:19:16)
 */
public void resize(int x0, int y0, int width, int height) {
	if (getComponent()!=null) resize(x0, y0, width, height);
	else {
		this.x0=x0;
		this.y0=y0;
		this.width=width;
		this.height=height;
	}

	ViewState view = ViewState.getInstance();
	
	view.setX0(x0);
	view.setY0(y0);
	view.setViewWidth(width);
	view.setViewHeight(height);

	// set navigator
	navigator.height = height / 6;
	navigator.width = (int)(navigator.height * (view.getWidth()/(double)view.getHeight()));
	navigator.x = width - navigator.width + x0;
	navigator.y = y0;

	recalculateNavigatorPosition();
	forceRedraw = true;	
}
/**
 * Insert the method's description here.
 * Creation date: (25.12.2000 15:57:43)
 */
private void restoreCursor() {
	SetCursorCommand cm = (SetCursorCommand)CommandManager.getInstance().getCommand("SetCursor");
	if (cm!=null) {
		currentCursor = previousCursor;
		previousCursor = defaultCursor;
		cm.setCursor(currentCursor);
		cm.execute();
	}
}
/**
 * Insert the method's description here.
 * Creation date: (25.12.2000 16:49:07)
 * @param x1 int
 * @param y1 int
 * @param x2 int
 * @param y2 int
 */
private void selectArea(int x1, int y1, int x2, int y2) {
	ViewState view = ViewState.getInstance();
	if (viewGroup.selectComponentsCheck(x1+view.getRx(), y1+view.getRy(), 
										x2+view.getRx(), y2+view.getRy()))
	{
		blockNavigatorRedrawOnce = true;
		repaint();
	}
}
/**
 * Insert the method's description here.
 * Creation date: (25.12.2000 15:57:43)
 * @param cursor java.awt.Cursor
 */
private void setCursor(Cursor cursor) {
	SetCursorCommand cm = (SetCursorCommand)CommandManager.getInstance().getCommand("SetCursor");
	if (cm!=null) {
		previousCursor = currentCursor;
		currentCursor = cursor;
		cm.setCursor(currentCursor);
		cm.execute();
	}
}
/**
 * Insert the method's description here.
 * Creation date: (1.2.2001 14:24:51)
 * @param newModified boolean
 */
public void setModified(boolean newModified) {
	modified = newModified;
}
/**
 * Insert the method's description here.
 * Creation date: (29.12.2000 12:40:13)
 * @param scale double
 */
public void setScale(double scale) {

	ViewState view = ViewState.getInstance();
	double oldscale = view.getScale();

	double ds = scale/oldscale;

	// find center
	double drx = view.getDrx() + view.getViewWidth()*oldscale/2.0;
	double dry = view.getDry() + view.getViewHeight()*oldscale/2.0;
	
	// transform into new scale
	drx *= ds;
	dry *= ds;
	
	// find new origin
	drx += view.getViewWidth()*(ds-1-scale)/2.0;
	dry += view.getViewHeight()*(ds-1-scale)/2.0;

	view.setDrx(drx);
	view.setDry(dry);

	view.setScale(scale);

	blockNavigatorRedrawOnce = true;
	recalculateNavigatorPosition();
	repaint();
}

/**
 * Insert the method's description here.
 * Creation date: (29.12.2000 12:40:13)
 * @param scale double
 */
public void centerObject(VisibleObject object) {

	// check if Groups are the same
	// not so clean !!!
	if (object.getParent() instanceof Group)
	{
		if (object.getParent()!=viewGroup)
		{
			moveToGroup((Group)object.getParent());
		}
	}
	else if (object.getParent().getParent() instanceof Group)
	{
		if (object.getParent().getParent()!=viewGroup)
		{
			moveToGroup((Group)object.getParent().getParent());
		}
	}

	ViewState view = ViewState.getInstance();

	// find center
	int drx = object.getRx()+object.getRwidth()/2;
	int dry = object.getRy()+object.getRheight()/2;
	
	// find new origin
	drx -= view.getViewWidth()/2;
	dry -= view.getViewHeight()/2;

	view.setRx(drx);
	view.setRy(dry);

	recalculateNavigatorPosition();
	repaint();


}

/**
 * Insert the method's description here.
 * Creation date: (3.2.2001 16:30:14)
 */
private void stopLinking() {
	if (tmplink!=null) {
		tmplink = null;
		if (hiliter!=null) hiliter.terminate();
		ViewState.getInstance().deblinkAll(); //!!!
		repaint();
	}
}
/**
 * Insert the method's description here.
 * Creation date: (29.12.2000 12:49:54)
 */
private void updateWorkspaceScale() {
	SetWorkspaceScale cmd = (SetWorkspaceScale)CommandManager.getInstance().getCommand("SetWorkspaceScale");
	cmd.setScale(ViewState.getInstance().getScale());
	cmd.execute();
}
/**
 * Insert the method's description here.
 * Creation date: (25.12.2000 16:49:07)
 * @param x1 int
 * @param y1 int
 * @param x2 int
 * @param y2 int
 */
public void zoomArea(int x1, int y1, int x2, int y2) {
	ViewState view = ViewState.getInstance();

	double scale = view.getScale();
	int w = Math.abs(x2-x1);
	int h = Math.abs(y2-y1);
	
	double xscale = view.getViewWidth()/(double)w;
	double yscale = view.getViewHeight()/(double)h;
	double dfscale = Math.min(xscale, yscale);

	double nscale = scale*dfscale;
	if (nscale > 2.5)	// !!! maximum
	{
		nscale=2.5; dfscale=2.5/scale;
	}

	double dx = w*(xscale-dfscale)/2.0;
	double dy = h*(yscale-dfscale)/2.0;

	x1=Math.min(x1, x2);
	y1=Math.min(y1, y2);
	
	dx = Math.max(0, (x1+view.getRx())*dfscale-dx);
	dy = Math.max(0, (y1+view.getRy())*dfscale-dy);
	
	view.setDrx(dx);
	view.setDry(dy);
	view.setScale(nscale);

	updateWorkspaceScale();
	
	blockNavigatorRedrawOnce = true;
	recalculateNavigatorPosition();
	repaint();
}

/**
 * Insert the method's description here.
 * Creation date: (3.2.2001 23:27:30)
 * @param name java.lang.String
 * @param type java.lang.String
 * @param relative boolean
 */
public void createTemplateInstance(String name, String type, boolean relative) {
	if (relative)
	{
		String parentName = getViewGroup().getAbsoluteName();
		if (parentName.length()>0)
			name = parentName + Constants.GROUP_SEPARATOR + name;
	}
	
	VDBTemplate template = (VDBTemplate)VDBData.getTemplates().get(type);
	if (template==null)
	{
		Console.getInstance().println(
			"Template instance "+name+" cannot be created since "
				+ type
				+ " does not exist.");
		return;
	}			

	// generate name
	if (name==null)
	{
		name = "template000";	
		while (Group.getRoot().findObject(name, true)!=null)
			name = StringUtils.incrementName(name, null);
	}

	VDBTemplateInstance templateInstance = VDBData.generateNewVDBTemplateInstance(name, template);

	ViewState view = ViewState.getInstance();
	double scale = view.getScale();
	
	Template templ = new Template(null, templateInstance);
	Group.getRoot().addSubObject(name, templ, true);

	//templ.setDescription(dbTemplate.getDescription());
	//templ.setDescription(template.getDescription());
	templ.setX((int)((getPressedX() + view.getRx()) / scale));
	templ.setY((int)((getPressedY() + view.getRy()) / scale));

	UndoManager.getInstance().addAction(new CreateAction(templ));

	//drawingSurface.setModified(true);
	repaint();
}

/**
 * true - OK, false - denied
 */
public boolean prepareTemplateLeave()
{
	/// !!! getInstance()
	if (isModified())
	{
		if (templateStack.isEmpty())
		{
			JOptionPane.showMessageDialog(VisualDCT.getInstance(), "The file has to be saved first...");
			return false;
		}
		else if (JOptionPane.showConfirmDialog(VisualDCT.getInstance(), "The file has been modified. Discard changes?", "Confirmation", 
						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)==JOptionPane.OK_OPTION)
		{
			// pop old current
			VDBTemplate backup = (VDBTemplate)templateStack.pop();
			// reload template
			boolean ok = reloadTemplate(Group.getEditingTemplateData());
			// push new
			VDBTemplate reloaded = (VDBTemplate)VDBData.getTemplates().get(backup.getId());
			
			if (!ok || reloaded==null)
			{
				VDBData.addTemplate(backup);
				templateStack.push(backup);
			}
			else
			{
				templateStack.push(reloaded);
				viewGroup = reloaded.getGroup();
			}
					
			return true;
		}
		else
			return false;
	}
	else		
		return true;
}

/**
 */
public void templateReloadPostInit()
{

	// initialize
	setModified(false);
	UndoManager.getInstance().reset();
	UndoManager.getInstance().setMonitor(true);

	// update opened file
	if (Group.getEditingTemplateData()==null)
		VisualDCT.getInstance().setOpenedFile(null);
	else
		VisualDCT.getInstance().setOpenedFile(new File(Group.getEditingTemplateData().getFileName()));

	InspectorManager.getInstance().updateObjectLists();

}

/**
 * Insert the method's description here.
 * Creation date: (22.4.2001 18:44:03)
 * @param template com.cosylab.vdct.graphics.objects.Template
 */
public void descendIntoTemplate(Template template)
{

	if (!prepareTemplateLeave())
		return;

	viewStack.push(viewGroup);
	templateStack.push(template.getTemplateData().getTemplate());
	
	Group group = template.getTemplateData().getTemplate().getGroup();
	Group.setRoot(group);

	Group.setEditingTemplateData(template.getTemplateData().getTemplate());
	
	// initialize
	templateReloadPostInit();

	moveToGroup(group);
}

/**
 * Insert the method's description here.
 * Creation date: (22.4.2001 18:44:03)
 * @param template com.cosylab.vdct.graphics.objects.Template
 */
public void ascendFromTemplate()
{
	if (!prepareTemplateLeave())
		return;

	viewGroup = (Group)viewStack.pop();
	templateStack.pop();
	
	if (!templateStack.isEmpty())
		Group.setEditingTemplateData((VDBTemplate)templateStack.peek());
	else
		Group.setEditingTemplateData(null);
		
	Group grp = viewGroup;
	while (grp.getParent()!=null)
		grp = (Group)grp.getParent();
	Group.setRoot(grp);

	// initialize
	templateReloadPostInit();
	
	moveToGroup(grp);	
}

/**
 * Insert the method's description here.
 * Creation date: (22.4.2001 18:44:03)
 * @param group com.cosylab.vdct.graphics.objects.Group
 */
public void moveToGroup(Group group)
{
	viewGroup = group;
	ViewState.getInstance().set(viewGroup);
	//createNavigatorImage();
	viewGroup.unconditionalValidateSubObjects(isFlat());

	updateWorkspaceGroup();

	updateWorkspaceScale();
	
	blockNavigatorRedrawOnce = false;
	forceRedraw = true;
	repaint();
}

/**
 * Insert the method's description here.
 * Creation date: (22.4.2001 18:44:03)
 */
public void updateWorkspaceGroup()
{
	SetWorkspaceGroup cmd = (SetWorkspaceGroup)CommandManager.getInstance().getCommand("SetGroup");
	
	String name = viewGroup.getAbsoluteName();
	if (name.length()==0)
		name = Constants.MAIN_GROUP;
	
	if (templateStack.size()>1) 
		name = Constants.TEMPLATE_GROUP + " [" + ((VDBTemplate)templateStack.peek()).getDescription() + "]: "+ name;
	cmd.setGroup(name);
	cmd.execute();
}

/**
 * Returns the templateStack.
 * @return Stack
 */
public Stack getTemplateStack()
{
	return templateStack;
}

/**
 */
public boolean reloadTemplate(VDBTemplate data)
{
	if (data==null)
		return true;
	
	// remove from template repository
	VDBData.removeTemplate(data);

	InspectorManager.getInstance().updateObjectLists();

	Console.getInstance().println("Reloading template '"+data.getFileName()+"'.");	
	
	// reload
	try
	{
		// import
		boolean ok = open(new File(data.getFileName()), true);

		if (!ok || !VDBData.getTemplates().containsKey(data.getId()))
		{
			Console.getInstance().println("Failed to reload template '"+data.getFileName()+"'. Using in-memory definitions...");	
		}

		return true;
		
	}
	catch (Exception e)
	{
		Console.getInstance().println("Failed to reload template '"+data.getFileName()+"'. Using in-memory definitions...");	
		Console.getInstance().println(e);
	}
	
	return false;
}

/**
 * Sets the blockNavigatorRedrawOnce.
 * @param blockNavigatorRedrawOnce The blockNavigatorRedrawOnce to set
 */
public void setBlockNavigatorRedrawOnce(boolean blockNavigatorRedrawOnce)
{
	this.blockNavigatorRedrawOnce = blockNavigatorRedrawOnce;
}

/**
 * Loads white on black color cheme
 */
public static void loadWhiteOnBlackColorScheme()
{
	// black on white color scheme
    Constants.BACKGROUND_COLOR = Color.black;
    Constants.PICK_COLOR = Color.red;
    Constants.FRAME_COLOR = Color.white;
    Constants.HILITE_COLOR = Color.yellow;
    Constants.LINE_COLOR = Color.white;
    Constants.RECORD_COLOR = Color.black;
    Constants.SELECTION_COLOR = Color.red;
    Constants.LINK_COLOR = Color.white;

    Constants.GRID_COLOR = Color.lightGray;}

/**
 * Loads black on white color cheme
 */
public static void loadBlackOnWhiteColorScheme()
{
	// white on black color scheme
    Constants.BACKGROUND_COLOR = Color.white;
    Constants.PICK_COLOR = Color.pink;
    Constants.FRAME_COLOR = Color.black;
    Constants.HILITE_COLOR = Color.red;
    Constants.LINE_COLOR = Color.black;
    Constants.RECORD_COLOR = Color.white;
    Constants.SELECTION_COLOR = Color.pink;
    Constants.LINK_COLOR = Color.white;

    Constants.GRID_COLOR = Color.lightGray;
}

/**
 * Insert the method's description here.
 * Creation date: (3.2.2001 23:27:30)
 * @param name java.lang.String
 */
public void createPort(VDBPort vdbPort) {
	
	// if null bring up dialog and ask for name, then create port
	if (vdbPort==null)
		vdbPort = Group.getEditingTemplateData().addPort();
		
	if (vdbPort==null)
		return;
		
	ViewState view = ViewState.getInstance();
	double scale = view.getScale();
	
	Port port = new Port(vdbPort, viewGroup,
						   (int)((getPressedX() + view.getRx()) / scale),
						   (int)((getPressedY() + view.getRy()) / scale));

	getViewGroup().addSubObject(vdbPort.getName(), port);

	UndoManager.getInstance().addAction(new CreateAction(port));

	//drawingSurface.setModified(true);
	repaint();
}
}
