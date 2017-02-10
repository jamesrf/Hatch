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

// printing
import javafx.print.*;
import javafx.scene.web.WebEngine;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.OrientationRequested;

import java.lang.IllegalArgumentException;

// data structures
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedList;

import java.util.logging.Logger;

import org.json.*;

public class PrintManager {

    /** Our logger instance */
   static final Logger logger = Logger.getLogger("org.evergreen_ils.hatch");

    /**
     * Returns all known Printer's.
     *
     * @return Array of all printers
     */
    protected Printer[] getPrinters() {
        ObservableSet<Printer> printerObserver = Printer.getAllPrinters();

        if (printerObserver == null) return new Printer[0];

        return (Printer[]) printerObserver.toArray(new Printer[0]);
    }

    /**
     * Returns a list of all known printers, with their attributes 
     * encoded as a simple key/value Map.
     *
     * @return Map of printer information.
     */
    protected List<Map<String,Object>> getPrintersAsMaps() {
        Printer[] printers = getPrinters();

        List<Map<String,Object>> printerMaps = 
            new LinkedList<Map<String,Object>>();

        Printer defaultPrinter = Printer.getDefaultPrinter();

        for (Printer printer : printers) {
            HashMap<String, Object> printerMap = new HashMap<String, Object>();
            printerMaps.add(printerMap);
            printerMap.put("name", printer.getName());
            if (defaultPrinter != null && 
                printer.getName().equals(defaultPrinter.getName())) {
                printerMap.put("is-default", new Boolean(true));
            }
        }

        return printerMaps;
    }


    /**
     * Returns the Printer with the specified name.
     *
     * @param name The printer name
     * @return The printer whose name matches the provided name, or null
     * if no such printer is found.
     */
    protected Printer getPrinterByName(String name) {
        Printer[] printers = getPrinters();
        for (Printer printer : printers) {
            if (printer.getName().equals(name))
                return printer;
        }
        return null;
    }


    /**
     * Print the requested page using the provided settings
     *
     * @param engine The WebEngine instance to print
     * @param params Print request parameters
     */
    public void print(WebEngine engine, JSONObject request) {

        JSONObject response = new JSONObject();
        response.put("status", 200);
        response.put("message", "OK");

        try {
            response.put("clientid", request.getLong("clientid"));
            response.put("msgid", request.getLong("msgid"));

            boolean showDialog = request.optBoolean("showDialog");

            // if no "settings" are applied, use defaults.
            JSONObject settings = request.optJSONObject("settings");
            if (settings == null) settings = new JSONObject();

            PrinterJob job = buildPrinterJob(settings);

            if (showDialog) {
                if (!job.showPrintDialog(null)) {
                    job.endJob(); // canceled by user
                    response.put("status", 200);
                    response.put("message", "Print job canceled by user");
                    RequestHandler.reply(response);
                    return;
                }
            }

            engine.print(job);
            job.endJob();
            response.put("message", "Print job queued");
            // TODO: support watching the print job until it completes

        } catch (JSONException je) {

            String error = "JSON request protocol error: " 
                + je.toString() + " : " + request.toString();

            logger.warning(error);
            response.put("status", 400);
            response.put("message", error);

        } catch(IllegalArgumentException iae) {

            String error = "Illegal argument in print request: "
                + iae.toString() + " : " + request.toString();

            logger.warning(error);
            response.put("status", 400);
            response.put("message", error);
        }

        RequestHandler.reply(response);
    }

    /**
     * Constructs a PrinterJob based on the provided settings.
     *
     * @param settings The printer configuration Map.
     * @return The newly created printer job.
     */
    public PrinterJob buildPrinterJob(
        JSONObject settings) throws IllegalArgumentException {

        Printer printer;
        if (settings.has("printer")) {
            String name = settings.getString("printer");
            printer = getPrinterByName(name);
            if (printer == null) 
                throw new IllegalArgumentException("No such printer: " + name);

        } else {
            printer = Printer.getDefaultPrinter();
            if (printer == null) 
                throw new IllegalArgumentException(
                    "No printer specified; no default printer is set");
        }

        PageLayout layout = buildPageLayout(settings, printer);
        PrinterJob job = PrinterJob.createPrinterJob(printer);

        if (layout != null) job.getJobSettings().setPageLayout(layout);

        // apply any provided settings to the job
        applySettingsToJob(settings, job);

        return job;
    }

