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

import java.util.logging.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.regex.Pattern;
import org.json.*;

/**
 * Reads and writes JSON strings from STDIN / to STDOUT.
 *
 * Each string is prefixed with a 4-byte message length header.  All I/O
 * occurs in a separate thread, so no blocking of the main thread occurs.
 */
public class MessageIO {

    private LinkedBlockingQueue<JSONObject> inQueue;
    private LinkedBlockingQueue<JSONObject> outQueue;

    private MessageReader reader;
    private MessageWriter writer;

    static final Logger logger = Logger.getLogger("org.evergreen_ils.hatch");

    public MessageIO() {
        inQueue = new LinkedBlockingQueue<JSONObject>();
        outQueue = new LinkedBlockingQueue<JSONObject>();
        reader = new MessageReader();
        writer = new MessageWriter();
    }

    /**
     * Starts the read and write threads.
     */
    public void listen() {
        writer.start();
        reader.start();
    }

    /**
     * Receive one message from STDIN.
     *
     * This call blocks the current thread until a message is available.
     */
    public JSONObject recvMessage() {
        while (true) {
            try {
                return inQueue.take();
            } catch (InterruptedException e) {}
        }
    }

    /**
     * Queue a message for sending to STDOUT.
     */
    public void sendMessage(JSONObject msg) {
        outQueue.offer(msg);
    }

    /**
     * Thrown when STDIN or STDOUT are closed.
     */
    class EndOfStreamException extends IOException { }

    /**
     * Reads JSON-encoded strings from STDIN.
     *
     * As messages arrive, they are enqueued for access by recvMessage().
     *
     * Each message is prefixed with a 4-byte message length header.
     */
    class MessageReader extends Thread {

        /**
         * Converts a 4-byte array to its integer value.
         */
        private int bytesToInt(byte[] bytes) {
            return 
                  (bytes[3] << 24) & 0xff000000 
                | (bytes[2] << 16) & 0x00ff0000
                | (bytes[1] <<  8) & 0x0000ff00 
                | (bytes[0] <<  0) & 0x000000ff;
        }

        /**
         * Reads one message from STDIN.
         *
         * This method blocks until a message is available.
         */
        private String readOneMessage() throws EndOfStreamException, IOException {
            byte[] lenBytes = new byte[4];
            int bytesRead = System.in.read(lenBytes);

            if (bytesRead == -1) {
                throw new EndOfStreamException();
            }

            int msgLength = bytesToInt(lenBytes);

            if (msgLength == 0) {
                throw new IOException(
                    "Inbound message is 0 bytes.  I/O interrupted?");
            }

            byte[] msgBytes = new byte[msgLength];

            bytesRead = System.in.read(msgBytes);

            if (bytesRead == -1) {
                throw new EndOfStreamException();
            }

            String message = new String(msgBytes, "UTF-8");

            logger.finest("MessageReader read: " + message);

            return message;
        }

        /**
         * Read messages from STDIN until STDIN is closed or the application exits.
         */
        public void run() {

            while (true) {

                String message = "";
                JSONObject jsonMsg = null;

                try {
                    
                    message = readOneMessage();
                    jsonMsg = new JSONObject(message);

                } catch (EndOfStreamException eose) {

                    logger.warning("STDIN closed... exiting");
                    System.exit(1);

                } catch (IOException ioe) {

                    logger.warning(ioe.toString());
                    continue;

                } catch (JSONException je) {

                    logger.warning("Error parsing JSON message on STDIN " +
                        je.toString() + " : " + message);
                    continue;
                }

                inQueue.offer(jsonMsg);
            }
        }
    }

    /**
     * Writes JSON-encoded strings from STDOUT.
     *
     * As messages are queued for delivery, each is serialized as a JSON
     * string and stamped with a 4-byte length header.
     */
    class MessageWriter extends Thread {

        /**
         * Returns the 4-byte array representation of an integer.
         */
        private byte[] intToBytes(int length) {
            byte[] bytes = new byte[4];
            bytes[0] = (byte) (length & 0xFF);
            bytes[1] = (byte) ((length >> 8) & 0xFF);
            bytes[2] = (byte) ((length >> 16) & 0xFF);
            bytes[3] = (byte) ((length >> 24) & 0xFF);
            return bytes;
        }

        /**
         * Encodes and writes one message to STDOUT.
         */
        public void writeOneMessage(String message) throws IOException {
            logger.finest("MessageWriter sending: " + message);
            System.out.write(intToBytes(message.getBytes("UTF-8").length));
            System.out.write(message.getBytes("UTF-8"));
            System.out.flush();
        }

        /**
         * Waits for messages to be queued for delivery and writes
         * each to STDOUT until STDOUT is closed or the application exits.
         */
        public void run() {

            while (true) {
                try {

                    // take() blocks the thread until a message is available
                    JSONObject jsonMsg = outQueue.take();

                    writeOneMessage(jsonMsg.toString());

                } catch (InterruptedException e) {
                    // interrupted, go back and listen
                    continue;
                } catch (IOException ioe) {
                    logger.warning(
                        "Error writing message to STDOUT: " + ioe.toString());
                }
           }
        }
    }
}

