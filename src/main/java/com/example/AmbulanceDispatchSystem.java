package com.example;

import java.util.*;

public class AmbulanceDispatchSystem {

    // Emergency priority
    public enum EmergencyPriority {
        CRITICAL(4),
        HIGH(3),
        MODERATE(2),
        NORMAL(1);

        private final int level;

        EmergencyPriority(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    // Ambulance types
    public enum AmbulanceType {
        BASIC,
        ADVANCED_LIFE_SUPPORT,
        ICU
    }

    // Ambulance states
    public enum AmbulanceState {
        AVAILABLE,
        DISPATCHED,
        EN_ROUTE,
        PATIENT_PICKED_UP,
        HOSPITAL_ARRIVED
    }

    // Emergency request
    public static class EmergencyRequest {

        private final String patientId;
        private final String emergencyType;
        private final String pickupLocation;
        private final String destinationHospital;
        private final double distance;
        private final EmergencyPriority priority;

        public EmergencyRequest(
                String patientId,
                String emergencyType,
                String pickupLocation,
                String destinationHospital,
                double distance) {

            this.patientId = patientId;
            this.emergencyType = emergencyType;
            this.pickupLocation = pickupLocation;
            this.destinationHospital = destinationHospital;
            this.distance = distance;
            this.priority = classifyEmergency(emergencyType);
        }

        public static EmergencyPriority classifyEmergency(String emergencyType) {

            if (emergencyType == null) {
                throw new InvalidEmergencyRequestException(
                        "Emergency type cannot be null.");
            }

            String type = emergencyType.toLowerCase();

            if (type.contains("critical") ||
                type.contains("cardiac") ||
                type.contains("heart attack")) {

                return EmergencyPriority.CRITICAL;

            } else if (type.contains("high") ||
                       type.contains("severe") ||
                       type.contains("accident")) {

                return EmergencyPriority.HIGH;

            } else if (type.contains("moderate") ||
                       type.contains("fracture")) {

                return EmergencyPriority.MODERATE;

            } else {
                return EmergencyPriority.NORMAL;
            }
        }

        public String getPatientId() {
            return patientId;
        }

        public String getEmergencyType() {
            return emergencyType;
        }

        public String getPickupLocation() {
            return pickupLocation;
        }

        public String getDestinationHospital() {
            return destinationHospital;
        }

        public double getDistance() {
            return distance;
        }

        public EmergencyPriority getPriority() {
            return priority;
        }
    }

    // Ambulance
    public static class Ambulance {

        private final String ambulanceId;
        private final AmbulanceType type;
        private final String driverName;
        private final String driverPhone;
        private AmbulanceState state;

        public Ambulance(
                String ambulanceId,
                AmbulanceType type,
                String driverName,
                String driverPhone) {

            this.ambulanceId = ambulanceId;
            this.type = type;
            this.driverName = driverName;
            this.driverPhone = driverPhone;
            this.state = AmbulanceState.AVAILABLE;
        }

        public String getAmbulanceId() {
            return ambulanceId;
        }

        public AmbulanceType getType() {
            return type;
        }

        public String getDriverName() {
            return driverName;
        }

        public String getDriverPhone() {
            return driverPhone;
        }

        public AmbulanceState getState() {
            return state;
        }

        public void setState(AmbulanceState state) {
            this.state = state;
        }
    }

    // Custom exception for invalid requests
    public static class InvalidEmergencyRequestException
            extends RuntimeException {

        public InvalidEmergencyRequestException(String message) {
            super(message);
        }
    }

    // Custom exception for unavailable resources
    public static class NoAmbulanceAvailableException
            extends RuntimeException {

        public NoAmbulanceAvailableException(String message) {
            super(message);
        }
    }

    // Booking / dispatch history
    public static class DispatchRecord {

        private final EmergencyRequest request;
        private final Ambulance ambulance;
        private final double estimatedArrivalTime;

        public DispatchRecord(
                EmergencyRequest request,
                Ambulance ambulance,
                double estimatedArrivalTime) {

            this.request = request;
            this.ambulance = ambulance;
            this.estimatedArrivalTime = estimatedArrivalTime;
        }

        public EmergencyRequest getRequest() {
            return request;
        }

        public Ambulance getAmbulance() {
            return ambulance;
        }

        public double getEstimatedArrivalTime() {
            return estimatedArrivalTime;
        }
    }

    private final List<Ambulance> ambulances = new ArrayList<>();
    private final Queue<EmergencyRequest> waitingQueue =
            new PriorityQueue<>(
                    Comparator.comparingInt(
                            (EmergencyRequest r)
                                    -> r.getPriority().getLevel())
                            .reversed());

    private final List<DispatchRecord> history = new ArrayList<>();

    // Add ambulance
    public void addAmbulance(Ambulance ambulance) {

        if (ambulance == null) {
            throw new InvalidEmergencyRequestException(
                    "Ambulance cannot be null.");
        }

        ambulances.add(ambulance);
    }

    // Submit emergency request
    public Ambulance processEmergency(EmergencyRequest request) {

        validateRequest(request);

        Ambulance ambulance = findBestAmbulance(request);

        if (ambulance == null) {

            waitingQueue.offer(request);

            return null;
        }

        dispatch(request, ambulance);

        return ambulance;
    }

    // Validate emergency request
    private void validateRequest(EmergencyRequest request) {

        if (request == null) {
            throw new InvalidEmergencyRequestException(
                    "Emergency request cannot be null.");
        }

        if (request.getPatientId() == null ||
                request.getPatientId().trim().isEmpty()) {

            throw new InvalidEmergencyRequestException(
                    "Patient ID is required.");
        }

        if (request.getPickupLocation() == null ||
                request.getPickupLocation().trim().isEmpty()) {

            throw new InvalidEmergencyRequestException(
                    "Pickup location is required.");
        }

        if (request.getDestinationHospital() == null ||
                request.getDestinationHospital().trim().isEmpty()) {

            throw new InvalidEmergencyRequestException(
                    "Destination hospital is required.");
        }

        if (request.getDistance() <= 0) {

            throw new InvalidEmergencyRequestException(
                    "Distance must be greater than zero.");
        }
    }

    // Find the most appropriate available ambulance
    public Ambulance findBestAmbulance(EmergencyRequest request) {

        List<Ambulance> suitableAmbulances = new ArrayList<>();

        for (Ambulance ambulance : ambulances) {

            if (ambulance.getState() != AmbulanceState.AVAILABLE) {
                continue;
            }

            if (isSuitable(ambulance.getType(),
                    request.getPriority())) {

                suitableAmbulances.add(ambulance);
            }
        }

        if (suitableAmbulances.isEmpty()) {
            return null;
        }

        // Select based on ambulance suitability and distance.
        suitableAmbulances.sort(
                Comparator
                        .comparingInt(
                                (Ambulance a)
                                        -> suitabilityScore(
                                                a.getType(),
                                                request.getPriority()))
                        .reversed()
                        .thenComparingDouble(
                                a -> request.getDistance()));

        return suitableAmbulances.get(0);
    }

    // Check whether ambulance type is suitable
    private boolean isSuitable(
            AmbulanceType type,
            EmergencyPriority priority) {

        if (priority == EmergencyPriority.CRITICAL) {
            return type == AmbulanceType.ICU;
        }

        if (priority == EmergencyPriority.HIGH) {
            return type == AmbulanceType.ICU ||
                   type == AmbulanceType.ADVANCED_LIFE_SUPPORT;
        }

        if (priority == EmergencyPriority.MODERATE) {
            return type == AmbulanceType.ADVANCED_LIFE_SUPPORT ||
                   type == AmbulanceType.BASIC;
        }

        return true;
    }

    // Calculate suitability score
    private int suitabilityScore(
            AmbulanceType type,
            EmergencyPriority priority) {

        if (priority == EmergencyPriority.CRITICAL &&
                type == AmbulanceType.ICU) {
            return 5;
        }

        if (priority == EmergencyPriority.HIGH &&
                type == AmbulanceType.ICU) {
            return 5;
        }

        if (priority == EmergencyPriority.HIGH &&
                type == AmbulanceType.ADVANCED_LIFE_SUPPORT) {
            return 4;
        }

        if (priority == EmergencyPriority.MODERATE &&
                type == AmbulanceType.ADVANCED_LIFE_SUPPORT) {
            return 4;
        }

        if (priority == EmergencyPriority.MODERATE &&
                type == AmbulanceType.BASIC) {
            return 3;
        }

        if (priority == EmergencyPriority.NORMAL &&
                type == AmbulanceType.BASIC) {
            return 3;
        }

        if (priority == EmergencyPriority.NORMAL &&
                type == AmbulanceType.ADVANCED_LIFE_SUPPORT) {
            return 2;
        }

        if (priority == EmergencyPriority.NORMAL &&
                type == AmbulanceType.ICU) {
            return 1;
        }

        return 0;
    }

    // Dispatch ambulance
    private void dispatch(
            EmergencyRequest request,
            Ambulance ambulance) {

        if (ambulance.getState() != AmbulanceState.AVAILABLE) {

            throw new NoAmbulanceAvailableException(
                    "Ambulance is already assigned.");
        }

        ambulance.setState(AmbulanceState.DISPATCHED);

        double estimatedTime =
                calculateEstimatedArrivalTime(request.getDistance());

        history.add(
                new DispatchRecord(
                        request,
                        ambulance,
                        estimatedTime));
    }

    // Calculate estimated arrival time in minutes
    public double calculateEstimatedArrivalTime(double distance) {

        if (distance <= 0) {
            throw new InvalidEmergencyRequestException(
                    "Distance must be greater than zero.");
        }

        // Average emergency ambulance speed = 40 km/h
        double averageSpeed = 40.0;

        return (distance / averageSpeed) * 60;
    }

    // Change ambulance state
    public void updateAmbulanceState(
            String ambulanceId,
            AmbulanceState newState) {

        Ambulance ambulance = getAmbulance(ambulanceId);

        AmbulanceState currentState = ambulance.getState();

        if (!isValidTransition(currentState, newState)) {

            throw new IllegalStateException(
                    "Invalid state transition from "
                            + currentState + " to "
                            + newState);
        }

        ambulance.setState(newState);

        // Automatically allocate when ambulance becomes available
        if (newState == AmbulanceState.AVAILABLE) {
            allocateWaitingRequests();
        }
    }

    // Check valid state transition
    private boolean isValidTransition(
            AmbulanceState current,
            AmbulanceState next) {

        if (current == AmbulanceState.AVAILABLE &&
                next == AmbulanceState.DISPATCHED) {
            return true;
        }

        if (current == AmbulanceState.DISPATCHED &&
                next == AmbulanceState.EN_ROUTE) {
            return true;
        }

        if (current == AmbulanceState.EN_ROUTE &&
                next == AmbulanceState.PATIENT_PICKED_UP) {
            return true;
        }

        if (current == AmbulanceState.PATIENT_PICKED_UP &&
                next == AmbulanceState.HOSPITAL_ARRIVED) {
            return true;
        }

        if (current == AmbulanceState.HOSPITAL_ARRIVED &&
                next == AmbulanceState.AVAILABLE) {
            return true;
        }

        return false;
    }

    // Automatically allocate waiting emergency requests
    public void allocateWaitingRequests() {

        while (!waitingQueue.isEmpty()) {

            EmergencyRequest request = waitingQueue.peek();

            Ambulance ambulance = findBestAmbulance(request);

            if (ambulance == null) {
                break;
            }

            waitingQueue.poll();

            dispatch(request, ambulance);
        }
    }

    // Get ambulance
    private Ambulance getAmbulance(String ambulanceId) {

        for (Ambulance ambulance : ambulances) {

            if (ambulance.getAmbulanceId()
                    .equals(ambulanceId)) {

                return ambulance;
            }
        }

        throw new NoAmbulanceAvailableException(
                "Ambulance not found: " + ambulanceId);
    }

    // Get waiting queue size
    public int getWaitingQueueSize() {
        return waitingQueue.size();
    }

    // Get complete emergency history
    public List<DispatchRecord> getHistory() {
        return Collections.unmodifiableList(history);
    }

    // Display booking / dispatch details
    public String getDispatchDetails(
            EmergencyRequest request) {

        for (DispatchRecord record : history) {

            if (record.getRequest() == request) {

                Ambulance ambulance = record.getAmbulance();

                return "Patient ID: "
                        + request.getPatientId()
                        + "\nEmergency Type: "
                        + request.getEmergencyType()
                        + "\nPriority: "
                        + request.getPriority()
                        + "\nPickup Location: "
                        + request.getPickupLocation()
                        + "\nDestination Hospital: "
                        + request.getDestinationHospital()
                        + "\nDistance: "
                        + request.getDistance()
                        + " km"
                        + "\nAmbulance ID: "
                        + ambulance.getAmbulanceId()
                        + "\nAmbulance Type: "
                        + ambulance.getType()
                        + "\nDriver: "
                        + ambulance.getDriverName()
                        + "\nDriver Phone: "
                        + ambulance.getDriverPhone()
                        + "\nAmbulance Status: "
                        + ambulance.getState()
                        + "\nEstimated Arrival Time: "
                        + record.getEstimatedArrivalTime()
                        + " minutes";
            }
        }

        return "Emergency request is waiting for an ambulance.";
    }

    // Demonstration
    public static void main(String[] args) {

        AmbulanceDispatchSystem system =
                new AmbulanceDispatchSystem();

        Ambulance ambulance1 =
                new Ambulance(
                        "AMB101",
                        AmbulanceType.BASIC,
                        "Rahul",
                        "9876543210");

        Ambulance ambulance2 =
                new Ambulance(
                        "AMB102",
                        AmbulanceType.ADVANCED_LIFE_SUPPORT,
                        "Arun",
                        "9876543211");

        Ambulance ambulance3 =
                new Ambulance(
                        "AMB103",
                        AmbulanceType.ICU,
                        "Kumar",
                        "9876543212");

        system.addAmbulance(ambulance1);
        system.addAmbulance(ambulance2);
        system.addAmbulance(ambulance3);

        EmergencyRequest request =
                new EmergencyRequest(
                        "P001",
                        "Cardiac Critical",
                        "Vellore Bus Stand",
                        "CMC Hospital",
                        8);

        system.processEmergency(request);

        System.out.println(
                system.getDispatchDetails(request));
    }
}
