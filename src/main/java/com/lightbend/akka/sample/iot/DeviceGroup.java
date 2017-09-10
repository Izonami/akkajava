package com.lightbend.akka.sample.iot;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class DeviceGroup extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), getSelf());

    public static Props props(String groupId) {
        return Props.create(DeviceGroup.class, groupId);
    }

    private final Map<ActorRef, String> actorToDeviceId = new HashMap<>();
    private final Map<String, ActorRef> deviceIdToActor = new HashMap<>();
    private final String groupId;

    public DeviceGroup(final String groupId) {
        this.groupId = groupId;
    }

    public static final class RequestDeviceList {
        private final long requestId;

        public RequestDeviceList(final long requestId) {
            this.requestId = requestId;
        }
    }

    public static final class ReplyDeviceList {
        private final long requestId;
        private final Set<String> deviceIds;

        public ReplyDeviceList(final long requestId, final Set<String> deviceIds) {
            this.requestId = requestId;
            this.deviceIds = deviceIds;
        }

        public long getRequestId() {
            return requestId;
        }

        public Set<String> getDeviceIds() {
            return deviceIds;
        }
    }

    @Override
    public void preStart() {
        log.info("DeviceGroup {} started", groupId);
    }

    @Override
    public void postStop() {
        log.info("DeviceGroup {} stopped", groupId);
    }

    private void onTrackDevice(DeviceManager.RequestTrackDevice trackMsg) {
        if (this.groupId.equals(trackMsg.getGroupId())) {
            ActorRef deviceActor = deviceIdToActor.get(trackMsg.getDeviceId());
            if (deviceActor != null) {
                deviceActor.forward(trackMsg, getContext());
            } else {
                log.info("Creating device actor for {}", trackMsg.getDeviceId());
                deviceActor = getContext().actorOf(Device.props(groupId, trackMsg.getDeviceId()), "device-" + trackMsg.getDeviceId());
                getContext().watch(deviceActor);
                actorToDeviceId.put(deviceActor, trackMsg.getDeviceId());
                deviceIdToActor.put(trackMsg.getDeviceId(), deviceActor);
                deviceActor.forward(trackMsg, getContext());
            }
        } else {
            log.warning(
                    "Ignoring TrackDevice request for {}. This actor is responsible for {}.",
                    groupId, this.groupId
            );
        }
    }

    private void onDeviceList(RequestDeviceList r) {
        getSender().tell(new ReplyDeviceList(r.requestId, deviceIdToActor.keySet()), getSelf());
    }

    private void onTerminated(Terminated t) {
        ActorRef deviceActor = t.getActor();
        String deviceId = actorToDeviceId.get(deviceActor);
        log.info("Device actor for {} has been terminated", deviceId);
        actorToDeviceId.remove(deviceActor);
        deviceIdToActor.remove(deviceId);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(DeviceManager.RequestTrackDevice.class, this::onTrackDevice)
                .match(RequestDeviceList.class, this::onDeviceList)
                .match(Terminated.class, this::onTerminated)
                .build();
    }
}
