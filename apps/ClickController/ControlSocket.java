/*
 * ControlSocket.java -- class for manipulating ControlSockets
 * Douglas S. J. De Couto, Eddie Kohler
 *
 * Copyright (c) 2000 Massachusetts Institute of Technology.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, subject to the conditions
 * listed in the Click LICENSE file. These conditions include: you must
 * preserve this copyright notice, and you cannot mention the copyright
 * holders in advertising related to the Software without their permission.
 * The Software is provided WITHOUT ANY WARRANTY, EXPRESS OR IMPLIED. This
 * notice is a summary of the Click LICENSE file; the license in that file is
 * legally binding.
 */

import java.net.*;
import java.io.*;
import java.util.Vector;

/**
 * Manage a user-level click Router via its TCP ControlSocket.  
 * 
 * @author Douglas S. J. De Couto, Eddie Kohler
 */

public class ControlSocket {
    
    static public class ControlSocketException extends Exception {
	ControlSocketException() {
	    super("ControlSocket error");
	}
	ControlSocketException(String s) {
	    super(s);
	}
    }

    static public class NoSuchElementException extends ControlSocketException {
	NoSuchElementException() { 
	    super("No such element"); 
	}
	NoSuchElementException(String s) { 
	    super("No such element `" + s + "'"); 
	}
    }
    
    static public class NoSuchHandlerException extends ControlSocketException {
	NoSuchHandlerException() { 
	    super("No such handler"); 
	}
	NoSuchHandlerException(String hid) {
	    super("No such handler `" + hid + "'"); 
	}
    }

    static public class PermissionDeniedException extends ControlSocketException {
	PermissionDeniedException() {
	    super("Permission denied calling handler"); 
	}
	PermissionDeniedException(String hid) {
	    super("Permission denied calling handler `" + hid + "'"); 
	}
    }

    static public class HandlerErrorException extends ControlSocketException {
	HandlerErrorException() { 
	    super("Unspecified error calling handler"); 
	}
	HandlerErrorException(String hid, String description) { 
	    super(description); 
	}
    }

    static public class HandlerFormatException extends ControlSocketException {
	HandlerFormatException() { 
	    super("Unspecified error calling handler"); 
	}
	HandlerFormatException(String hid) { 
	    super("Bad format in handler `" + hid + "'"); 
	}
    }

    private InetAddress _host;
    private int _port;
    private Socket _sock;
    private BufferedReader _in;
    private BufferedWriter _out;
    private int _protocolMinorVersion;
    
    private static final int CODE_OK = 200;
    private static final int CODE_OK_WARN = 220;
    private static final int CODE_SYNTAX_ERR = 500;
    private static final int CODE_UNIMPLEMENTED = 501;
    private static final int CODE_NO_EL = 510;
    private static final int CODE_NO_HANDLER = 511;
    private static final int CODE_HANDLER_ERR = 520;
    private static final int CODE_NO_PERM = 530;
    
    public static final int PROTOCOL_MAJOR_VERSION = 1;
    public static final int PROTOCOL_MINOR_VERSION = 0;

    /* XXX not sure timeout is a good idea; if we do timeout, we should
       reset the connection (close and re-open) to get rid of all old
       data... */
    public static final int _sock_timeout = 0; // 1500; // msecs

    /**
     * Constructs a new ControlSocket.
     *
     * @param host Machine that the user-level click is running on.
     * @param port Port the Click ControlSocket is listening on.
     * @exception IOException If there was a problem setting up the
     * socket and streams, or if the ControlSocket version is wrong, or
     * if the ControlSocket returned a bad response.  
     * @see java.net.InetAddress 
     */
    public ControlSocket(InetAddress host, int port) throws IOException {
	_host = host;
	_port = port;
	
	/* 
	 * setup connection to the node's click ControlSocket
	 */
	_sock = new Socket(_host, _port);
	_sock.setSoTimeout(_sock_timeout); 
	
	InputStream is = _sock.getInputStream();
	OutputStream os = _sock.getOutputStream();
	
	_in = new BufferedReader(new InputStreamReader(is));
	_out = new BufferedWriter(new OutputStreamWriter(os));
	
	/*
	 * check version 
	 */
	try {
	    String banner = _in.readLine();
	    int slash = banner.indexOf('/');
	    int dot = (slash >= 0 ? banner.indexOf('.', slash + 1) : -1);
	    if (slash < 0 || dot < 0) {
		_sock.close();
		throw new IOException("Unexpected greeting from ControlSocket");
	    }
	    int majorVersion = Integer.parseInt(banner.substring(slash + 1, dot));
	    int minorVersion = Integer.parseInt(banner.substring(dot + 1));
	    if (majorVersion != PROTOCOL_MAJOR_VERSION
		|| minorVersion < PROTOCOL_MINOR_VERSION) {
		_sock.close();
		throw new IOException("Wrong ControlSocket version");
	    }
	    _protocolMinorVersion = minorVersion;
	} catch (InterruptedIOException e) {
	    // read timed out
	    throw e;
	} catch (NumberFormatException e) {
	    throw new IOException("Unexpected greeting from ControlSocket");
	}
    }
    
