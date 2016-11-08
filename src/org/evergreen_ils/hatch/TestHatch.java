/* -----------------------------------------------------------------------
 * Copyright 2016 King County Library System
 * Bill Erickson <berickxx@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * -----------------------------------------------------------------------
 */
package org.evergreen_ils.hatch;

import java.util.logging.Logger;
import org.json.*;


public class TestHatch {
    static MessageIO io;
    static final Logger logger = Logger.getLogger("org.evergreen_ils.hatch");
    static final String origin = "https://test.hatch.evergreen-ils.org";

    public static void pause() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}
    }

    public static void doSends() {
        int msgid = 1;
        int clientid = 1;
        JSONObject obj;

        // get a list of stored keys
        obj = new JSONObject();
        obj.put("msgid", msgid++);
        obj.put("clientid", clientid);
        obj.put("origin", origin);
        obj.put("action", "keys");
        io.sendMessage(obj);

        pause();

        // store a string
        obj = new JSONObject();
        obj.put("msgid", msgid++);
        obj.put("clientid", clientid);
        obj.put("origin", origin);
        obj.put("action", "set");
        obj.put("key", "eg.hatch.test.key1");
        obj.put("content", "Rando content, now with cheese and Aljamía");
        io.sendMessage(obj);

        pause();

        // store a value
        obj = new JSONObject();
        obj.put("msgid", msgid++);
        obj.put("clientid", clientid);
        obj.put("origin", origin);
        obj.put("action", "get");
        obj.put("key", "eg.hatch.test.key1");
        io.sendMessage(obj);

        // store an array
        obj = new JSONObject();
        obj.put("msgid", msgid++);
        obj.put("clientid", clientid);
        obj.put("origin", origin);
        obj.put("action", "set");
        obj.put("key", "eg.hatch.test.key2");
        JSONArray arr = new JSONArray();
        arr.put(123);
        arr.put("23 Skidoo");
        obj.put("content", arr);
        io.sendMessage(obj);

        pause();

        obj = new JSONObject();
        obj.put("msgid", msgid++);
        obj.put("clientid", clientid);
        obj.put("origin", origin);
        obj.put("action", "get");
        obj.put("key", "eg.hatch.test.key2");
        io.sendMessage(obj);

        pause();

        obj = new JSONObject();
        obj.put("msgid", msgid++);
        obj.put("clientid", clientid);
        obj.put("origin", origin);
        obj.put("action", "keys");
        io.sendMessage(obj);

        pause();

        // get a list of printers
        obj = new JSONObject();
        obj.put("msgid", msgid++);
        obj.put("clientid", clientid);
        obj.put("origin", origin);
        obj.put("action", "printers");
        io.sendMessage(obj);

        pause();

        // get a list of printers
        obj = new JSONObject();
        obj.put("msgid", msgid++);
        obj.put("clientid", clientid);
        obj.put("origin", origin);
        obj.put("action", "printer-options");
        io.sendMessage(obj);

        pause();

        /*
        // Printing tests
        
        obj = new JSONObject();
        obj.put("msgid", msgid++);
        obj.put("clientid", clientid);
        obj.put("origin", origin);
        obj.put("action", "print");
        obj.put("contentType", "text/plain");
        obj.put("content", "Hello, World!");
        obj.put("showDialog", true); // avoid auto-print while testing
        io.sendMessage(obj);

        pause();

        obj = new JSONObject();
        obj.put("msgid", msgid++);
        obj.put("clientid", clientid);
        obj.put("origin", origin);
        obj.put("action", "print");
        obj.put("contentType", "text/html");
        obj.put("content", "<html><body><b>HELLO WORLD</b><img src='" +
            "http://evergreen-ils.org/wp-content/uploads/2013/09/copy-Evergreen_Logo_sm072.jpg"
            + "'/></body></html>");
        obj.put("showDialog", true); // avoid auto-print while testing

        JSONObject settings = new JSONObject();
        settings.put("copies", 2);
        obj.put("settings", settings);
        io.sendMessage(obj);

        pause();
        
        */
    }

    /**
     * Log all received message as a JSON string
     */
    public static void doReceive() {
        while (true) {
            JSONObject resp = io.recvMessage();
            logger.info("TestJSON:doReceive(): " + resp.toString());
        }
    }

    public static void main (String[] args) {
        io = new MessageIO();
        io.listen();

        if (args.length > 0 && args[0].equals("receive")) {
            doReceive();
        } else {
            doSends();
        }
    }
}

