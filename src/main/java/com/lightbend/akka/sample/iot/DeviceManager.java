package com.lightbend.akka.sample.iot;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.HashMap;
import java.util.Map;

public class DeviceManager extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props() {
        return Props.create(DeviceManager.class);
    }

    final Map<String, ActorRef> groupIdToActor = new HashMap<>();
    final Map<ActorRef, String> actorToGroupId = new HashMap<>();

    public static final class RequestTrackDevice {
        private final String groupId;
        private final String deviceId;

        public RequestTrackDevice(final String groupId, final String deviceId) {
            this.groupId = groupId;
            this.deviceId = deviceId;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getDeviceId() {
            return deviceId;
        }
    }

    public static final class DeviceRegistered {
    }

    private void onTrackDevice(RequestTrackDevice trackMsg) {
        String groupId = trackMsg.groupId;
        ActorRef ref = groupIdToActor.get(groupId);
        if (ref != null) {
            ref.forward(trackMsg, getContext());
        } else {
            log.info("Creating device group actor for {}", groupId);
            ActorRef groupActor = getContext().actorOf(DeviceGroup.props(groupId), "group-" + groupId);
            getContext().watch(groupActor);
            groupActor.forward(trackMsg, getContext());
            groupIdToActor.put(groupId, groupActor);
            actorToGroupId.put(groupActor, groupId);
        }
    }

    private void onTerminated(Terminated t) {
        ActorRef groupActor = t.getActor();
        String groupId = actorToGroupId.get(groupActor);
        log.info("Device group actor for {} has been terminated", groupId);
        actorToGroupId.remove(groupActor);
        groupIdToActor.remove(groupId);
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RequestTrackDevice.class, this::onTrackDevice)
                .match(Terminated.class, this::onTerminated)
                .build();
    }
}