    /**
     * Returns a String describing the socket's destination address and port.
     *
     * @return Socket description.
     */
    public String socketName() {
	return _sock.getInetAddress().toString() + ":" + _sock.getPort();
    }

    /**
     * Gets a String containing the router's configuration. 
     *
     * @return Router configuration.
     * @exception NoSuchHandlerException If there is no configuration read handler.
     * @exception HandlerErrorException If the handler returned an error.
     * @exception PermissionDeniedException If the router would not let us access the handler.
     * @exception IOException If there was some other error accessing
     * the handler (e.g., there was a stream or socket error, the
     * ControlSocket returned an unknwon unknown error code, or the
     * response could otherwise not be understood). 
     * @see #getRouterFlatConfig
     * @see #getConfigElementNames
     */
    public String getRouterConfig() 
	throws ControlSocketException, IOException {
	return readString(null, "config");
    }
    
    /**
     * Gets a String containing the router's flattened configuration. 
     *
     * @return Flattened router configuration.
     * @exception NoSuchHandlerException If there is no flattened configuration read handler.
     * @exception HandlerErrorException If the handler returned an error.
     * @exception PermissionDeniedException If the router would not let us access the handler.
     * @exception IOException If there was some other error accessing
     * the handler (e.g., there was a stream or socket error, the
     * ControlSocket returned an unknwon unknown error code, or the
     * response could otherwise not be understood). 
     * @see #getRouterConfig
     * @see #getConfigElementNames
     */
    public String getRouterFlatConfig()
	throws ControlSocketException, IOException {
	return readString(null, "flatconfig");
    }

    /**
     * Gets a String containing the router's version.
     *
     * @return Version string.
     * @exception NoSuchHandlerException If there is no version read handler.
     * @exception HandlerErrorException If the handler returned an error.
     * @exception PermissionDeniedException If the router would not let us access the handler.
     * @exception IOException If there was some other error accessing
     * the handler (e.g., there was a stream or socket error, the
     * ControlSocket returned an unknwon unknown error code, or the
     * response could otherwise not be understood). 
     * @see java.lang.String
     */
    public String getRouterVersion()
	throws ControlSocketException, IOException {
	return readString(null, "version").trim();
    }
    
    /**
     * Gets the names of elements in the current router configuration.
     *
     * @return Vector of Strings of the element names.
     * @exception NoSuchHandlerException If there is no element list read handler.
     * @exception HandlerErrorException If the handler returned an error.
     * @exception PermissionDeniedException If the router would not let us access the handler.
     * @exception IOException If there was some other error accessing
     * the handler (e.g., there was a stream or socket error, the
     * ControlSocket returned an unknwon unknown error code, or the
     * response could otherwise not be understood). 
     * @see #getElementHandlers
     * @see #getRouterConfig
     * @see #getRouterFlatConfig
     */
    public Vector getConfigElementNames() 
	throws ControlSocketException, IOException {
	char[] buf = read(null, "list");
	
	// how many elements?
	int i;
	for (i = 0; i < buf.length && buf[i] != '\n'; i++)
	    ; // do it

	int numElements = 0;
	try {
	    numElements = Integer.parseInt(new String(buf, 0, i));
	} catch (NumberFormatException ex) { 
	    throw new HandlerFormatException("element list"); 
	}
	
	Vector v = StringUtils.split(buf, i + 1, '\n');
	if (v.size() != numElements)
	    throw new HandlerFormatException("element list");
	return v;
    }
    
    /**
     * Gets the names of element types that the router knows about.
     *
     * @return Vector of Strings of the element names.
     * @exception NoSuchHandlerException If there is no element list read handler.
     * @exception HandlerErrorException If the handler returned an error.
     * @exception PermissionDeniedException If the router would not let us access the handler.
     * @exception IOException If there was some other error accessing
     * the handler (e.g., there was a stream or socket error, the
     * ControlSocket returned an unknwon unknown error code, or the
     * response could otherwise not be understood). 
     */
    public Vector getRouterClasses()
	throws ControlSocketException, IOException {
	char[] buf = read(null, "classes");
	return StringUtils.split(buf, 0, '\n');
    }
    
