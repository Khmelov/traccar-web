/*
 * Copyright 2015 Vitaly Litvak (vitavaque@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.web.server.reports;

import org.traccar.web.client.model.DataService;
import org.traccar.web.server.model.ServerMessages;
import org.traccar.web.shared.model.*;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class ReportGenerator {
    @Inject
    EntityManager entityManager;

    @Inject
    User currentUser;

    @Inject
    HttpServletRequest request;

    @Inject
    HttpServletResponse response;

    @Inject
    DataService dataService;

    @Inject
    ServerMessages messages;

    @Inject
    ApplicationSettings applicationSettings;

    private ReportRenderer renderer;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    abstract void generateImpl(Report report) throws IOException;

    public final void generate(Report report) throws IOException {
        renderer = new ReportRenderer(response);
        renderer.start(report);
        generateImpl(report);
        renderer.end(report);
    }

    void h1(String text) {
        renderer.h1(text);
    }

    void h2(String text) {
        renderer.h2(text);
    }

    void h3(String text) {
        renderer.h3(text);
    }

    public void tableRowStart() {
        renderer.tableRowStart();
    }

    public void paragraphEnd() {
        renderer.paragraphEnd();
    }

    public void tableRowEnd() {
        renderer.tableRowEnd();
    }

    public void tableBodyEnd() {
        renderer.tableBodyEnd();
    }

    public void tableStart() {
        renderer.tableStart(null);
    }

    public void tableStart(ReportRenderer.TableStyle style) {
        renderer.tableStart(style);
    }

    ReportRenderer.TableStyle hover() {
        return new ReportRenderer.TableStyle().hover();
    }

    ReportRenderer.TableStyle condensed() {
        return new ReportRenderer.TableStyle().condensed();
    }

    public void tableHeadStart() {
        renderer.tableHeadStart();
    }

    public void tableHeadEnd() {
        renderer.tableHeadEnd();
    }

    public void tableHeadCellStart() {
        renderer.tableHeadCellStart(null);
    }

    public void tableHeadCellStart(ReportRenderer.CellStyle style) {
        renderer.tableHeadCellStart(style);
    }

    public void tableHeadCellEnd() {
        renderer.tableHeadCellEnd();
    }

    public void panelBodyStart() {
        renderer.panelBodyStart();
    }

    public void panelBodyEnd() {
        renderer.panelBodyEnd();
    }

    public void tableCellEnd() {
        renderer.tableCellEnd();
    }

    public void panelStart() {
        renderer.panelStart();
    }

    public void panelHeadingEnd() {
        renderer.panelHeadingEnd();
    }

    public void text(String text) {
        renderer.text(text);
    }

    public void tableEnd() {
        renderer.tableEnd();
    }

    public void panelEnd() {
        renderer.panelEnd();
    }

    public void panelHeadingStart() {
        renderer.panelHeadingStart();
    }

    public void tableBodyStart() {
        renderer.tableBodyStart();
    }

    public void paragraphStart() {
        renderer.paragraphStart();
    }

    public void bold(String text) {
        renderer.bold(text);
    }

    public void tableCellStart() {
        renderer.tableCellStart(null);
    }

    void tableCell(String text) {
        tableCellStart();
        text(text);
        tableCellEnd();
    }

    public void tableCellStart(ReportRenderer.CellStyle style) {
        renderer.tableCellStart(style);
    }

    public void link(String url, String target, String text) {
        renderer.link(url, target, text);
    }

    void mapLink(double latitude, double longitude) {
        UserSettings userSettings = currentUser.getUserSettings();
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        DecimalFormat lonLatFormat = new DecimalFormat("##.######", symbols);
        String text = lonLatFormat.format(latitude) + " \u00B0, " +
                lonLatFormat.format(longitude) + " \u00B0";

        switch (userSettings.getMapType()) {
            case GOOGLE_HYBRID:
            case GOOGLE_NORMAL:
            case GOOGLE_SATELLITE:
            case GOOGLE_TERRAIN: {
                link("https://maps.google.com/maps?q=" + lonLatFormat.format(latitude) + "," + lonLatFormat.format(longitude) + "&t=m",
                        "_blank", text);
                break;
            }
            default: {
                link("http://www.openstreetmap.org/?" +
                                "mlat=" + lonLatFormat.format(latitude) + "&mlon=" + lonLatFormat.format(longitude) +
                                "#map=" + userSettings.getZoomLevel() + "/" +
                                lonLatFormat.format(latitude) + "/" + lonLatFormat.format(longitude),
                        "_blank", text);
                break;
            }
        }
    }

    ReportRenderer.CellStyle colspan(int colspan) {
        return new ReportRenderer.CellStyle().colspan(colspan);
    }

    ReportRenderer.CellStyle rowspan(int rowspan) {
        return new ReportRenderer.CellStyle().rowspan(rowspan);
    }

    List<Device> getDevices(Report report) {
        if (report.getDevices().isEmpty()) {
            return dataService.getDevices();
        } else {
            List<Device> devices = new ArrayList<Device>(report.getDevices().size());
            for (Device reportDevice : report.getDevices()) {
                Device device = entityManager.find(Device.class, reportDevice.getId());
                if (currentUser.hasAccessTo(device)) {
                    devices.add(device);
                }
            }
            return devices;
        }
    }

    String formatDuration(long duration) {
        if (duration == 0) {
            return "0s";
        }

        int days = (int) (duration / 86400000L);
        duration -= (long) days * 86400000L;

        int hours = (int) (duration / 3600000L);
        duration -= (long) hours * 3600000L;

        int minutes = (int) (duration / 60000L);
        duration -= (long) minutes * 60000L;

        int seconds = (int) (duration / 1000L);

        return
                (days == 0 ? "" : days + message("day") + " ") +
                        (hours == 0 ? "" : hours + message("hour") + " ") +
                        (minutes == 0 ? "" : minutes + message("minute") + " ") +
                        (seconds == 0 ? "" : seconds + message("second") + " ");
    }

    String formatSpeed(double speed) {
        UserSettings.SpeedUnit speedUnit = currentUser.getUserSettings().getSpeedUnit();
        NumberFormat speedFormat = NumberFormat.getInstance();
        speedFormat.setMaximumFractionDigits(2);
        speedFormat.setMinimumIntegerDigits(0);
        return speedFormat.format((Double.isNaN(speed) ? 0d : speed) * speedUnit.getFactor()) + " " + speedUnit.getUnit();
    }

    String formatDistance(double distance) {
        UserSettings.SpeedUnit speedUnit = currentUser.getUserSettings().getSpeedUnit();
        UserSettings.DistanceUnit distanceUnit = speedUnit.getDistanceUnit();
        NumberFormat distanceFormat = NumberFormat.getInstance();
        distanceFormat.setMaximumFractionDigits(2);
        distanceFormat.setMinimumIntegerDigits(0);
        distanceFormat.setMinimumIntegerDigits(1);
        return distanceFormat.format((Double.isNaN(distance) ? 0d : distance) * distanceUnit.getFactor()) + " " + distanceUnit.getUnit();
    }

    String formatDate(Date date) {
        return dateFormat.format(date);
    }

    String message(String key) {
        String locale = request.getParameter("locale");
        if (locale == null) {
            locale = applicationSettings.getLanguage();
        }
        return messages.message(locale, key);
    }
}
