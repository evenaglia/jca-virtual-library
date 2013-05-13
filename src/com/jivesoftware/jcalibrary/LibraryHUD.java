package com.jivesoftware.jcalibrary;

import com.jivesoftware.jcalibrary.api.rest.CustomerInfo;
import com.jivesoftware.jcalibrary.api.rest.CustomerInstallation;
import com.jivesoftware.jcalibrary.api.rest.CustomerType;
import com.jivesoftware.jcalibrary.api.rest.InstallationType;
import com.jivesoftware.jcalibrary.objects.VisualObjects;
import com.jivesoftware.jcalibrary.scheduler.WorkScheduler;
import com.jivesoftware.jcalibrary.structures.JiveInstance;
import com.jivesoftware.jcalibrary.structures.JiveInstanceState;
import com.jivesoftware.jcalibrary.structures.ServerRack;
import com.jivesoftware.jcalibrary.structures.ServerSlot;
import net.venaglia.realms.common.navigation.Position;
import org.apache.commons.io.IOUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: ed
 * Date: 5/12/13
 * Time: 11:37 AM
 */
public class LibraryHUD {

    private static final Polygon CAMERA = new Polygon(
            new int[]{ 0, 3, 0, -3 },
            new int[]{ 6, -6, -3, -6 },
            4
    );
    private static final Pattern MATCH_REPORT_TOKEN = Pattern.compile("\\$\\{(\\w+\\.\\w+)*}");

    private int radius = 384;
//    private int radius = 192;
    private JFrame frame;
    private JPanel libraryHotspots;
    private JTextArea activeInstanceDetail;
    private JiveInstance jiveInstance;
    private double cameraX, cameraY, cameraA;
    private AtomicReference<Collection<PointData>> data = new AtomicReference<Collection<PointData>>();
    private String reportTemplate;