    /**
     * Gets the names of packages that the router knows about.
     *
     * @return Vector of Strings of the package names.
     * @exception NoSuchHandlerException If there is no element list read handler.
     * @exception HandlerErrorException If the handler returned an error.
     * @exception PermissionDeniedException If the router would not let us access the handler.
     * @exception IOException If there was some other error accessing
     * the handler (e.g., there was a stream or socket error, the
     * ControlSocket returned an unknwon unknown error code, or the
     * response could otherwise not be understood). 
     */
    public Vector getRouterPackages()
	throws ControlSocketException, IOException {
	char[] buf = read(null, "packages");
	return StringUtils.split(buf, 0, '\n');
    }
    
    /**
     * Gets the names of the current router configuration requirements.
     *
     * @return Vector of Strings of the package names.
     * @exception NoSuchHandlerException If there is no element list read handler.
     * @exception HandlerErrorException If the handler returned an error.
     * @exception PermissionDeniedException If the router would not let us access the handler.
     * @exception IOException If there was some other error accessing
     * the handler (e.g., there was a stream or socket error, the
     * ControlSocket returned an unknwon unknown error code, or the
     * response could otherwise not be understood). 
     * @see #getRouterConfig
     * @see #getRouterFlatConfig
     */
    public Vector getConfigRequirements()
	throws ControlSocketException, IOException {
	char[] buf = read(null, "requirements");
	return StringUtils.split(buf, 0, '\n');
    }

    public static class HandlerInfo {
	String elementName;
	String handlerName;
	boolean canRead;
	boolean canWrite;
	
	HandlerInfo() {
	    this(null, null);
	}
	HandlerInfo(String el) {
	    this(el, null);
	}
	HandlerInfo(String el, String handler) {
	    elementName = el;
	    handlerName = handler;
	    canRead = canWrite = false; 
	}
	
	public String getDescription() {
	    if (elementName == null)
		return handlerName;
	    else
		return elementName + "." + handlerName;
	}
	
	public String toString() {
	    return handlerName;
	}
    }
    
    /**
     * Gets the information about an element's handlers in the current
     * router configuration.
     *
     * @param el The element name.
     * @return Vector of HandlerInfo structures.
     * @exception NoSuchHandlerException If there is no handler list read handler.
     * @exception NoSuchElementException If there is no such element in the current configuration.
     * @exception HandlerErrorException If the handler returned an error.
     * @exception PermissionDeniedException If the router would not let us access the handler.
     * @exception IOException If there was some other error accessing
     * the handler (e.g., there was a stream or socket error, the
     * ControlSocket returned an unknwon unknown error code, or the
     * response could otherwise not be understood). 
     * @see #HandlerInfo
     * @see #getConfigElementNames
     * @see #getRouterConfig
     * @see #getRouterFlatConfig
     */
    public Vector getElementHandlers(String elementName)
	throws ControlSocketException, IOException {
	char[] buf = read(elementName, "handlers");
	Vector vh = StringUtils.split(buf, 0, '\n');
	
	Vector v = new Vector();
	for (int i = 0; i < vh.size(); i++) {
	    String s = (String) vh.elementAt(i);
	    int j;
	    for (j = 0; j < s.length() && !Character.isWhitespace(s.charAt(j)); j++)
		; // find record split
	    if (j == s.length())
		throw new HandlerFormatException(elementName + ".handlers");
	    HandlerInfo hi = new HandlerInfo(elementName, s.substring(0, j).trim());
	    while (j < s.length() && Character.isWhitespace(s.charAt(j)))
		j++;
	    for ( ; j < s.length(); j++) {
		char c = s.charAt(j);
		if (Character.toLowerCase(c) == 'r')
		    hi.canRead = true;
		else if (Character.toLowerCase(c) == 'w')
		    hi.canWrite = true;
		else if (Character.isWhitespace(c))
		    break;
	    }
	    v.addElement(hi);
	}
	return v;
    }
    
