## ADDED Requirements

### Requirement: System calculates instantaneous velocity vector
Given a history of 3D position estimates at timestamps t₀..tₙ, the system SHALL compute the current 3D velocity vector as the derivative of position, with optional low-pass filtering to reduce noise.

#### Scenario: Velocity calculation from position history
- **WHEN** position history contains at least 2 samples within the last 0.5 seconds
- **THEN** velocity SHALL V = (P_now - P_prev) / (t_now - t_prev)
- **AND** the magnitude SHALL be reported as speed in m/s

#### Scenario: Velocity smoothing reduces jitter
- **WHEN** position noise is present (Gaussian, σ=0.5m at 20Hz)
- **THEN** applying EMA filter (α=0.3) SHALL reduce velocity standard deviation by ≥40% with ≤50ms effective delay

#### Scenario: Velocity zero when stationary
- **WHEN** kite is held stationary (relative to user) for 2 seconds
- **THEN** reported speed SHALL be ≤ 0.2 m/s

### Requirement: System outputs speed magnitude
The system SHALL provide instantaneous speed as a scalar value in meters per second (m/s), updated at effective tracking rate (≥20 Hz).

#### Scenario: Speed reflects rapid changes
- **WHEN** kite accelerates from 5 m/s to 20 m/s over 2 seconds
- **THEN** reported speed SHALL increase monotonically
- **AND** speed at t+1s SHALL be within 3 m/s of true value

### Requirement: System calculates direction vector
The system SHALL compute a 2D direction angle (azimuth) in the horizontal plane, expressed either as radians/degrees from forward or as a unit vector (dx, 0, dz).

#### Scenario: Direction points toward flight path
- **WHEN** kite moves from position (10m left, 30m forward) to position (5m right, 35m forward)
- **THEN** the direction vector SHALL point generally rightward with positive forward component

### Requirement: High accelerations produce stable velocity estimate
The system SHALL apply outlier rejection on position differentials: if displacement between consecutive frames exceeds a physically plausible maximum (e.g., 15 m/s equivalent velocity change > 3σ), the differential SHALL be capped or discarded.

#### Scenario: Velocity spike suppression
- **WHEN** a tracking glitch causes position to jump 5m in one frame (temporal artifact)
- **THEN** velocity estimate SHALL NOT show a 250 m/s spike
- **AND** tracking re-acquisition SHALL occur within next frame

### Requirement: Velocity data available for audio rendering
The system SHALL expose current velocity magnitude and direction through a thread-safe API to the audio module, updated at 20-50 Hz.

#### Scenario: Audio module receives smooth velocity updates
- **WHEN** tracking is active
- **THEN** velocity data SHALL be readable by audio thread without blocking
- **AND** updates SHALL be no older than 50ms
