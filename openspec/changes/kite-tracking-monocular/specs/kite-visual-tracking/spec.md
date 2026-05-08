## ADDED Requirements

### Requirement: System detects kite presence in camera frames
The system SHALL detect the kite in each video frame using a lightweight object detection model or color-based segmentation fallback. Detection SHALL return a bounding box around the kite with a confidence score.

#### Scenario: Successful detection with high confidence
- **WHEN** a kite is visible against a clear sky background
- **THEN** the system SHALL return a bounding box with IoU ≥ 0.7 compared to ground truth
- **AND** the confidence score SHALL be ≥ 0.6

#### Scenario: Detection succeeds with partial occlusion
- **WHEN** the kite is partially occluded (e.g., by cloud, another object covering <30% of area)
- **THEN** the system SHALL still return a bounding box with center displacement error ≤ 10 pixels

#### Scenario: Detection fails when kite not in frame
- **WHEN** the kite is completely outside the camera field of view
- **THEN** the system SHALL return no detection (null or empty bounding box)

### Requirement: System tracks kite across consecutive frames
The system SHALL maintain continuous tracking of the kite by estimating its motion between detection updates using optical flow or feature matching. Tracking SHALL operate at a minimum effective rate of 20 Hz.

#### Scenario: Smooth tracking between detections
- **WHEN** kite moves slowly (< 10 pixels/frame) across consecutive frames
- **THEN** the tracked position SHALL have mean absolute error ≤ 3 pixels compared to true center

#### Scenario: Tracking handles moderate acceleration
- **WHEN** kite accelerates rapidly (up to 50 pixels/frame change)
- **THEN** tracking SHALL NOT lose the kite for at least 8 consecutive frames

#### Scenario: Re-acquisition after tracking loss
- **WHEN** tracking is lost (e.g., kite turns edge-on), then becomes visible again within 2 seconds
- **THEN** re-detection SHALL re-establish the track within 3 frames

### Requirement: Tracker re-identifies kite after loss
The system SHALL periodically run the detector (every 10 frames or upon low tracking confidence) to re-identify and lock onto the kite, preventing drift.

#### Scenario: Periodic re-detection prevents drift
- **WHEN** tracking has been running for 30 consecutive frames without full re-detection
- **THEN** the detector SHALL be invoked to confirm target identity and correct accumulated drift

#### Scenario: Re-identification distinguishes kite from similar objects
- **WHEN** another red object (balloon, bird) enters the field of view
- **THEN** the detector SHALL NOT confuse it with the kite if its shape/size differs by >20%

### Requirement: Tracker is robust to lighting and background changes
The system SHALL maintain tracking under varying outdoor lighting conditions (sunny, partially cloudy) and against sky of varying brightness.

#### Scenario: Tracking under changing illumination
- **WHEN** cloud coverage increases (luminance drops by 50%)
- **THEN** tracking SHALL NOT degrade by more than 10 pixels mean error compared to sunny condition

### Requirement: Tracking operates within resource constraints
The system SHALL NOT exceed 30% CPU usage on a mid-range Android device (Snapdragon 7-series) during active tracking, and SHALL process frames at no less than 15 FPS effective rate.

#### Scenario: CPU usage stays within budget
- **WHEN** tracking is active
- **THEN** CPU utilization SHALL be ≤ 30% on reference device
- **AND** effective tracking rate SHALL be ≥ 20 Hz