    /**
     * Returns the result of reading an element's handler.
     *
     * @param elementName The element name.
     * @param handlerName The handler name.
     * @return Char array containing the data.
     * @exception NoSuchHandlerException If there is no such read handler.
     * @exception NoSuchElementException If there is no such element in the current configuration.
     * @exception HandlerErrorException If the handler returned an error.
     * @exception PermissionDeniedException If the router would not let us access the handler.
     * @exception IOException If there was some other error accessing
     * the handler (e.g., there was a stream or socket error, the
     * ControlSocket returned an unknown unknown error code, or the
     * response could otherwise not be understood). 
     * @see #write
     * @see #getConfigElementNames
     * @see #getElementHandlers
     */
    public char[] read(String elementName, String handlerName) 
	throws ControlSocketException, IOException {
	String handler = handlerName;
	if (elementName != null)
	    handler = elementName + "." + handlerName;
	_out.write("READ " + handler + "\n");
	_out.flush();
	
	// make sure we read all the response lines... 
	String response = "";
	String lastLine = null;
	do {
	    lastLine = _in.readLine();
	    if (lastLine.length() < 4)
		throw new ControlSocketException("Bad response line from ControlSocket");
	    response = response + lastLine.substring(4);
	} while (lastLine.charAt(3) == '-');
	
	int code = getResponseCode(lastLine);
	if (code != CODE_OK && code != CODE_OK_WARN) 
	    handleErrCode(code, elementName, handlerName, response);
	
	response = _in.readLine();
	int num_bytes = getDataLength(response);
	if (num_bytes < 0)
	    throw new ControlSocketException("Bad length returned from ControlSocket");

	if (num_bytes == 0) 
	    return new char[0];
	
	// sometimes, read will return without completely filling the
	// buffer (e.g. on win32 JDK)
	char data[] = new char[num_bytes];
	int bytes_left = num_bytes;
	while (bytes_left > 0) {
	    int bytes_read = _in.read(data, num_bytes - bytes_left, bytes_left);
	    bytes_left -= bytes_read;
	}
	return data;
    }
    
    public String readString(String el, String handler) 
	throws ControlSocketException, IOException {
	return new String(read(el, handler));
    }
    
    public String readString(HandlerInfo hinfo) 
	throws ControlSocketException, IOException {
	return new String(read(hinfo.elementName, hinfo.handlerName));
    }


    private void handleWriteResponse(String elementName, String handlerName)
	throws ControlSocketException, IOException {
	String response = "";
	String lastLine = null;
	do {
	    lastLine = _in.readLine();
	    if (lastLine.length() < 4)
		throw new ControlSocketException("Bad response line from ControlSocket");
	    response = response + lastLine.substring(4) + "\n";
	} while (lastLine.charAt(3) == '-');
	
	int code = getResponseCode(lastLine);
	if (code != CODE_OK && code != CODE_OK_WARN) 
	    handleErrCode(code, elementName, handlerName, response);
    }
        
    /**
     * Writes data to an element's handler.
     *
     * @param elementName The element name.
     * @param handlerName The handler name.
     * @param data Char array containing the data.
     * @exception NoSuchHandlerException If there is no such write handler.
     * @exception NoSuchElementException If there is no such element in the current configuration.
     * @exception HandlerErrorException If the handler returned an error.
     * @exception PermissionDeniedException If the router would not let us access the handler.
     * @exception IOException If there was some other error accessing
     * the handler (e.g., there was a stream or socket error, the
     * ControlSocket returned an unknwon unknown error code, or the
     * response could otherwise not be understood). 
     * @see #write
     * @see #getConfigElementNames
     * @see #getElementHandlers
     */
    public void write(String elementName, String handlerName, char[] data) 
	throws ControlSocketException, IOException {

	String handler = handlerName;
	if (elementName != null)
	    handler = elementName + "." + handlerName;
	
	_out.write("WRITEDATA " + handler + " " + data.length + "\n");
	_out.write(data, 0, data.length);
	_out.flush();
	
	handleWriteResponse(elementName, handlerName);
    }

    public void write(String elementName, String handlerName, String data) 
	throws ControlSocketException, IOException {

	String handler = handlerName;
	if (elementName != null)
	    handler = elementName + "." + handlerName;
	
	_out.write("WRITEDATA " + handler + " " + data.length() + "\n");
	_out.write(data);
	_out.flush();
	
	handleWriteResponse(elementName, handlerName);
    }

    public void write(HandlerInfo info, char[] data) 
	throws ControlSocketException, IOException {
	write(info.elementName, info.handlerName, data);
    }

    public void write(HandlerInfo info, String data) 
	throws ControlSocketException, IOException {
	write(info.elementName, info.handlerName, data);
    }

