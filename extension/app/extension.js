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

// Singleton connection to Hatch
var hatchPort = null;

// Map of tab identifers to tab-specific connection ports.
var browserPorts = {};
 
const HATCH_EXT_NAME = 'org.evergreen_ils.hatch';
const HATCH_RECONNECT_TIME = 5; // seconds

var hatchHostUnavailable = false;
function connectToHatch() {
    console.debug("Connecting to native messaging host: " + HATCH_EXT_NAME);
    try {
        hatchPort = chrome.runtime.connectNative(HATCH_EXT_NAME);
        hatchPort.onMessage.addListener(onNativeMessage);
        hatchPort.onDisconnect.addListener(onDisconnected);
    } catch (E) {
        console.warn("Hatch host connection failed: " + E);
        hatchHostUnavailable = true;
    }
}

/**
 * Called when the connection to Hatch goes away.
 */
function onDisconnected() {
  console.warn("Hatch disconnected: " + chrome.runtime.lastError.message);
  hatchPort = null;

  if (hatchHostUnavailable) return;

  // If we can reasonablly assume a connection to the Hatch host 
  // is possible, attempt to reconnect after a failure.
  setTimeout(connectToHatch, HATCH_RECONNECT_TIME  * 1000); 

  console.debug("Reconnecting to Hatch after connection failure in " +
    HATCH_RECONNECT_TIME + " seconds...");
}


/**
 * Handle response messages received from Hatch.
 */
function onNativeMessage(message) {
    var tabId = message.clientid;

    if (tabId && browserPorts[tabId]) {
        message.from = 'extension';
        browserPorts[tabId].postMessage(message);
    } else {
        // if browserPorts[tabId] is empty, it generally means the
        // user navigated away before receiving the response. 
    }
}


/**
 * Called when our content script opens connection to this extension.
 */
chrome.runtime.onConnect.addListener(function(port) {
    var tabId = port.sender.tab.id;

    browserPorts[tabId] = port;
    console.debug('new port connected with id ' + tabId);

    port.onMessage.addListener(function(msg) {
        console.debug("Received message from browser on port " + tabId);

        if (!hatchPort) {
            // TODO: we could queue failed messages for redelivery
            // after a reconnect.  Not sure yet if that level of 
            // sophistication is necessary.
            console.debug("Cannot send message " + 
                msg.msgid + " - no Hatch connection present");
            return;
        }

        // tag the message with the browser tab ID for response routing.
        msg.clientid = tabId;

        // Stamp the origin (protocol + host) on every request.
        msg.origin = port.sender.url.match(/https?:\/\/[^\/]+/)[0];

        hatchPort.postMessage(msg);
    });

    port.onDisconnect.addListener(function() {
        console.log("Removing port " + tabId + " on tab disconnect");
        delete browserPorts[tabId];
    });
});


function setPageActionRules() {
    // Replace all rules on extension reload
    chrome.declarativeContent.onPageChanged.removeRules(undefined, function() {
        chrome.declarativeContent.onPageChanged.addRules([
            {
                conditions: [
                    new chrome.declarativeContent.PageStateMatcher({
                        pageUrl : {
                            pathPrefix : '/eg/staff/',
                            schemes : ['https']
                        },
                        css: ["eg-navbar"] // match on <eg-navbar/>
                    })
                ],
                actions: [ 
                    new chrome.declarativeContent.RequestContentScript({
                        'js': ['content.js']
                    })
                ]
            }
        ]);
    });
}

chrome.browserAction.onClicked.addListener(function (tab) {
    chrome.permissions.request({
        origins: ['https://*/eg/staff/*']
    }, function (ok) {
        if (ok) {
            console.log('access granted');
        } else if (chrome.runtime.lastError) {
            alert('Permission Error: ' + chrome.runtime.lastError.message);
        } else {
            alert('Optional permission denied.');
        }
    });
});


// Link the page action icon to loading the content script
chrome.runtime.onInstalled.addListener(setPageActionRules);

// Connect to Hatch on startup.
connectToHatch();

