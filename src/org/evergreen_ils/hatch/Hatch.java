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

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.xml.XmlConfiguration;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.scene.transform.Scale;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.concurrent.WorkerStateEvent;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.jetty.util.ajax.JSON;

import java.util.Map;

import java.io.FileInputStream;

public class Hatch extends Application {

    private BrowserView browser;
    private Stage primaryStage;
    static final Logger logger = Log.getLogger("Hatch");

    private static LinkedBlockingQueue<Map> requestQueue =
        new LinkedBlockingQueue<Map>();

    /**
     * Printable region containing a browser
     */
    class BrowserView extends Region {
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        public BrowserView() {
            getChildren().add(webView);
        }
    }

    /**
     * Service task which listens for inbound messages from the
     * servlet.
     *
     * The code blocks on the concurrent queue, so it must be
     * run in a separate thread to avoid locking the main FX thread.
     */
    private static class MsgListenService extends Service<Map<String,Object>> {
        protected Task<Map<String,Object>> createTask() {
            return new Task<Map<String,Object>>() {
                protected Map<String,Object> call() {
                    while (true) {
                        try {
                            // take() blocks until a message is available
                            return requestQueue.take();
                        } catch (InterruptedException e) {
                            // interrupted, go back and listen
                            continue;
                        }
                    }
                }
            };
        }
    }


    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        logger.debug("start()");
        startMsgTask();
    }

    public static void enqueueMessage(Map<String,Object> params) {
        logger.debug("queueing print message");
        requestQueue.offer(params);
    }

    /**
     * Build a browser view from the print content, tell the
     * browser to print itself.
     */
    private void handlePrint(Map<String,Object> params) {
        String content = (String) params.get("content");
        String contentType = (String) params.get("contentType");

        browser = new BrowserView();
        Scene scene = new Scene(browser, 640, 480); // TODO: printer dimensions
        primaryStage.setScene(scene);

        browser.webEngine.getLoadWorker()
            .stateProperty()
            .addListener( (ChangeListener) (obsValue, oldState, newState) -> {
                if (newState == State.SUCCEEDED) {
                    logger.debug("browser page loaded");
                    new PrintManager().print(browser.webEngine, params);
                }
            });

        logger.info("printing " + content.length() + " bytes of " + contentType);
        browser.webEngine.loadContent(content, contentType);
    }

    /**
     * Fire off the Service task, which checks for queued messages.
     *
     * When a queued message is found, it's analyzed and passed off
     * to the correct message handler
     */
    public void startMsgTask() {

        MsgListenService service = new MsgListenService();

        service.setOnSucceeded(
            new EventHandler<WorkerStateEvent>() {

            @Override
            public void handle(WorkerStateEvent t) {
                Map<String,Object> message =
                    (Map<String,Object>) t.getSource().getValue();

                if (message != null) handlePrint(message);

                // once this task is complete, kick off the next
                // message task handler.
                startMsgTask();
            }
        });

        service.start();
    }

    public static void main(String[] args) throws Exception {

        // build a server from our hatch.xml configuration file
        XmlConfiguration configuration =
            new XmlConfiguration(new FileInputStream("hatch.xml"));

        Server server = (Server) configuration.configure();

        // start our server, but do not join(), since we want to server
        // to continue running in its own thread
        server.start();

        // launch the FX Application thread
        launch(args);
    }
}