    /**
     * Close the ControlSocket.
     */
    public void close() {
	try {
	    _sock.close(); 
	} catch (IOException ex) {
	}
    }

    private int getResponseCode(String s) {
	String code_str = s.substring(0, 3);
	try { 
	    return Integer.parseInt(code_str); 
	} catch (NumberFormatException ex) { 
	    return -1; 
	}
    }

    private int getDataLength(String s) {
	int i;
	for (i = 0; i < s.length() && !Character.isDigit(s.charAt(i)); i++)
	    ; // do it
	if (i == s.length()) 
	    return -1;
	String len_str = s.substring(i);
	try { 
	    return Integer.parseInt(len_str); 
	} catch (NumberFormatException ex) { 
	    return -1; 
	}
    }

    private void handleErrCode(int code, String elementName, 
			       String handlerName, String response) 
	throws ControlSocketException {

	String hid = handlerName;
	if (elementName != null)
	    hid = elementName + "." + handlerName;

	switch (code) {
	 case CODE_SYNTAX_ERR: 
	  throw new ControlSocketException("Syntax error calling handler `" + hid + "'");
	 case CODE_UNIMPLEMENTED: 
	  throw new ControlSocketException("Unimplemented ControlSocket command");
	 case CODE_NO_EL: 
	  throw new NoSuchElementException(elementName);
	 case CODE_NO_HANDLER: 
	  throw new NoSuchHandlerException(hid); 
	 case CODE_HANDLER_ERR: 
	  throw new HandlerErrorException(hid, response);
	 case CODE_NO_PERM: 
	  throw new PermissionDeniedException(hid); 
	 default: 
	  throw new ControlSocketException("Unknown ControlSocket error code " + code);
	}
    }


    /*
     * test driver 
     */
    public static void main(String args[]) {
	if (args.length == 0 || args.length > 3) {
	    System.out.println("to list router info, `java ControlSocket'");
	    System.out.println("to list handlers, `java ControlSocket <element>'");
	    System.out.println("to read, `java ControlSocket <element> <handler>'");
	    System.out.println("to write, `java ControlSocket <element> <handler> <data>'");
	    System.out.println("router info follows");
	}

	InetAddress localhost = null;
	try { 
	    // localhost = InetAddress.getLocalHost(); 
	    localhost = InetAddress.getByName("bermuda.lcs.mit.edu");
	} catch (UnknownHostException ex) { 
	    System.out.println("Can't get localhost address");
	    System.exit(-1);
	}
    
	try {
	    ControlSocket cs = new ControlSocket(localhost, 7777);
	    if (args.length == 2) {
		char data[] = cs.read(args[0], args[1]);
		System.out.println(data);
	    } else if (args.length == 3) {
		cs.write(args[0], args[1], args[2].toCharArray());
	    } else if (args.length == 1) {
		// dump element handler info
		Vector v = cs.getElementHandlers(args[0]);
		for (int i = 0; i < v.size(); i++) {
		    ControlSocket.HandlerInfo hi = (ControlSocket.HandlerInfo) v.elementAt(i);
		    System.out.print(hi.handlerName + "\t");
		    if (hi.canRead)
			System.out.print("r");
		    if (hi.canWrite)
			System.out.print("w");
		    System.out.println();
		}
	    } else {
		// dump router info
		System.out.println("Click version: " + cs.getRouterVersion().trim());
		System.out.print("Router classes: ");
		Vector v = cs.getRouterClasses();
		for (int i = 0; i < v.size(); i++)
		    System.out.print(v.elementAt(i) + " ");
		System.out.println();
		System.out.println("Router config:");
		System.out.print(cs.getRouterConfig());
		System.out.print("Config element names: ");
		v = cs.getConfigElementNames();
		for (int i = 0; i < v.size(); i++)
		    System.out.print(v.elementAt(i) + " ");
		System.out.println();
		System.out.print("Router packages: ");
		v = cs.getRouterPackages();
		for (int i = 0; i < v.size(); i++)
		    System.out.print(v.elementAt(i) + " ");
		System.out.println();
		System.out.print("Config requirements: ");
		v = cs.getConfigRequirements();
		for (int i = 0; i < v.size(); i++)
		    System.out.print(v.elementAt(i) + " ");
		System.out.println();	
	    }
	} catch (IOException ex) {
	    System.out.println("I/O error calling ControlSocket: " + ex.getMessage());
	    System.exit(1);
	} catch (ControlSocket.ControlSocketException ex) {
	    System.out.println(ex.getMessage());
	    System.exit(1);
	}
    }
}
