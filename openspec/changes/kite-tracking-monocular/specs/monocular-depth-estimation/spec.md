## ADDED Requirements

### Requirement: System estimates distance to kite in meters
The system SHALL compute the approximate distance (Z coordinate in world frame, along camera optical axis) from the kite's apparent size in the image, given a calibrated real-world size and camera focal length.

#### Scenario: Distance estimation accuracy baseline
- **WHEN** kite is at a known reference distance (calibrated at 20m) with known height (1.2m)
- **THEN** the estimated distance SHALL have mean absolute error ≤ 20% (i.e., ±4m at 20m)

#### Scenario: Distance scales correctly with apparent size
- **WHEN** kite moves from 20m to 40m (doubling distance, halving apparent pixel height)
- **THEN** estimated distance SHALL be within 25% of true distance

#### Scenario: Pitch compensation corrects camera tilt
- **WHEN** camera is pitched upward by 30° relative to horizontal
- **THEN** distance estimate SHALL compensate for pitch angle, and error SHALL be within 25% (otherwise would be ~15% worse without compensation)

### Requirement: System estimates height above ground
The system SHALL estimate the kite's altitude (Y coordinate in world frame, up direction) from distance and known pitch angle, assuming kite flies from user's hand level (or optionally from a known ground reference).

#### Scenario: Height estimation baseline
- **WHEN** kite altitude is 40m above user's hand position, distance = 50m, pitch angle = 45°
- **THEN** estimated height SHALL be within ±15m (≈37% error — acceptable for audio mapping)

#### Scenario: Height updates with changing angle
- **WHEN** kite climbs from 30m to 60m while user tilts head up from 20° to 50°
- **THEN** estimated height trend SHALL be monotonic and within ±20m per 30m true change

### Requirement: System estimates horizontal direction (azimuth)
The system SHALL compute the kite's horizontal direction relative to the user (left/right angle) from the kite's horizontal pixel offset from image center and the current distance.

#### Scenario: Azimuth angle accuracy
- **WHEN** kite is centered +100 pixels right of center at distance 40m on device with 60° horizontal FOV
- **THEN** estimated azimuth SHALL be within ±10° of true bearing

### Requirement: 3D position expressed in phone-relative coordinates
The system SHALL output the kite's 3D position as (X_right, Y_up, Z_forward) in the phone camera coordinate frame, where Z is along optical axis toward kite, X is horizontal screen axis, Y is vertical screen axis.

#### Scenario: Coordinate frame consistency
- **WHEN** kite appears at image center (50%, 50%)
- **THEN** (X, Y) SHALL be (0, 0) in phone-relative coordinates
- **AND** Z SHALL equal estimated distance

### Requirement: System recalibrates depth from periodic reference updates
The system SHALL support an optional re-calibration check: user taps screen over kite at known distance (e.g., kite at 30m -> user enters 30), updating the size-to-distance scaling factor.

#### Scenario: Re-calibration corrects size assumption error
- **WHEN** user corrects distance at current frame (actual kite size was input wrong)
- **THEN** subsequent distance estimates SHALL use updated scaling factor
- **AND** error SHALL reduce from >30% to within 20%
