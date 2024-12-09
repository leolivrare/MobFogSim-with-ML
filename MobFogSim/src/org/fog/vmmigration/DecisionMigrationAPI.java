package org.fog.vmmigration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.entities.ApDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.localization.DiscoverLocalization;
import org.fog.localization.Distances;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DecisionMigrationAPI implements DecisionMigration {

    private List<FogDevice> serverCloudlets;
    private List<ApDevice> apDevices;
    private int migPointPolicy;
    private ApDevice currentAP;
    private int nextApId;
    private int nextServerCloudletId;
    private int policyReplicaVM;

    private int smartThingPosition;
    private boolean migZone;
    private boolean migPoint;

    public DecisionMigrationAPI(List<FogDevice> serverCloudlets, List<ApDevice> apDevices, int migPointPolicy, int policyReplicaVM) {
        setServerCloudlets(serverCloudlets);
        setApDevices(apDevices);
        setMigPointPolicy(migPointPolicy);
        setPolicyReplicaVM(policyReplicaVM);
    }

    @Override
    public boolean shouldMigrate(MobileDevice smartThing) {
        setCurrentAP(smartThing.getSourceAp());
        setSmartThingPosition(DiscoverLocalization.discoverLocal(currentAP.getCoord(), smartThing.getCoord()));
        smartThing.getMigrationTechnique().verifyPoints(smartThing, getSmartThingPosition());

        setMigPoint(smartThing.isMigPoint());
        setMigZone(smartThing.isMigZone());

        double posX = smartThing.getCoord().getCoordX();
        double posY = smartThing.getCoord().getCoordY();
        double direction = smartThing.getDirection();

        boolean decision = callMigrationAPI(
            posX,
            posY,
            direction,
            smartThing.getSpeed(),
            Distances.checkDistance(smartThing.getSourceAp().getCoord(), smartThing.getCoord()),
            calculateDistanceToLocalCloudlet(smartThing),
            calculateDistanceToClosestCloudlet(smartThing),
            isMigPoint(),
            isMigZone()
        );

        if (!decision) {
            logMetrics(smartThing, false, "Migration rejected by API");
            return false;
        }

        setNextServerCloudletId(Migration.lowestLatencyCostServerCloudlet(serverCloudlets, apDevices, smartThing));
        if (getNextServerCloudletId() < 0) {
            logMetrics(smartThing, false, "No suitable server cloudlet");
            return false;
        }

        setNextApId(Migration.nextAp(apDevices, smartThing));
        if (getNextApId() >= 0 && !Migration.isEdgeAp(apDevices.get(getNextApId()), smartThing)) {
            logMetrics(smartThing, false, "Next AP is not an edge AP");
            return false;
        }

        smartThing.setDestinationServerCloudlet(serverCloudlets.get(getNextServerCloudletId()));
        if (getNextApId() >= 0) {
            smartThing.setDestinationAp(apDevices.get(getNextApId()));
        }

        logMetrics(smartThing, true, "Migration approved");
        return true;
    }

    private boolean callMigrationAPI(
        double posX,
        double posY,
        double direction,
        double speed,
        double distanceToSourceAp,
        double distanceToLocalCloudlet,
        double distanceToClosestCloudlet,
        boolean isMigPoint,
        boolean isMigZone
    ) {
        try {
            Map<String, Object> payload = buildPayload(
                posX, posY, direction, speed,
                distanceToSourceAp, distanceToLocalCloudlet, distanceToClosestCloudlet,
                isMigPoint, isMigZone
            );

            String jsonPayload = new ObjectMapper().writeValueAsString(payload);
            String responseStr = sendHttpRequest("http://127.0.0.1:8000/should_migrate", jsonPayload);

            if (responseStr == null) {
                System.err.println("API returned no response");
                return false;
            }

            return parseResponse(responseStr);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Map<String, Object> buildPayload(
        double posX,
        double posY,
        double direction,
        double speed,
        double distanceToSourceAp,
        double distanceToLocalCloudlet,
        double distanceToClosestCloudlet,
        boolean isMigPoint,
        boolean isMigZone
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("PosX", posX);
        payload.put("PosY", posY);
        payload.put("Direction", direction);
        payload.put("Speed", speed);
        payload.put("DistanceToSourceAp", distanceToSourceAp);
        payload.put("DistanceToLocalCloudlet", distanceToLocalCloudlet);
        payload.put("DistanceToClosestCloudlet", distanceToClosestCloudlet);
        payload.put("IsMigPoint", isMigPoint);
        payload.put("IsMigZone", isMigZone);
        return payload;
    }

    private String sendHttpRequest(String urlString, String jsonPayload) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                os.write(jsonPayload.getBytes("utf-8"));
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder responseStr = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        responseStr.append(line.trim());
                    }
                }
                return responseStr.toString();
            } else {
                System.err.println("Error calling API: HTTP " + responseCode);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean parseResponse(String responseStr) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap = objectMapper.readValue(responseStr, Map.class);
        return (boolean) responseMap.get("shouldMigrate");
    }

    private double calculateDistanceToLocalCloudlet(MobileDevice smartThing) {
        FogDevice localCloudlet = smartThing.getVmLocalServerCloudlet();
        if (localCloudlet != null) {
            return Distances.checkDistance(localCloudlet.getCoord(), smartThing.getCoord());
        }
        return -1;
    }

    private double calculateDistanceToClosestCloudlet(MobileDevice smartThing) {
        double minDistance = Double.MAX_VALUE;
        FogDevice localCloudlet = smartThing.getVmLocalServerCloudlet();

        for (FogDevice cloudlet : getServerCloudlets()) {
            if (localCloudlet != null && cloudlet.getId() == localCloudlet.getId()) {
                continue;
            }
            double distance = Distances.checkDistance(cloudlet.getCoord(), smartThing.getCoord());
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        return minDistance == Double.MAX_VALUE ? -1 : minDistance;
    }

    private void logMetrics(MobileDevice smartThing, boolean shouldMigrate, String reason) {
        double distanceToSourceAp = Distances.checkDistance(smartThing.getSourceAp().getCoord(), smartThing.getCoord());
        double migrationTime = smartThing.getMigTime();

        FogDevice localCloudlet = smartThing.getVmLocalServerCloudlet();
        String localCloudletName = (localCloudlet != null) ? localCloudlet.getName() : "None";
        double distanceToLocalCloudlet = (localCloudlet != null)
            ? Distances.checkDistance(localCloudlet.getCoord(), smartThing.getCoord())
            : -1;

        double minDistance = Double.MAX_VALUE;
        FogDevice closestCloudlet = null;
        for (FogDevice cloudlet : serverCloudlets) {
            if (localCloudlet == null || cloudlet.getId() != localCloudlet.getId()) {
                double dist = Distances.checkDistance(cloudlet.getCoord(), smartThing.getCoord());
                if (dist < minDistance) {
                    minDistance = dist;
                    closestCloudlet = cloudlet;
                }
            }
        }

        String closestCloudletName = (closestCloudlet != null) ? closestCloudlet.getName() : "None";
        double distanceToClosestCloudlet = (closestCloudlet != null) ? minDistance : -1;

        String nextServerCloudletName = (getNextServerCloudletId() > 0) ? serverCloudlets.get(getNextServerCloudletId()).getName() : "None";
        String nextApName = (getNextApId() > 0) ? apDevices.get(getNextApId()).getName() : "None";

        MyStatistics.getInstance().logMigrationMetrics(
            CloudSim.clock(),
            smartThing.getMyId(),
            smartThing.getCoord().getCoordX(),
            smartThing.getCoord().getCoordY(),
            smartThing.getDirection(),
            smartThing.getSpeed(),
            smartThing.getSourceAp().getName(),
            distanceToSourceAp,
            migrationTime,
            shouldMigrate,
            nextServerCloudletName,
            nextApName,
            reason,
            localCloudletName,
            distanceToLocalCloudlet,
            closestCloudletName,
            distanceToClosestCloudlet,
            isMigPoint(),
            isMigZone()
        );
    }

    public ApDevice getCurrentAP() {
        return currentAP;
    }

    public void setCurrentAP(ApDevice currentAP) {
        this.currentAP = currentAP;
    }

    public List<FogDevice> getServerCloudlets() {
        return serverCloudlets;
    }

    public void setServerCloudlets(List<FogDevice> serverCloudlets) {
        this.serverCloudlets = serverCloudlets;
    }

    public List<ApDevice> getApDevices() {
        return apDevices;
    }

    public void setApDevices(List<ApDevice> apDevices) {
        this.apDevices = apDevices;
    }

    public int getMigPointPolicy() {
        return migPointPolicy;
    }

    public void setMigPointPolicy(int migPointPolicy) {
        this.migPointPolicy = migPointPolicy;
    }

    public int getNextApId() {
        return nextApId;
    }

    public void setNextApId(int nextApId) {
        this.nextApId = nextApId;
    }

    public int getNextServerCloudletId() {
        return nextServerCloudletId;
    }

    public void setNextServerCloudletId(int nextServerCloudletId) {
        this.nextServerCloudletId = nextServerCloudletId;
    }

    public int getSmartThingPosition() {
        return smartThingPosition;
    }

    public void setSmartThingPosition(int smartThingPosition) {
        this.smartThingPosition = smartThingPosition;
    }

    public boolean isMigZone() {
        return migZone;
    }

    public void setMigZone(boolean migZone) {
        this.migZone = migZone;
    }

    public boolean isMigPoint() {
        return migPoint;
    }

    public void setMigPoint(boolean migPoint) {
        this.migPoint = migPoint;
    }

    public int getPolicyReplicaVM() {
        return policyReplicaVM;
    }

    public void setPolicyReplicaVM(int policyReplicaVM) {
        this.policyReplicaVM = policyReplicaVM;
    }
}
