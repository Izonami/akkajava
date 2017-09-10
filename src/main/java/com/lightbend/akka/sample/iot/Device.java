package com.lightbend.akka.sample.iot;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.lightbend.akka.sample.iot.DeviceManager.DeviceRegistered;
import com.lightbend.akka.sample.iot.DeviceManager.RequestTrackDevice;

import java.util.Optional;

public final class Device extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(final String groupId, final String deviceId) {
        return Props.create(Device.class, groupId, deviceId);
    }

    private final String groupId;
    private final String deviceId;

    private Optional<Double> lastTemperatureReading = Optional.empty();

    public Device(final String groupId, final String deviceId) {
        this.groupId = groupId;
        this.deviceId = deviceId;
    }

    public static final class RecordTemperature {
        private final long requestId;
        private final double value;

        public RecordTemperature(final long requestId, final double value) {
            this.requestId = requestId;
            this.value = value;
        }

        public long getRequestId() {
            return requestId;
        }

        public double getValue() {
            return value;
        }
    }

    public static final class TemperatureRecorded {
        private final long requestId;

        public TemperatureRecorded(final long requestId) {
            this.requestId = requestId;
        }

        public long getRequestId() {
            return requestId;
        }
    }

    public static final class ReadTemperature {
        private final long requestId;

        public ReadTemperature(final long requestId) {
            this.requestId = requestId;
        }

        public long getRequestId() {
            return requestId;
        }
    }

    public static final class RespondTemperature {
        private final long requestId;
        private final Optional<Double> value;

        public RespondTemperature(final long requestId, final Optional<Double> value) {
            this.requestId = requestId;
            this.value = value;
        }

        public long getRequestId() {
            return requestId;
        }

        public Optional<Double> getValue() {
            return value;
        }
    }

    @Override
    public void preStart() {
        log.info("Device actor {}-{} started", groupId, deviceId);
    }

    @Override
    public void postStop() {
        log.info("Device actor {}-{} stopped", groupId, deviceId);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RequestTrackDevice.class, requestTrackDevice -> {
                    if (this.groupId.equals(requestTrackDevice.getGroupId()) && this.deviceId.equals(requestTrackDevice.getDeviceId())) {
                        getSender().tell(new DeviceRegistered(), getSelf());
                    } else {
                        log.warning(
                                "Ignoring TrackDevice request for {}-{}.This actor is responsible for {}-{}.",
                                requestTrackDevice.getGroupId(), requestTrackDevice.getDeviceId(), this.groupId, this.deviceId
                        );
                    }
                })
                .match(RecordTemperature.class, recordTemperature -> {
                    log.info("Recorded temperature reading {} with {}", recordTemperature.getValue(), recordTemperature.getRequestId());
                    lastTemperatureReading = Optional.of(recordTemperature.getValue());
                    getSender().tell(new TemperatureRecorded(recordTemperature.getRequestId()), getSelf());
                })
                .match(ReadTemperature.class, readTemperature ->
                        getSender().tell(new RespondTemperature(readTemperature.getRequestId(), lastTemperatureReading), getSelf()))
                .build();
    }
}
