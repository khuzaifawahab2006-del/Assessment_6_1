package com.example;

import org.junit.Test;

import static org.junit.Assert.*;

public class AmbulanceDispatchSystemTest {

    @Test
    public void testCriticalEmergencyGetsICUAmbulance() {

        AmbulanceDispatchSystem system =
                new AmbulanceDispatchSystem();

        AmbulanceDispatchSystem.Ambulance basic =
                new AmbulanceDispatchSystem.Ambulance(
                        "AMB101",
                        AmbulanceDispatchSystem.AmbulanceType.BASIC,
                        "Rahul",
                        "9000000001");

        AmbulanceDispatchSystem.Ambulance icu =
                new AmbulanceDispatchSystem.Ambulance(
                        "AMB103",
                        AmbulanceDispatchSystem.AmbulanceType.ICU,
                        "Kumar",
                        "9000000003");

        system.addAmbulance(basic);
        system.addAmbulance(icu);

        AmbulanceDispatchSystem.EmergencyRequest request =
                new AmbulanceDispatchSystem.EmergencyRequest(
                        "P001",
                        "Cardiac Critical",
                        "Vellore",
                        "CMC Hospital",
                        8);

        AmbulanceDispatchSystem.Ambulance selected =
                system.processEmergency(request);

        assertEquals("AMB103",
                selected.getAmbulanceId());
    }

    @Test
    public void testEmergencyClassification() {

        assertEquals(
                AmbulanceDispatchSystem.EmergencyPriority.CRITICAL,
                AmbulanceDispatchSystem.EmergencyRequest
                        .classifyEmergency("Cardiac"));

        assertEquals(
                AmbulanceDispatchSystem.EmergencyPriority.HIGH,
                AmbulanceDispatchSystem.EmergencyRequest
                        .classifyEmergency("Accident"));

        assertEquals(
                AmbulanceDispatchSystem.EmergencyPriority.MODERATE,
                AmbulanceDispatchSystem.EmergencyRequest
                        .classifyEmergency("Fracture"));

        assertEquals(
                AmbulanceDispatchSystem.EmergencyPriority.NORMAL,
                AmbulanceDispatchSystem.EmergencyRequest
                        .classifyEmergency("Fever"));
    }

    @Test
    public void testAmbulanceStateFlow() {

        AmbulanceDispatchSystem system =
                new AmbulanceDispatchSystem();

        AmbulanceDispatchSystem.Ambulance ambulance =
                new AmbulanceDispatchSystem.Ambulance(
                        "AMB101",
                        AmbulanceDispatchSystem.AmbulanceType.BASIC,
                        "Rahul",
                        "9000000001");

        system.addAmbulance(ambulance);

        AmbulanceDispatchSystem.EmergencyRequest request =
                new AmbulanceDispatchSystem.EmergencyRequest(
                        "P002",
                        "Fever",
                        "Vellore",
                        "Hospital A",
                        5);

        system.processEmergency(request);

        assertEquals(
                AmbulanceDispatchSystem.AmbulanceState.DISPATCHED,
                ambulance.getState());

        system.updateAmbulanceState(
                "AMB101",
                AmbulanceDispatchSystem.AmbulanceState.EN_ROUTE);

        assertEquals(
                AmbulanceDispatchSystem.AmbulanceState.EN_ROUTE,
                ambulance.getState());

        system.updateAmbulanceState(
                "AMB101",
                AmbulanceDispatchSystem.AmbulanceState.PATIENT_PICKED_UP);

        assertEquals(
                AmbulanceDispatchSystem.AmbulanceState.PATIENT_PICKED_UP,
                ambulance.getState());

        system.updateAmbulanceState(
                "AMB101",
                AmbulanceDispatchSystem.AmbulanceState.HOSPITAL_ARRIVED);

        assertEquals(
                AmbulanceDispatchSystem.AmbulanceState.HOSPITAL_ARRIVED,
                ambulance.getState());

        system.updateAmbulanceState(
                "AMB101",
                AmbulanceDispatchSystem.AmbulanceState.AVAILABLE);

        assertEquals(
                AmbulanceDispatchSystem.AmbulanceState.AVAILABLE,
                ambulance.getState());
    }

    @Test
    public void testEstimatedArrivalTime() {

        AmbulanceDispatchSystem system =
                new AmbulanceDispatchSystem();

        double time =
                system.calculateEstimatedArrivalTime(20);

        assertEquals(30.0, time, 0.01);
    }