    public LibraryHUD(final ServerRack[] serverRacks) {
        this.frame = new JFrame("Overview");
        this.libraryHotspots = new JPanel() {
            @Override
            public void paint(Graphics graphics) {
                Graphics2D g = (Graphics2D)graphics;
                int width = frame.getWidth();
                int height = frame.getHeight();
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, width, height);
                g.translate(width / 2, height / 2 - 10);
                g.scale(radius / -20.f, radius / 20.f);
                drawLibrary(g);
                drawCamera(g);
                drawInstances(g);
            }
        };
        this.activeInstanceDetail = new JTextArea();
        Font font = getFont();
        this.activeInstanceDetail.setFont(font);
        this.frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                radius = Math.round(Math.min(frame.getHeight(), frame.getWidth()) * 0.45f);
            }
        });
        this.frame.add(libraryHotspots);
        this.frame.setSize(radius << 1, radius << 1);
        this.frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.reportTemplate = readTemplate();

        WorkScheduler.interval(new Runnable() {

            private final int dataSize = 4096;
            private PointData[][] pointData = {
                    new PointData[dataSize],
                    new PointData[dataSize],
                    new PointData[dataSize]
            };
            private int writeData = 0;

            {
                for (PointData[] data : pointData) {
                    for (int i = 0; i < data.length; i++) {
                        data[i] = new PointData();
                        data[i] = new PointData();
                    }
                }
            }

            public void run() {
                int i = 0;
                PointData[] allData = pointData[writeData];
                double[] xys = {0,0,1};
                long now = System.currentTimeMillis();
                for (ServerRack serverRack : serverRacks) {
                    for (ServerSlot slot : serverRack.getSlots()) {
                        JiveInstance jiveInstance = slot.getJiveInstance();
                        if (jiveInstance != null) {
                            jiveInstance.checkAndClearDirty();
                            PointData data = allData[i++];
                            slot.getSlotTransformation().getXYScale(xys);
                            data.x = Math.round((float)xys[0] * 100.0f);
                            data.y = Math.round((float)xys[1] * 100.0f);
                            data.scale = (float)xys[2];
                            JiveInstanceState state = VisualObjects.OVERALL_STATE.get(jiveInstance);
                            data.state = state;
                            net.venaglia.realms.common.physical.decorators.Color color = state.getColor(now);
                            data.rgb = toInt(color.r,16) | toInt(color.g,8) | toInt(color.b,0);
                        }
                    }
                }
                data.set(Arrays.asList(allData).subList(0,i));
                writeData = (writeData + 1) % pointData.length;
            }

            private int toInt(float f, int bits) {
                return Math.min(255, Math.max(0, Math.round(f * 255))) << bits;
            }
        }, 33, TimeUnit.MILLISECONDS);
    }

    private Font getFont() {
        try {
            URL fontURL = Thread.currentThread().getContextClassLoader().getResource("fonts/DroidSansMono.ttf");
            return Font.createFont(Font.TRUETYPE_FONT, fontURL.openStream()).deriveFont(12.0f);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String readTemplate() {
        try {
            return IOUtils.toString(getClass().getResourceAsStream("InstanceReport.txt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void drawLibrary(Graphics2D g) {
        g.setColor(Color.CYAN);
        g.setStroke(new BasicStroke(0.125f));
        g.drawOval(-20, -20, 40, 40);
        g.drawOval(-14, -14, 28, 28);
        g.drawOval(-4,-4,8,8);
    }

    private void drawCamera(Graphics2D g) {
        AffineTransform transform = g.getTransform();
        AffineTransform copy = new AffineTransform(transform);
        copy.translate(cameraX, cameraY);
        copy.rotate(cameraA);
        copy.scale(0.1, 0.1);
        g.setTransform(copy);
        g.setColor(Color.WHITE);
        g.fill(CAMERA);
        g.setTransform(transform);
    }

    private void drawInstances(Graphics2D g) {
        AffineTransform transform = g.getTransform();
        Collection<PointData> allData = this.data.get();
        if (allData == null || allData.isEmpty()) {
            return;
        }
        g.scale(0.01,0.01);
        PointData lastData = null;
        for (PointData data : allData) {
            if (data.state != JiveInstanceState.EMPTY_SLOT) {
                if (!data.colorEquals(lastData)) {
                    g.setColor(new Color(data.rgb));
                }
                int r = Math.round(data.scale * 15.0f);
                g.fillOval(data.x - r, data.y - r, r << 1, r << 1);
            }
        }
        g.setTransform(transform);
    }

    public void updateCamera(Position position) {
        cameraX = position.cameraX;
        cameraY = position.cameraY;
        cameraA = position.heading;
        libraryHotspots.repaint();
    }

    public void show() {
        this.frame.setVisible(true);
    }

    public void showJiveInstanceData(final JiveInstance jiveInstance) {
        WorkScheduler.now(new Runnable() {
            @Override
            public void run() {
                if (LibraryHUD.this.jiveInstance == jiveInstance) {
                    return;
                }
                String instanceReport = generateReport(jiveInstance);
                activeInstanceDetail.setText(instanceReport);
                LibraryHUD.this.jiveInstance = jiveInstance;
                if (jiveInstance == null) {
                    frame.remove(activeInstanceDetail);
                    frame.add(libraryHotspots);
                } else if (LibraryHUD.this.jiveInstance == null) {
                    frame.remove(libraryHotspots);
                    frame.add(activeInstanceDetail);
                }
            }
        });
    }

    private String generateReport(JiveInstance jiveInstance) {
        if (jiveInstance == null) {
            return "";
        }
        Map<String,String> values = new HashMap<String,String>();
        values.put("customerInstallationId", String.valueOf(jiveInstance.getCustomerInstallationId()));
        values.put("pageViews", String.valueOf(jiveInstance.getPageViews()));
        CustomerInfo customer = jiveInstance.getCustomer();
        if (customer != null) {
            values.put("customer.customerId", String.valueOf(customer.getCustomerId()));
            values.put("customer.name", customer.getName());
            values.put("customer.accountKey", customer.getAccountKey());
            values.put("customer.jiveContactEmail", customer.getJiveContactEmail());
            CustomerType customerType = customer.getCustomerType();
            if (customerType != null) {
                values.put("customer.customerType.code", customerType.getCode());
            }
            values.put("customer.notes", customer.getNotes());
            values.put("customer.deployDate", toString(customer.getDeployDate()));
            values.put("customer.domain", customer.getDomain());
        }
        CustomerInstallation installation = jiveInstance.getInstallation();
        if (installation != null) {
            values.put("installation.customerInstallationId", String.valueOf(installation.getCustomerInstallationId()));
            values.put("installation.installationName", installation.getInstallationName());
            values.put("installation.installationDescription", installation.getInstallationDescription());
            InstallationType installationType = installation.getInstallationType();
            if (installationType != null) {
                values.put("installation.installationType.typeCode", installationType.getTypeCode());
                values.put("installation.installationType.description", installationType.getDescription());
            }
            values.put("installation.installationUrl", installation.getInstallationUrl());
            values.put("installation.analyticsDbDir", installation.getAnalyticsDbDir());
            values.put("installation.systemDbDir", installation.getSystemDbDir());
            values.put("installation.eaeDbDir", installation.getEaeDbDir());
            values.put("installation.cloneStatus", installation.getCloneStatus());
            values.put("installation.notes", installation.getNotes());
            values.put("installation.ssoNotes", installation.getSsoNotes());
            values.put("installation.supportalKey", installation.getSupportalKey());
            values.put("installation.sforceContract", installation.getSforceContract());
            values.put("installation.costActual", toString(installation.getCostActual()));
            values.put("installation.costBilled", toString(installation.getCostBilled()));
            values.put("installation.version", installation.getVersion());
            values.put("installation.inMaintenanceMode", toString(installation.isInMaintenanceMode()));
            values.put("installation.active", toString(installation.isActive()));
            values.put("installation.additionalRestart", toString(installation.isAdditionalRestart()));
            values.put("installation.hasVPN", toString(installation.isHasVPN()));
            values.put("installation.hasLDAP", toString(installation.isHasLDAP()));
            values.put("installation.hasLogStreaming", toString(installation.isHasLogStreaming()));
            values.put("installation.installationSystemName", installation.getInstallationSystemName());
            values.put("installation.seed", toString(installation.isSeed()));
            values.put("installation.jiveInstanceId", installation.getJiveInstanceId());
            values.put("installation.zenossCollector", installation.getZenossCollector());
            values.put("installation.zenossMaster", installation.getZenossMaster());
        }
        StringBuffer buffer = new StringBuffer(reportTemplate.length() + 1024);
        Matcher matcher = MATCH_REPORT_TOKEN.matcher(reportTemplate);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = values.get(key);
            matcher.appendReplacement(buffer, "");
            if (value == null) {
                buffer.append("[n/a]");
            } else {
                buffer.append(value);
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String toString (Object obj) {
        return obj == null ? null : obj.toString();
    }

    private static class PointData implements Comparable<PointData> {
        int x,y;
        float scale;
        int rgb;
        JiveInstanceState state = JiveInstanceState.EMPTY_SLOT;

        @Override
        public int compareTo(PointData o) {
            return state.compareTo(o.state);
        }

        public boolean colorEquals(PointData data) {
            return data != null && data.rgb == rgb;
        }

        @Override
        public String toString() {
            return String.format("PointData(%.2f,%.2f,%6x)", x / 100.0f, y / 100.0f, rgb);
        }
    }
}
