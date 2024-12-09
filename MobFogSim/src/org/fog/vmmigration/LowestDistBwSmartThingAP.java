package org.fog.vmmigration;

import java.util.List;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.entities.ApDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.localization.DiscoverLocalization;
import org.fog.localization.Distances;

public class LowestDistBwSmartThingAP implements DecisionMigration {

	private List<FogDevice> serverCloudlets;
	private List<ApDevice> apDevices;
	private int migPointPolicy;
	private ApDevice correntAP;
	private int nextApId;
	private int nextServerClouletId;
	private int smartThingPosition;
	private boolean migZone;
	private boolean migPoint;
	private int policyReplicaVM;

	public LowestDistBwSmartThingAP(List<FogDevice> serverCloudlets,
		List<ApDevice> apDevices, int migPointPolicy, int policyReplicaVM) {
		setServerCloudlets(serverCloudlets);
		setApDevices(apDevices);
		setMigPointPolicy(migPointPolicy);
		setPolicyReplicaVM(policyReplicaVM);
	}

	@Override
	public boolean shouldMigrate(MobileDevice smartThing) {
		setCorrentAP(smartThing.getSourceAp());
		setSmartThingPosition(DiscoverLocalization.discoverLocal(getCorrentAP().getCoord(), smartThing.getCoord()));
		smartThing.getMigrationTechnique().verifyPoints(smartThing, getSmartThingPosition());

		if (!(smartThing.isMigPoint() && smartThing.isMigZone())) {
			logMetrics(smartThing, false, "Not in migPoint and migZone");
			return false;
		} else {
			setNextApId(Migration.nextAp(apDevices, smartThing));
			if (getNextApId() < 0) {
				logMetrics(smartThing, false, "No suitable next AP");
				return false;
			}
			if (!Migration.isEdgeAp(apDevices.get(getNextApId()), smartThing)) {
				logMetrics(smartThing, false, "Next AP is not an edge AP");
				return false;
			}
			setNextServerClouletId(apDevices.get(getNextApId()).getServerCloudlet().getMyId());
		}

		boolean result = ServiceAgreement.serviceAgreement(serverCloudlets.get(getNextServerClouletId()), smartThing);
		logMetrics(smartThing, result, "Service agreement decision");
		return ServiceAgreement.serviceAgreement(serverCloudlets.get(getNextServerClouletId()), smartThing);
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

		String nextServerCloudletName = (getNextServerClouletId() > 0) ? serverCloudlets.get(getNextServerClouletId()).getName() : "None";
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

	public ApDevice getCorrentAP() {
		return correntAP;
	}

	public void setCorrentAP(ApDevice correntAP) {
		this.correntAP = correntAP;
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