    @Test
    public void testWaitingQueue() {

        AmbulanceDispatchSystem system =
                new AmbulanceDispatchSystem();

        AmbulanceDispatchSystem.Ambulance ambulance =
                new AmbulanceDispatchSystem.Ambulance(
                        "AMB101",
                        AmbulanceDispatchSystem.AmbulanceType.BASIC,
                        "Rahul",
                        "9000000001");

        system.addAmbulance(ambulance);

        AmbulanceDispatchSystem.EmergencyRequest first =
                new AmbulanceDispatchSystem.EmergencyRequest(
                        "P001",
                        "Fever",
                        "Location A",
                        "Hospital A",
                        5);

        AmbulanceDispatchSystem.EmergencyRequest second =
                new AmbulanceDispatchSystem.EmergencyRequest(
                        "P002",
                        "Fever",
                        "Location B",
                        "Hospital B",
                        10);

        system.processEmergency(first);

        system.processEmergency(second);

        assertEquals(1,
                system.getWaitingQueueSize());
    }

    @Test
    public void testAutomaticAllocationWhenAmbulanceBecomesAvailable() {

        AmbulanceDispatchSystem system =
                new AmbulanceDispatchSystem();

        AmbulanceDispatchSystem.Ambulance ambulance =
                new AmbulanceDispatchSystem.Ambulance(
                        "AMB101",
                        AmbulanceDispatchSystem.AmbulanceType.BASIC,
                        "Rahul",
                        "9000000001");

        system.addAmbulance(ambulance);

        AmbulanceDispatchSystem.EmergencyRequest first =
                new AmbulanceDispatchSystem.EmergencyRequest(
                        "P001",
                        "Fever",
                        "Location A",
                        "Hospital A",
                        5);

        AmbulanceDispatchSystem.EmergencyRequest second =
                new AmbulanceDispatchSystem.EmergencyRequest(
                        "P002",
                        "Fever",
                        "Location B",
                        "Hospital B",
                        10);

        system.processEmergency(first);
        system.processEmergency(second);

        assertEquals(1,
                system.getWaitingQueueSize());

        system.updateAmbulanceState(
                "AMB101",
                AmbulanceDispatchSystem.AmbulanceState.EN_ROUTE);

        system.updateAmbulanceState(
                "AMB101",
                AmbulanceDispatchSystem.AmbulanceState.PATIENT_PICKED_UP);

        system.updateAmbulanceState(
                "AMB101",
                AmbulanceDispatchSystem.AmbulanceState.HOSPITAL_ARRIVED);

        system.updateAmbulanceState(
                "AMB101",
                AmbulanceDispatchSystem.AmbulanceState.AVAILABLE);

        assertEquals(0,
                system.getWaitingQueueSize());

        assertEquals(2,
                system.getHistory().size());

        assertEquals(
                AmbulanceDispatchSystem.AmbulanceState.DISPATCHED,
                ambulance.getState());
    }

    @Test
    public void testHistoryIsMaintained() {

        AmbulanceDispatchSystem system =
                new AmbulanceDispatchSystem();

        AmbulanceDispatchSystem.Ambulance ambulance =
                new AmbulanceDispatchSystem.Ambulance(
                        "AMB101",
                        AmbulanceDispatchSystem.AmbulanceType.BASIC,
                        "Rahul",
                        "9000000001");

        system.addAmbulance(ambulance);

        AmbulanceDispatchSystem.EmergencyRequest request =
                new AmbulanceDispatchSystem.EmergencyRequest(
                        "P100",
                        "Fever",
                        "Vellore",
                        "Hospital A",
                        10);

        system.processEmergency(request);

        assertEquals(1,
                system.getHistory().size());

        assertEquals(
                "P100",
                system.getHistory()
                        .get(0)
                        .getRequest()
                        .getPatientId());
    }

    @Test
    public void testInvalidDistanceThrowsException() {

        AmbulanceDispatchSystem.EmergencyRequest request =
                new AmbulanceDispatchSystem.EmergencyRequest(
                        "P001",
                        "Accident",
                        "Vellore",
                        "Hospital A",
                        -5);

        AmbulanceDispatchSystem system =
                new AmbulanceDispatchSystem();

        try {

            system.processEmergency(request);

            fail("Expected InvalidEmergencyRequestException");

        } catch (
                AmbulanceDispatchSystem.InvalidEmergencyRequestException e) {

            assertEquals(
                    "Distance must be greater than zero.",
                    e.getMessage());
        }
    }

    @Test
    public void testInvalidStateTransition() {

        AmbulanceDispatchSystem system =
                new AmbulanceDispatchSystem();

        AmbulanceDispatchSystem.Ambulance ambulance =
                new AmbulanceDispatchSystem.Ambulance(
                        "AMB101",
                        AmbulanceDispatchSystem.AmbulanceType.BASIC,
                        "Rahul",
                        "9000000001");

        system.addAmbulance(ambulance);

        try {

            system.updateAmbulanceState(
                    "AMB101",
                    AmbulanceDispatchSystem.AmbulanceState.HOSPITAL_ARRIVED);

            fail("Expected IllegalStateException");

        } catch (IllegalStateException e) {

            assertTrue(
                    e.getMessage()
                            .contains("Invalid state transition"));
        }
    }
}
