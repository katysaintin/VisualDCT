package com.cosylab.vdct.dbd;

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
import java.util.*;
import com.cosylab.vdct.Console;

/**
 * This type was created in VisualAge.
 */
public class DBDResolver {
	private static String MENU   	 = "menu";
	private static String FIELD  	 = "field";
	private static String PATH  	 = "path";
	private static String ADDPATH  	 = "addpath";
	private static String DEVICE     = "device";
	private static String INCLUDE    = "include";
	private static String RECORDTYPE = "recordtype";

	private static String CHOICE = "choice";

	private static String ENDSTR		 = "}";

	private static String BASE		 = "base";
	private static String SIZE		 = "size";
	private static String INITIAL	 = "initial";
	private static String PROMPT	 = "prompt";
	private static String PROMPTGROUP  = "promptgroup";

/**
 * This method was created in VisualAge.
 * @return int
 * @param bt java.lang.String
 */
public static int getBaseType(String bt) {
	if (bt.equalsIgnoreCase("DECIMAL"))	return (DBDConstants.DECIMAL);
	else if (bt.equalsIgnoreCase("HEX")) return (DBDConstants.HEX);
	else return DBDConstants.NOT_DEFINED;
}
/**
 * This method was created in VisualAge.
 * @return int
 * @param stype java.lang.String
 */
public static int getFieldType(String stype) {
	
	if (stype.equalsIgnoreCase("DBF_STRING"))	     return (DBDConstants.DBF_STRING);
	else if (stype.equalsIgnoreCase("DBF_CHAR"))     return (DBDConstants.DBF_CHAR);
	else if (stype.equalsIgnoreCase("DBF_UCHAR"))    return (DBDConstants.DBF_UCHAR);
	else if (stype.equalsIgnoreCase("DBF_SHORT"))    return (DBDConstants.DBF_SHORT);
	else if (stype.equalsIgnoreCase("DBF_USHORT"))   return (DBDConstants.DBF_USHORT);
	else if (stype.equalsIgnoreCase("DBF_LONG"))     return (DBDConstants.DBF_LONG);
	else if (stype.equalsIgnoreCase("DBF_ULONG"))    return (DBDConstants.DBF_ULONG);
	else if (stype.equalsIgnoreCase("DBF_FLOAT"))    return (DBDConstants.DBF_FLOAT);
	else if (stype.equalsIgnoreCase("DBF_DOUBLE"))   return (DBDConstants.DBF_DOUBLE);
	else if (stype.equalsIgnoreCase("DBF_ENUM"))     return (DBDConstants.DBF_ENUM);
	else if (stype.equalsIgnoreCase("DBF_MENU"))     return (DBDConstants.DBF_MENU);
	else if (stype.equalsIgnoreCase("DBF_DEVICE"))   return (DBDConstants.DBF_DEVICE);
	else if (stype.equalsIgnoreCase("DBF_INLINK"))   return (DBDConstants.DBF_INLINK);
	else if (stype.equalsIgnoreCase("DBF_OUTLINK"))  return (DBDConstants.DBF_OUTLINK);
	else if (stype.equalsIgnoreCase("DBF_FWDLINK"))  return (DBDConstants.DBF_FWDLINK);
	else if (stype.equalsIgnoreCase("DBF_NOACCESS")) return (DBDConstants.DBF_NOACCESS);
	else return DBDConstants.NOT_DEFINED;
}
/**
 * This method was created in VisualAge.
 * @return java.lang.String
 * @param GUItype int
 */
public static String getGUIString(int GUItype) {
	if (GUItype==DBDConstants.GUI_COMMON) return "GUI_COMMON";
	else if (GUItype==DBDConstants.GUI_ALARMS) return "GUI_ALARMS";
	else if (GUItype==DBDConstants.GUI_BITS1) return "GUI_BITS1";
	else if (GUItype==DBDConstants.GUI_BITS2) return "GUI_BITS2";
	else if (GUItype==DBDConstants.GUI_CALC) return "GUI_CALC";
	else if (GUItype==DBDConstants.GUI_CLOCK) return "GUI_CLOCK";
	else if (GUItype==DBDConstants.GUI_COMPRESS) return "GUI_COMPRESS";
	else if (GUItype==DBDConstants.GUI_CONVERT) return "GUI_CONVERT";
	else if (GUItype==DBDConstants.GUI_DISPLAY) return "GUI_DISPLAY";
	else if (GUItype==DBDConstants.GUI_HIST) return "GUI_HIST";
	else if (GUItype==DBDConstants.GUI_INPUTS) return "GUI_INPUTS";
	else if (GUItype==DBDConstants.GUI_LINKS) return "GUI_LINKS";
	else if (GUItype==DBDConstants.GUI_MBB) return "GUI_MBB";
	else if (GUItype==DBDConstants.GUI_MOTOR) return "GUI_MOTOR";
	else if (GUItype==DBDConstants.GUI_OUTPUT) return "GUI_OUTPUT";
	else if (GUItype==DBDConstants.GUI_PID) return "GUI_PID";
	else if (GUItype==DBDConstants.GUI_PULSE) return "GUI_PULSE";
	else if (GUItype==DBDConstants.GUI_SELECT) return "GUI_SELECT";
	else if (GUItype==DBDConstants.GUI_SEQ1) return "GUI_SEQ1";
	else if (GUItype==DBDConstants.GUI_SEQ2) return "GUI_SEQ2";
	else if (GUItype==DBDConstants.GUI_SEQ3) return "GUI_SEQ3";
	else if (GUItype==DBDConstants.GUI_SUB) return "GUI_SUB";
	else if (GUItype==DBDConstants.GUI_TIMER) return "GUI_TIMER";
	else if (GUItype==DBDConstants.GUI_WAVE) return "GUI_WAVE";
	else if (GUItype==DBDConstants.GUI_SCAN) return "GUI_SCAN";
	else return "Miscellaneous";
}
/**
 * This method was created in VisualAge.
 * @return int
 * @param gui java.lang.String
 */
public static int getGUIType(String gui) {
	if (gui.equalsIgnoreCase("GUI_COMMON")) return DBDConstants.GUI_COMMON;
	else if (gui.equalsIgnoreCase("GUI_ALARMS")) return DBDConstants.GUI_ALARMS;
	else if (gui.equalsIgnoreCase("GUI_BITS1")) return DBDConstants.GUI_BITS1;
	else if (gui.equalsIgnoreCase("GUI_BITS2")) return DBDConstants.GUI_BITS2;
	else if (gui.equalsIgnoreCase("GUI_CALC")) return DBDConstants.GUI_CALC;
	else if (gui.equalsIgnoreCase("GUI_CLOCK")) return DBDConstants.GUI_CLOCK;
	else if (gui.equalsIgnoreCase("GUI_COMPRESS")) return DBDConstants.GUI_COMPRESS;
	else if (gui.equalsIgnoreCase("GUI_CONVERT")) return DBDConstants.GUI_CONVERT;
	else if (gui.equalsIgnoreCase("GUI_DISPLAY")) return DBDConstants.GUI_DISPLAY;
	else if (gui.equalsIgnoreCase("GUI_HIST")) return DBDConstants.GUI_HIST;
	else if (gui.equalsIgnoreCase("GUI_INPUTS")) return DBDConstants.GUI_INPUTS;
	else if (gui.equalsIgnoreCase("GUI_LINKS")) return DBDConstants.GUI_LINKS;
	else if (gui.equalsIgnoreCase("GUI_MBB")) return DBDConstants.GUI_MBB;
	else if (gui.equalsIgnoreCase("GUI_MOTOR")) return DBDConstants.GUI_MOTOR;
	else if (gui.equalsIgnoreCase("GUI_OUTPUT")) return DBDConstants.GUI_OUTPUT;
	else if (gui.equalsIgnoreCase("GUI_PID")) return DBDConstants.GUI_PID;
	else if (gui.equalsIgnoreCase("GUI_PULSE")) return DBDConstants.GUI_PULSE;
	else if (gui.equalsIgnoreCase("GUI_SELECT")) return DBDConstants.GUI_SELECT;
	else if (gui.equalsIgnoreCase("GUI_SEQ1")) return DBDConstants.GUI_SEQ1;
	else if (gui.equalsIgnoreCase("GUI_SEQ2")) return DBDConstants.GUI_SEQ2;
	else if (gui.equalsIgnoreCase("GUI_SEQ3")) return DBDConstants.GUI_SEQ3;
	else if (gui.equalsIgnoreCase("GUI_SUB")) return DBDConstants.GUI_SUB;
	else if (gui.equalsIgnoreCase("GUI_TIMER")) return DBDConstants.GUI_TIMER;
	else if (gui.equalsIgnoreCase("GUI_WAVE")) return DBDConstants.GUI_WAVE;
	else if (gui.equalsIgnoreCase("GUI_SCAN")) return DBDConstants.GUI_SCAN;
	else return DBDConstants.GUI_UNDEFINED;
}
/**
 * This method was created in VisualAge.
 * @return java.io.StreamTokenizer
 * @param fileName java.lang.String
 */
public static StreamTokenizer getStreamTokenizer(String fileName) {

	FileInputStream fi = null;
	StreamTokenizer tokenizer = null;
	
	try	{
		fi = new FileInputStream(fileName);
		tokenizer = new StreamTokenizer(new BufferedReader(new InputStreamReader(fi)));
		initializeTokenizer(tokenizer);
	} catch (IOException e) {
		Console.getInstance().println("\no) Error occured while opening file '"+fileName+"'");
		Console.getInstance().println(e);
	}

	return tokenizer;
}
/**
 * This method was created in VisualAge.
 * @param st java.io.StreamTokenizer
 */
public static void initializeTokenizer(StreamTokenizer tokenizer) {
	tokenizer.whitespaceChars(0, 32);
	tokenizer.wordChars(33, 255);			// reset
	tokenizer.eolIsSignificant(true);
	tokenizer.commentChar('#');
	tokenizer.quoteChar(DBDConstants.quoteChar);
	tokenizer.whitespaceChars(',', ',');
	tokenizer.whitespaceChars('{', '{');
	tokenizer.whitespaceChars('(', '(');
	tokenizer.whitespaceChars(')',')');
}
/**
 * This method was created in VisualAge.
 * @param data com.cosylab.vdct.dbd.DBDData
 * @param tokenizer java.io.StreamTokenizer
 */
public static void processDBD(DBDData data, StreamTokenizer tokenizer, String fileName) {
	
	DBDRecordData rd;
	DBDMenuData md;
	DBDDeviceData dd;

	String include_filename;
	StreamTokenizer inctokenizer = null;

	if (data!=null) 
	
	try	{
		
		while (tokenizer.nextToken() != tokenizer.TT_EOF)
			if (tokenizer.ttype == tokenizer.TT_WORD)

				/****************** records ********************/

				if (tokenizer.sval.equalsIgnoreCase(RECORDTYPE)) {
					rd = new DBDRecordData();

					// read record_type
					tokenizer.nextToken();
					if ((tokenizer.ttype == tokenizer.TT_WORD) ||
						(tokenizer.ttype == DBDConstants.quoteChar)) rd.setName(tokenizer.sval);
					else throw (new DBDParseException("Invalid record_type...", tokenizer, fileName));

					processFields(rd, tokenizer, fileName);
					data.addRecord(rd);
				}
				
				/******************* menus *********************/

				else if (tokenizer.sval.equalsIgnoreCase(MENU)) {
					md = new DBDMenuData();

					// read menu_name
					tokenizer.nextToken();
					if ((tokenizer.ttype == tokenizer.TT_WORD) ||
						(tokenizer.ttype == DBDConstants.quoteChar)) md.setName(tokenizer.sval);
					else throw (new DBDParseException("Invalid menu_name...", tokenizer, fileName));

					processMenuChoices(md, tokenizer, fileName);
					data.addMenu(md);
				}
				
				/******************* devices *********************/

				else if (tokenizer.sval.equalsIgnoreCase(DEVICE)) {
					dd = new DBDDeviceData();

					// read record_type
					tokenizer.nextToken();
					if ((tokenizer.ttype == tokenizer.TT_WORD) ||
						(tokenizer.ttype == DBDConstants.quoteChar)) dd.setRecord_type(tokenizer.sval);
					else throw (new DBDParseException("Invalid record_type...", tokenizer, fileName));

					// read link_type
					tokenizer.nextToken();
					if ((tokenizer.ttype == tokenizer.TT_WORD) ||
						(tokenizer.ttype == DBDConstants.quoteChar)) dd.setLink_type(tokenizer.sval);
					else throw (new DBDParseException("Invalid link_type...", tokenizer, fileName));

					// read dset_name
					tokenizer.nextToken();
					if ((tokenizer.ttype == tokenizer.TT_WORD) ||
						(tokenizer.ttype == DBDConstants.quoteChar)) dd.setDset_name(tokenizer.sval);
					else throw (new DBDParseException("Invalid dset_name...", tokenizer, fileName));

					// read choice_string
					tokenizer.nextToken();
					if (tokenizer.ttype == DBDConstants.quoteChar) dd.setChoice_string(tokenizer.sval);
					else throw (new DBDParseException("Invalid choice_string...", tokenizer, fileName));

					data.addDevice(dd);
				}
				
				/****************** include ********************/

				else if (tokenizer.sval.equalsIgnoreCase(INCLUDE)) {

					// read incude_filename
					tokenizer.nextToken();
					if (tokenizer.ttype == DBDConstants.quoteChar) include_filename=tokenizer.sval;
					else throw (new DBDParseException("Invalid include_filename...", tokenizer, fileName));

					include_filename = com.cosylab.vdct.util.StringUtils.replaceFileName(fileName, include_filename);

					inctokenizer = getStreamTokenizer(include_filename);
					if (inctokenizer!=null) processDBD(data, inctokenizer, include_filename);
				}

				/****************** path ********************/
				else if (tokenizer.sval.equalsIgnoreCase(PATH) ||
 						 tokenizer.sval.equalsIgnoreCase(ADDPATH))
					Console.getInstance().println("Warning: 'path' and 'addpath' commands are not supported...");
						
				// driver, breaktable not read

	} catch (Exception e) {
		Console.getInstance().println("\n"+e);
	}	
	
}
/**
 * This method was created in VisualAge.
 * @param tokenizer java.io.StreamTokenizer
 */
public static void processFields(DBDRecordData rd, StreamTokenizer tokenizer, String fileName) throws Exception {

	DBDFieldData fd;

	String include_filename;
	StreamTokenizer inctokenizer = null;
	
	if (rd!=null)

	/********************** fields area *************************/
					
	while (tokenizer.nextToken() != tokenizer.TT_EOF) 
		if (tokenizer.ttype == tokenizer.TT_WORD) 
			if (tokenizer.sval.equals(ENDSTR))	break;
			else if (tokenizer.sval.equalsIgnoreCase(FIELD)) {
				fd = new DBDFieldData();

				// read field_name
				tokenizer.nextToken();
				if ((tokenizer.ttype == tokenizer.TT_WORD) ||
					(tokenizer.ttype == DBDConstants.quoteChar)) fd.setName(tokenizer.sval);
				else throw (new DBDParseException("Invalid field_name...", tokenizer, fileName));
					
				// read field_type
				tokenizer.nextToken();
				if ((tokenizer.ttype == tokenizer.TT_WORD) ||
					(tokenizer.ttype == DBDConstants.quoteChar)) fd.setField_type(getFieldType(tokenizer.sval));
				else throw (new DBDParseException("Invalid field_type...", tokenizer, fileName));
				if (fd.field_type == DBDConstants.NOT_DEFINED) throw (new DBDParseException("Invalid field_type...", tokenizer, fileName));

				/****************** field def. ********************/
								
			    while (tokenizer.nextToken() != tokenizer.TT_EOF)
					if (tokenizer.ttype == tokenizer.TT_WORD) 
						if (tokenizer.sval.equals(ENDSTR))	break;
						
						else if (tokenizer.sval.equalsIgnoreCase(INITIAL)) {
							tokenizer.nextToken();
							if (tokenizer.ttype == DBDConstants.quoteChar) fd.setInit_value(tokenizer.sval);
							else throw (new DBDParseException("Invalid init_value...", tokenizer, fileName));
						}
						
						else if (tokenizer.sval.equalsIgnoreCase(BASE)) {
							tokenizer.nextToken();
							if (tokenizer.ttype == tokenizer.TT_WORD) fd.setBase_type(getBaseType(tokenizer.sval));
							else throw (new DBDParseException("Invalid base_type...", tokenizer, fileName));
						}
						
						else if (tokenizer.sval.equalsIgnoreCase(SIZE)) {
							tokenizer.nextToken();
							/*if (tokenizer.ttype == DBDConstants.quoteChar) fd.setSize_value(Integer.parseInt(tokenizer.sval));
							else*/ if (tokenizer.ttype == tokenizer.TT_NUMBER) fd.setSize_value((int)tokenizer.nval);
							else throw (new DBDParseException("Invalid size_value...", tokenizer, fileName));
						}
						
						else if (tokenizer.sval.equalsIgnoreCase(MENU)) {
							tokenizer.nextToken();
							if ((tokenizer.ttype == DBDConstants.quoteChar) ||
								(tokenizer.ttype == tokenizer.TT_WORD)) fd.setMenu_name(tokenizer.sval);
							else throw (new DBDParseException("Invalid menu_name...", tokenizer, fileName));
						}
						
						else if (tokenizer.sval.equalsIgnoreCase(PROMPT)) {
							tokenizer.nextToken();
							if (tokenizer.ttype == DBDConstants.quoteChar) fd.setPrompt_value(tokenizer.sval);
							else throw (new DBDParseException("Invalid prompt_value...", tokenizer, fileName));
						}

						else if (tokenizer.sval.equalsIgnoreCase(PROMPTGROUP)) {
							tokenizer.nextToken();
							if (tokenizer.ttype == tokenizer.TT_WORD) fd.setGUI_type(getGUIType(tokenizer.sval));
							else throw (new DBDParseException("Invalid gui_gruop...", tokenizer, fileName));
						}

						else tokenizer.nextToken();			// "read"/skip data
								
					/**************************************************/

					rd.addField(fd);

			}
			else if (tokenizer.sval.equalsIgnoreCase(INCLUDE)) {

				// read incude_filename
				tokenizer.nextToken();
				if (tokenizer.ttype == DBDConstants.quoteChar) include_filename=tokenizer.sval;
				else throw (new DBDParseException("Invalid include_filename...", tokenizer, fileName));

				include_filename = com.cosylab.vdct.util.StringUtils.replaceFileName(fileName, include_filename);

				inctokenizer = getStreamTokenizer(include_filename);
				if (inctokenizer!=null) processFields(rd, inctokenizer, include_filename);
			}	
						
	/***********************************************************/

}
/**
 * This method was created in VisualAge.
 * @param md com.cosylab.vdct.dbd.DBDMenuData
 * @param tokenizer java.io.StreamTokenizer
 * @exception java.lang.Exception The exception description.
 */
public static void processMenuChoices(DBDMenuData md, StreamTokenizer tokenizer, String fileName) throws Exception {

	String choice_name;
	String choice_value;

	String include_filename;
	StreamTokenizer inctokenizer = null;

	if (md!=null)
	
	/********************** choices area *************************/
					
	while (tokenizer.nextToken() != tokenizer.TT_EOF) 
		if (tokenizer.ttype == tokenizer.TT_WORD) 
			if (tokenizer.sval.equals(ENDSTR)) break;
			else if (tokenizer.sval.equalsIgnoreCase(CHOICE)) {
				
				// read choice_name
				tokenizer.nextToken();
				if ((tokenizer.ttype == tokenizer.TT_WORD) ||
					(tokenizer.ttype == DBDConstants.quoteChar)) choice_name=tokenizer.sval;
				else throw (new DBDParseException("Invalid choice_name...", tokenizer, fileName));
					
				// read choice_value
				tokenizer.nextToken();
				if (tokenizer.ttype == DBDConstants.quoteChar) choice_value=tokenizer.sval;
				else throw (new DBDParseException("Invalid choice_value...", tokenizer, fileName));

				md.addMenuChoice(choice_name, choice_value);

			}
			
			else if (tokenizer.sval.equalsIgnoreCase(INCLUDE)) {

				// read incude_filename
				tokenizer.nextToken();
				if (tokenizer.ttype == DBDConstants.quoteChar) include_filename=tokenizer.sval;
				else throw (new DBDParseException("Invalid include_filename...", tokenizer, fileName));
	
				include_filename = com.cosylab.vdct.util.StringUtils.replaceFileName(fileName, include_filename);

				inctokenizer = getStreamTokenizer(include_filename);
				if (inctokenizer!=null) processMenuChoices(md, inctokenizer, include_filename);
			}	
						
	/***********************************************************/

}
/**
 * This method was created in VisualAge.
 * @return Vector
 * @param fileName java.lang.String
 */
public static DBDData resolveDBD(DBDData data, String fileName) {
	
	if (data==null) data = new DBDData();

	StreamTokenizer tokenizer = getStreamTokenizer(fileName);
	if (tokenizer!=null) processDBD(data, tokenizer, fileName);
	else return null;
	
	return data;
}
/**
 * This method was created in VisualAge.
 * @return Vector
 * @param fileName java.lang.String
 */
public static DBDData resolveDBDasURL(DBDData data, java.net.URL url) {
	
	if (data==null) data = new DBDData();

	InputStream fi = null;
	StreamTokenizer tokenizer = null;
	
	try	{
		fi = url.openStream();
		tokenizer = new StreamTokenizer(new BufferedReader(new InputStreamReader(fi)));
		initializeTokenizer(tokenizer);
	} catch (Exception e) {
		Console.getInstance().println("\nError occured while opening URL '"+url.toString()+"'");
		Console.getInstance().println(e);
		return null;
	}

	if (tokenizer!=null) processDBD(data, tokenizer, url.toString());
	else return null;
	
	return data;
}
}
