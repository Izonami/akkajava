package com.lightbend.akka.sample;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Terminated;
import akka.testkit.javadsl.TestKit;
import com.lightbend.akka.sample.iot.Device;
import com.lightbend.akka.sample.iot.DeviceManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class DeviceManagerTest {

    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testRegisterDeviceActor() {
        TestKit probe = new TestKit(system);

        ActorRef deviceManager = system.actorOf(DeviceManager.props());
        deviceManager.tell(new DeviceManager.RequestTrackDevice("group", "device1"), probe.getRef());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
    }

    @Test
    public void testRegisterDevicesActor() {
        TestKit probe = new TestKit(system);

        ActorRef deviceManager = system.actorOf(DeviceManager.props());

        deviceManager.tell(new DeviceManager.RequestTrackDevice("group", "device1"), probe.getRef());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
        ActorRef device1 = probe.getLastSender();


        deviceManager.tell(new DeviceManager.RequestTrackDevice("group", "device2"), probe.getRef());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
        ActorRef device2 = probe.getLastSender();

        device1.tell(new Device.RecordTemperature(1L, 45.0), probe.getRef());
        Device.TemperatureRecorded temperatureRecorded1 = probe.expectMsgClass(Device.TemperatureRecorded.class);
        assertEquals(1L, temperatureRecorded1.getRequestId());

        device2.tell(new Device.RecordTemperature(1L, 33.2), probe.getRef());
        Device.TemperatureRecorded temperatureRecorded2 = probe.expectMsgClass(Device.TemperatureRecorded.class);
        assertEquals(1L, temperatureRecorded2.getRequestId());

        device1.tell(new Device.ReadTemperature(2L), probe.getRef());
        Device.RespondTemperature respondTemperature1 = probe.expectMsgClass(Device.RespondTemperature.class);
        assertEquals(2L, respondTemperature1.getRequestId());
        assertEquals(Optional.of(45.0), respondTemperature1.getValue());

        device2.tell(new Device.ReadTemperature(3L), probe.getRef());
        Device.RespondTemperature respondTemperature2 = probe.expectMsgClass(Device.RespondTemperature.class);
        assertEquals(3L, respondTemperature2.getRequestId());
        assertEquals(Optional.of(33.2), respondTemperature2.getValue());
    }

    @Test
    public void testTerminateDeviceGroup() {
        TestKit probe = new TestKit(system);

        ActorRef deviceManager = system.actorOf(DeviceManager.props());
        deviceManager.tell(new DeviceManager.RequestTrackDevice("group", "device1"), probe.getRef());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
        ActorRef deviceToTerminate = probe.getLastSender();

        probe.watch(deviceToTerminate);
        deviceToTerminate.tell(PoisonPill.getInstance(), ActorRef.noSender());
        probe.expectTerminated(deviceToTerminate);
    }
}
