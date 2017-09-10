package com.lightbend.akka.sample.iot;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.io.IOException;

public class IoTMain {

    public static void main(String[] args) throws IOException {
        ActorSystem system = ActorSystem.create("iot-system");

        try {
            ActorRef supervisor = system.actorOf(IotSupervisor.props(), "iot-supervisor");
            ActorRef device = system.actorOf(Device.props("1","2"), "device");

            device.tell(new Device.ReadTemperature(22L), ActorRef.noSender());

            System.out.println("Press ENTER to exit the system");

            System.in.read();
        } finally {
            system.terminate();
        }

    }
}
