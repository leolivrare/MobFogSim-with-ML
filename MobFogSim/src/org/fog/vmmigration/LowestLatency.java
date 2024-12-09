package org.fog.vmmigration;

import java.util.List;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.entities.ApDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.localization.DiscoverLocalization;
import org.fog.localization.Distances;

public class LowestLatency implements DecisionMigration {

    private List<FogDevice> serverCloudlets;
    private List<ApDevice> apDevices;
    private int migPointPolicy;
    private ApDevice correntAP;
    private int nextApId;
    private int nextServerClouletId;
    private int policyReplicaVM;

    private int smartThingPosition;
    private boolean migZone;
    private boolean migPoint;

    public LowestLatency(List<FogDevice> serverCloudlets, List<ApDevice> apDevices, int migPointPolicy, int policyReplicaVM) {
        super();
        setServerCloudlets(serverCloudlets);
        setApDevices(apDevices);
        setMigPointPolicy(migPointPolicy);
        setPolicyReplicaVM(policyReplicaVM);
    }

    @Override
    public boolean shouldMigrate(MobileDevice smartThing) {
        if (smartThing.getSpeed() == 0) { // smartThing is mobile
            logMetrics(smartThing, false, "Device is stationary");
            return false; // no migration
        }

        setCorrentAP(smartThing.getSourceAp());
        // Calculate the relative position between access point and smart thing
        setSmartThingPosition(DiscoverLocalization.discoverLocal(getCorrentAP().getCoord(), smartThing.getCoord()));

        smartThing.getMigrationTechnique().verifyPoints(smartThing, getSmartThingPosition());

        // Set migZone and migPoint before logging
        setMigPoint(smartThing.isMigPoint());
        setMigZone(smartThing.isMigZone());

        if (!(smartThing.isMigPoint() && smartThing.isMigZone())) {
            logMetrics(smartThing, false, "Outside migration zone or point");
            return false; // no migration
        } else {
            setNextServerClouletId(Migration.lowestLatencyCostServerCloudlet(serverCloudlets, apDevices, smartThing));
            if (getNextServerClouletId() < 0) {
                logMetrics(smartThing, false, "No suitable server cloudlet");
                return false;
            } else {
                setNextApId(Migration.nextAp(apDevices, smartThing));
                if (getNextApId() >= 0) {
                    // Verify if the next Ap is edge (return false if the ServerCloudlet destination is the same ServerCloud source)
                    if (!Migration.isEdgeAp(apDevices.get(getNextApId()), smartThing)) {
                        logMetrics(smartThing, false, "Next AP is not an edge AP");
                        return false; // no migration
                    }
                }
            }
        }
        logMetrics(smartThing, true, "Migration approved");
        return ServiceAgreement.serviceAgreement(serverCloudlets.get(getNextServerClouletId()), smartThing);
    }

    private void logMetrics(MobileDevice smartThing, boolean shouldMigrate, String reason) {
        double distanceToSourceAp = Distances.checkDistance(smartThing.getSourceAp().getCoord(), smartThing.getCoord());
        double migrationTime = smartThing.getMigTime();
    
        // Nome da cloudlet local
        FogDevice localCloudlet = smartThing.getVmLocalServerCloudlet();
        String localCloudletName = localCloudlet != null ? localCloudlet.getName() : "None";
    
        // Distância até a cloudlet local
        double distanceToLocalCloudlet = localCloudlet != null
            ? Distances.checkDistance(localCloudlet.getCoord(), smartThing.getCoord())
            : -1;
    
        // Descobrir a cloudlet mais próxima que não seja a local
        FogDevice closestCloudlet = null;
        double minDistance = Double.MAX_VALUE;
    
        for (FogDevice cloudlet : serverCloudlets) {
            if (cloudlet.getId() != (localCloudlet != null ? localCloudlet.getId() : -1)) {
                double distance = Distances.checkDistance(cloudlet.getCoord(), smartThing.getCoord());
                if (distance < minDistance) {
                    minDistance = distance;
                    closestCloudlet = cloudlet;
                }
            }
        }
    
        String closestCloudletName = closestCloudlet != null ? closestCloudlet.getName() : "None";
        double distanceToClosestCloudlet = closestCloudlet != null ? minDistance : -1;
    
        // Próxima cloudlet e AP para migração
        String nextServerCloudlet = getNextServerClouletId() > 0 ? serverCloudlets.get(getNextServerClouletId()).getName() : "None";
        String nextAp = getNextApId() > 0 ? apDevices.get(getNextApId()).getName() : "None";
    
        MyStatistics.getInstance().logMigrationMetrics(
            CloudSim.clock(),                           // Time
            smartThing.getMyId(),                       // DeviceId
            smartThing.getCoord().getCoordX(),          // PosX
            smartThing.getCoord().getCoordY(),          // PosY
            smartThing.getDirection(),                  // Direction
            smartThing.getSpeed(),                      // Speed
            smartThing.getSourceAp().getName(),         // SourceAp
            distanceToSourceAp,                         // DistanceToSourceAp
            migrationTime,                              // MigrationTime
            shouldMigrate,                              // ShouldMigrate
            nextServerCloudlet,                         // NextServerCloudlet
            nextAp,                                     // NextAp
            reason,                                     // Reason
            localCloudletName,                          // Local Cloudlet Name
            distanceToLocalCloudlet,                    // Distance to Local Cloudlet
            closestCloudletName,                        // Closest Cloudlet Name (not local)
            distanceToClosestCloudlet,                  // Distance to Closest Cloudlet (not local)
            isMigPoint(),                               // isMigPoint (new)
            isMigZone()                                 // isMigZone (new)
        );
    }
	

    public ApDevice getCorrentAP() {
        return correntAP;
    }

    public void setCorrentAP(ApDevice correntAP) {
        this.correntAP = correntAP;
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

    public int getNextServerClouletId() {
        return nextServerClouletId;
    }

    public void setNextServerClouletId(int nextServerClouletId) {
        this.nextServerClouletId = nextServerClouletId;
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
