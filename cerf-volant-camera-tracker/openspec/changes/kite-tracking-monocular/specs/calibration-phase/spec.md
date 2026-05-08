## ADDED Requirements

### Requirement: Calibration measures initial distance to kite
During setup, the system SHALL prompt the user to place the kite at a known, measured distance from the phone (e.g., using a tape measure, or user steps back a counted number of paces), then confirm via UI button.

#### Scenario: User confirms reference distance
- **WHEN** the kite is placed at measured distance D₀ (e.g., 20.0 m)
- **AND** user taps "Calibrate distance" button
- **THEN** the system SHALL store D₀ as the reference distance in meters
- **AND** capture the current bounding box of the kite (in pixels) as the reference apparent size

### Requirement: Calibration captures kite apparent size
The system SHALL ask the user to draw or confirm a bounding box around the kite during calibration, measuring its pixel height h₀ (and optionally width).

#### Scenario: Bounding box measured in pixels
- **WHEN** user draws rectangle around kite (or auto-detection is accepted)
- **THEN** the system SHALL extract the bounding box height h₀ in pixels
- **AND** optionally width w₀ for shape aspect ratio

### Requirement: Calibration records camera orientation angles
The system SHALL read the phone's IMU (gyroscope/accelerometer) during calibration to capture the camera's pitch θ (elevation angle relative to horizontal) and roll φ (tilt left/right).

#### Scenario: Pitch angle compensation established
- **WHEN** camera is pitched upward at angle θ₀ during calibration
- **THEN** the system SHALL store θ₀
- **AND** use cos(θ₀) as multiplicative factor in depth formula to correct foreshortening

### Requirement: Calibration determines camera focal length
The system SHALL obtain the camera's focal length in pixels either from EXIF metadata (if available) or from device camera specification database, or allow manual entry.

#### Scenario: Focal length auto-detected
- **WHEN** calibration starts on an Android device with Camera2 API
- **THEN** the system SHALL query `CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS`
- **AND** convert mm to pixel units using `sensor_width_mm / image_width_px`

#### Scenario: Manual focal length override permitted
- **WHEN** auto-detected focal length is inaccurate or unavailable
- **THEN** user may enter focal length in mm manually

### Requirement: Calibration validates inputs before proceeding
The system SHALL verify that reference distance > 5m, kite occupies ≥ 1% of frame height, and bounding box is reasonably centered; otherwise prompt user to adjust.

#### Scenario: Validation rejects too-close calibration
- **WHEN** user attempts calibration at distance < 5m
- **THEN** the system SHALL show warning "Please move further away (minimum 5m)"
- **AND** SHALL NOT proceed to tracking mode

### Requirement: Calibration data used to compute scaling factor
Upon successful calibration, the system SHALL compute the base scale factor: S = (real_height * focal_length) / (apparent_pixels * cos(pitch)), to be applied in depth formula during tracking.

#### Scenario: Scaling factor computed correctly
- **WHEN** calibration inputs: H_real=1.2m, h_px=60px, f_px=1200px, pitch=20°
- **THEN** base scale S = 1.2*1200/(60*cos(20°)) ≈ 25.5 m/px (meaning each pixel of kite height corresponds to ~25m distance)

### Requirement: Optional re-calibration during use
The system SHALL allow the user to trigger a quick re-calibration at any time if tracking accuracy degrades, by tapping the kite on screen and entering current true distance (or "unknown" for size-priors refresh).

#### Scenario: Re-calibration corrects accumulated drift
- **WHEN** user taps "Recalibrate" after kite has been flying for some time
- **THEN** current bounding box and distance input replace original reference values in scaling factor computation
- **AND** tracking accuracy SHALL improve within next 5 frames