    /**
     * Builds a PageLayout for the requested printer, using the
     * provided settings.
     *
     * @param settings The printer configuration settings
     * @param printer The printer from which to spawn the PageLayout
     * @return The newly constructed PageLayout object.
     */
    protected PageLayout buildPageLayout(
        JSONObject settings, Printer printer) {

        PrinterAttributes printerAttrs = printer.getPrinterAttributes();

        // Start with default page layout options, replace where possible.
        Paper paper = printerAttrs.getDefaultPaper();
        PageOrientation orientation = printerAttrs.getDefaultPageOrientation();

        String paperName = settings.optString("paper", null);
        String orientationName = settings.optString("pageOrientation", null);
        String marginName = settings.optString("marginType", null);

        if (paperName != null && !paperName.isEmpty()) {
            for (Paper source : printerAttrs.getSupportedPapers()) {
                if (source.getName().equals(paperName)) {
                    logger.finer("Found matching paper: " + paperName);
                    paper = source;
                    break;
                }
            }
        }

        if (orientationName != null && !orientationName.isEmpty()) {
            orientation = PageOrientation.valueOf(orientationName);
        }

        if (settings.optBoolean("autoMargins", true)) {
            // Using a pre-defined, automatic margin option

            Printer.MarginType margin = Printer.MarginType.DEFAULT;
            if (marginName != null && !marginName.isEmpty()) {
                // An auto-margin type has been specified
                for (Printer.MarginType marg : Printer.MarginType.values()) {
                    if (marg.toString().equals(marginName)) {
                        logger.finer("Found matching margin: " + marginName);
                        margin = marg;
                        break;
                    }
                }
            }

            return printer.createPageLayout(paper, orientation, margin);
        } 

        // Using manual margins
        // Any un-specified margins default to 54 == 0.75 inches.
        return printer.createPageLayout(
            paper, orientation, 
            settings.optDouble("leftMargin", 54),
            settings.optDouble("rightMargin", 54),
            settings.optDouble("topMargin", 54),
            settings.optDouble("bottomMargin", 54)
        );
    }

    /**
     * Applies the provided settings to the PrinterJob.
     *
     * @param settings The printer configuration settings map.
     * @param job A PrinterJob, constructed from buildPrinterJob()
     */
    protected void applySettingsToJob(JSONObject settings, PrinterJob job) {

        JobSettings jobSettings = job.getJobSettings();

        PrinterAttributes printerAttrs = 
            job.getPrinter().getPrinterAttributes();

        String collation = settings.optString("collation", null);
        if (collation != null && !collation.isEmpty()) {
            jobSettings.setCollation(Collation.valueOf(collation));
        }

        int copies = settings.optInt("copies");
        if (copies > 0) {
            jobSettings.setCopies(copies);
        }

        String printColor = settings.optString("printColor", null);
        if (printColor != null && !printColor.isEmpty()) {
            jobSettings.setPrintColor(PrintColor.valueOf(printColor));
        }

        String printQuality = settings.optString("printQuality", null);
        if (printQuality != null && !printQuality.isEmpty()) {
            jobSettings.setPrintQuality(PrintQuality.valueOf(printQuality));
        }

        String printSides = settings.optString("printColor", null);
        if (printSides != null && !printSides.isEmpty()) {
            jobSettings.setPrintSides(PrintSides.valueOf(printSides));
        }

        String paperSource = settings.optString("paperSource");

        if (paperSource != null && !paperSource.isEmpty()) {
            for (PaperSource source : printerAttrs.getSupportedPaperSources()) {
                if (source.getName().equals(paperSource)) {
                    logger.finer("Found paper source: " + paperSource);
                    jobSettings.setPaperSource(source);
                    break;
                }
            }
        }

        if (!settings.optBoolean("allPages", true)) {
            JSONArray pageRanges = settings.optJSONArray("pageRanges");

            if (pageRanges != null) {
                List<PageRange> builtRanges = new LinkedList<PageRange>();
                int i = 0, start = 0, end = 0;
                do {
                    if (i % 2 == 0 && i > 0)
                        builtRanges.add(new PageRange(start, end));

                    if (i == pageRanges.length()) break;

                    int current = pageRanges.getInt(i);
                    if (i % 2 == 0) start = current; else end = current;

                } while (++i > 0);

                jobSettings.setPageRanges(
                    builtRanges.toArray(new PageRange[0]));
            }
        }
    }

