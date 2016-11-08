/* -----------------------------------------------------------------------
 * Copyright 2014 Equinox Software, Inc.
 * Bill Erickson <berick@esilibrary.com>
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

import java.util.Map;
import java.util.logging.*;
import org.json.*;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker.State;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Main class for Hatch.
 *
 * TODO
 *
 * Beware: On Mac OS, the "FX Application Thread" is renamed to 
 * "AppKit Thread" when the first call to print() or showPrintDialog() 
 * [in PrintManager] is made.  This is highly confusing when viewing logs.
 *
 */
public class Hatch extends Application {

    /** Browser Region for rendering and printing HTML */
    private BrowserView browser;

    /** BrowserView requires a stage for rendering */
    private Stage primaryStage;
    
    /** Queue of incoming print requests */
    private static LinkedBlockingQueue<JSONObject> 
        printRequestQueue = new LinkedBlockingQueue<JSONObject>();

    static final Logger logger = Logger.getLogger("org.evergreen_ils.hatch");
    
    /**
     * Printable region containing a browser.
     */
    class BrowserView extends Region {
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        public BrowserView() {
            getChildren().add(webView);
        }
    }

    /**
     * Shuffles print requests from the request queue into the 
     * FX Platform queue.
     *
     * This step allows the code to process print requests in order
     * and without nesting UI event loops.
     */
    class PrintRequestShuffler extends Thread {
        public void run() {
            while (true) {
                try {
                    JSONObject printRequest = printRequestQueue.take();
                    Platform.runLater(                                             
                        new Runnable() {                                           
                            @Override public void run() {                          
                                handlePrint(printRequest);
                            }                                                      
                        }                                                          
                    ); 
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    /** Add a print request object to the print queue. */
    public static void enqueuePrintRequest(JSONObject request) {
        printRequestQueue.offer(request);
    }

    /**
     * JavaFX startup call
     */
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        new PrintRequestShuffler().start();
    }

    /**
     * Build a browser view from the print content, tell the
     * browser to print itself.
     */
    private void handlePrint(JSONObject request) {

        browser = new BrowserView();
        Scene scene = new Scene(browser);
        primaryStage.setScene(scene);

        browser.webEngine.getLoadWorker()
            .stateProperty()
            .addListener( (ChangeListener<State>) (obsValue, oldState, newState) -> {
                logger.finest("browser load state " + newState);

                if (newState == State.SUCCEEDED) {
                    Platform.runLater(new Runnable() { // Avoid nested events
                        @Override public void run() {
                            new PrintManager().print(browser.webEngine, request);
                        }
                    });
                }
            });

        try {

            String content = request.getString("content");
            String contentType = request.getString("contentType");
    
            logger.info("printing " + 
                content.length() + " bytes of " + contentType);

            browser.webEngine.loadContent(content, contentType);

        } catch (JSONException je) {
            // RequestHandler already confirmed 'content' and 'contentType'
            // values exist.  No exceptions should occur here.
        }
    }


    /**
     * Hatch main.
     *
     */
    public static void main(String[] args) throws Exception {
        new RequestHandler().start(); // start the STDIO handlers.
        launch(args);   // launch the FX Application thread
    }
}
