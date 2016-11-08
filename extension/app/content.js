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
 *
 * Hatch Content Script.
 *
 * Relays messages between the browser tab and the Hatch extension.js
 * script.
 */

console.debug('Loading Hatch relay content script');

// Tell the page DOM we're here.
document.body.setAttribute('hatch-is-open', '4-8-15-16-23-42');

/**
 * Open a port to our extension.
 */
var port = chrome.runtime.connect();

/**
 * Relay all messages received from the extension back to the tab
 */
port.onMessage.addListener(function(message) {

    /*
    console.debug(
        "Content script received from extension: "+ JSON.stringify(message));
    */

    window.postMessage(message, location.origin);
});


/**
 * Receive messages from the browser tab and relay them to the
 * Hatch extension script.
 */
window.addEventListener("message", function(event) {

    // We only accept messages from ourselves
    if (event.source != window) return;

    var message = event.data;

    // Ignore broadcast messages.  We only care about messages
    // received from our browser tab/page.
    if (message.from != 'page') return;

    /* 
    console.debug(
        "Content script received from page: " + JSON.stringify(message));
    */

    // standard Hatch-bound message; relay to extension.
    port.postMessage(message);

}, false);