    public JSONObject getPrintersOptions(String printerName) {
        Printer printer;

        if (printerName == null) { // no name provided, use default.
            printer = Printer.getDefaultPrinter();
        } else {
            printer = getPrinterByName(printerName);
        }

        if (printer == null) return null;

        JSONObject options = new JSONObject();
        PrinterAttributes printerAttrs = printer.getPrinterAttributes();

        JSONArray papersArray = new JSONArray();
        for (Paper source : printerAttrs.getSupportedPapers()) {
            papersArray.put(source.getName());
        }
        options.put("paper", papersArray);
        options.put("defaultPaper", 
            printerAttrs.getDefaultPaper().getName());

        JSONArray paperSourcesArray = new JSONArray();
        for (PaperSource source : 
            printerAttrs.getSupportedPaperSources()) {
            paperSourcesArray.put(source.getName());
        }
        options.put("paperSource", paperSourcesArray);
        options.put("defaultPaperSource", 
            printerAttrs.getDefaultPaperSource().getName());

        JSONArray collationsArray = new JSONArray();
        for (Collation collation : 
            printerAttrs.getSupportedCollations()) {
            collationsArray.put(collation.toString());
        }
        options.put("collation", collationsArray);
        options.put("defaultCollation", 
            printerAttrs.getDefaultCollation().toString());

        JSONArray colorsArray = new JSONArray();
        for (PrintColor color : 
            printerAttrs.getSupportedPrintColors()) {
            colorsArray.put(color.toString());
        }
        options.put("printColor", colorsArray);
        options.put("defaultPrintColor", 
            printerAttrs.getDefaultPrintColor().toString());

        JSONArray qualityArray = new JSONArray();
        for (PrintQuality quality : 
            printerAttrs.getSupportedPrintQuality()) {
            qualityArray.put(quality.toString());
        }
        options.put("printQuality", qualityArray);
        options.put("defaultPrintQuality", 
            printerAttrs.getDefaultPrintQuality().toString());

        JSONArray sidesArray = new JSONArray();
        for (PrintSides side : printerAttrs.getSupportedPrintSides()) {
            sidesArray.put(side.toString());
        }
        options.put("printSides", sidesArray);
        options.put("defaultPrintSides", 
            printerAttrs.getDefaultPrintSides().toString());

        JSONArray orientsArray = new JSONArray();
        for (PageOrientation orient : 
            printerAttrs.getSupportedPageOrientations()) {
            orientsArray.put(orient.toString());
        }
        options.put("pageOrientation", orientsArray);
        options.put("defaultPageOrientation", 
            printerAttrs.getDefaultPageOrientation().toString());

        JSONArray marginsArray = new JSONArray();
        for (Printer.MarginType margin : Printer.MarginType.values()) {
            marginsArray.put(margin.toString());
        }
        options.put("marginType", marginsArray);
        options.put("defaultMarginType", Printer.MarginType.DEFAULT);

        options.put("supportsPageRanges", printerAttrs.supportsPageRanges());
        options.put("defaultCopies", printerAttrs.getDefaultCopies());

        return options;
    }

}

