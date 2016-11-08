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

import org.json.*;
import java.io.File;
import java.util.logging.*;

/**
 * Dispatches requests received via MessageIO, sends responses back 
 * via MessageIO.
 */
public class RequestHandler extends Thread {

    /** STDIN/STDOUT handler */
    private static MessageIO io = new MessageIO();

    static final Logger logger = Logger.getLogger("org.evergreen_ils.hatch");

    /** Root directory for all FileIO operations */
    private static String profileDirectory = null;

    private void configure() {

        // Find the profile directory.
        // The profile directory + origin string represent the base 
        // directory for all file I/O for this session.
        if (profileDirectory == null) { // TODO: make configurable
            String home = System.getProperty("user.home");
            profileDirectory = new File(home, ".evergreen").getPath();
            if (profileDirectory == null) {
                logger.warning("Unable to set profile directory");
            }
        }
    }

    /**
     * Unpack a JSON request and send it to the necessary Hatch handler.
     *
     * @return True if the calling code should avoid calling reply() with
     * the response object.
     */
    private boolean dispatchRequest(
        JSONObject request, JSONObject response) throws JSONException {

        String action = request.getString("action");
        String origin = request.getString("origin");

        logger.info("Received message id=" + 
            response.get("msgid") + " action=" + action);

        if ("".equals(origin)) {
            response.put("status", 404); 
            response.put("message", "'origin' parameter required");
            return false;
        }

        String key = null;
        String content = null;
        FileIO fileIO = new FileIO(profileDirectory, origin);

        switch (action) {

            case "printers":
                response.put("content",
                    new PrintManager().getPrintersAsMaps());
                break;

            case "printer-options":

                String printer = request.optString("printer", null);
                JSONObject options = 
                    new PrintManager().getPrintersOptions(printer);

                if (options == null) {
                    response.put("status", 400); 
                    if (printer == null) {
                        response.put("message", "No default printer found");
                    } else {
                        response.put("message", "No such printer: " + printer);
                    }
                } else {
                    response.put("content", options);
                }

                break;

            case "print":
                // Confirm a minimal data set to enqueue print requests.
                content = request.getString("content");
                String contentType = request.getString("contentType");

                if (content == null || "".equals(content)) {
                    response.put("status", 400); 
                    response.put("message", "Empty print message");

                } else {
                    Hatch.enqueuePrintRequest(request);
                    // Responses to print requests are generated asynchronously 
                    // and delivered from the FX print thread via reply().
                    return true;
                }

            case "keys": // Return stored keys
                key = request.optString("key");
                response.put("content", fileIO.keys(key));
                break;
            
            case "get":
                key = request.getString("key");
                String val = fileIO.get(key);

                if (val != null) {
                    // Translate the JSON string stored by set() into a
                    // Java object that can be added to the response.
                    Object jsonBlob = new JSONTokener(val).nextValue();
                    response.put("content", jsonBlob);
                }
                break;

            case "set" :
                key = request.getString("key");

                // JSON-ify the thing stored under "content"
                String json = JSONObject.valueToString(request.get("content"));

                if (!fileIO.set(key, json)) {
                    response.put("status", 500);
                    response.put("message", "Unable to set key: " + key);
                }
                break;

            case "remove":
                key = request.getString("key");

                if (!fileIO.remove(key)) {
                    response.put("status", 500);
                    response.put("message", "Unable to remove key: " + key);
                }
                break;

            default:
                response.put("status", 404); 
                response.put("message", "Action not found: " + action);
        }

        return false;
    }

    /**
     * Most replies are delivered from within dispatchRequest, but some
     * like printing require the reply be delivered from another thread.
     */
    protected static void reply(JSONObject response) {
        io.sendMessage(response);
    }

    public void run() {

        configure();
        io.listen(); // STDIN/STDOUT handler

        while (true) { 

            boolean skipReply = false;
            JSONObject response = new JSONObject();

            // Status values overidden as needed by the dispatch handler.
            response.put("status", 200); 
            response.put("message", "OK");

            try {
                JSONObject request = io.recvMessage();

                response.put("clientid", request.getLong("clientid"));
                response.put("msgid", request.getLong("msgid"));

                skipReply = dispatchRequest(request, response); 

            } catch (JSONException je) {
                response.put("status", 400); 
                response.put("message", "Bad Request: " + je.toString());
            }

            if (!skipReply) reply(response);
        }
    }
}

