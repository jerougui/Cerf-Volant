## ADDED Requirements

### Requirement: System generates audio in real time from telemetry
The system SHALL produce an audio stream (44.1 kHz, stereo or mono) with parameters updated at ≥20 Hz based on the kite's current 3D position and velocity, using a lightweight digital synthesizer.

#### Scenario: Audio parameters change with kite position
- **WHEN** kite moves to the right (positive X coordinate increases)
- **THEN** stereo pan SHALL shift toward the right channel (gain right > gain left)

#### Scenario: Audio parameters change with distance
- **WHEN** kite moves farther away (Z distance increases)
- **THEN** overall amplitude SHALL decrease linearly or logarithmically (volume roll-off)
- **AND** timbre MAY become more subdued (e.g., low-pass filter cutoff reduced)

#### Scenario: Audio parameters change with speed
- **WHEN** kite speed increases
- **THEN** modulation rate (vibrato depth, filter cutoff sweeps) SHALL increase proportionally
- **OR** pitch MAY increase slightly to reflect energetic motion

### Requirement: Latency from position update to audible output ≤ 100ms
The end-to-end latency from frame capture to audible change SHALL be ≤ 100 ms (95th percentile), including image processing, position estimation, and audio buffer submission.

#### Scenario: Low-latency audio path
- **WHEN** tracking is active at 30 FPS
- **THEN** audio parameter update delay SHALL be ≤ 3 frames (~100ms at 30 FPS)
- **AND** audio callback buffer underruns SHALL NOT occur under normal load

### Requirement: Audio synthesis runs on a dedicated low-latency thread
The system SHALL use Android's Oboe library (or AAudio on API 26+) with an ultra-low-latency audio stream, and update synthesis parameters in a thread-safe manner from the vision/tracking thread.

#### Scenario: Parameter updates are lock-free or minimally contended
- **WHEN** vision thread computes new telemetry at 30 Hz
- **THEN** it SHALL write parameters to a double-buffered or atomic variable
- **AND** the audio callback SHALL read the latest value without blocking > 1 ms

### Requirement: Default sound is a simple tonal sonification
The system SHALL produce a continuous tone (sine or triangle wave) with configurable base frequency (default: 220 Hz), whose pitch, pan, and modulation are driven by telemetry.

#### Scenario: Default tone audible and pleasant
- **WHEN** no custom sound selected
- **THEN** output SHALL be a continuous monophonic tone with exponential amplitude envelope (no clicks)

### Requirement: Audio mapping curves are configurable
The system SHALL allow the user to adjust scaling factors: X → pan curve, Z → volume curve, speed → modulation intensity, either via preset modes (subtle/dramatic) or manual sliders.

#### Scenario: Customizable parameter responsiveness
- **WHEN** user selects "High responsiveness" mode
- **THEN** pan SHALL update with 1:1 mapping from normalized X coordinate (-1 to 1)
- **AND** volume SHALL drop 6 dB per 10m distance increase

### Requirement: Audio output may route to Bluetooth
The system SHALL support Bluetooth audio output (A2DP or Bluetooth LE Audio) as an option, and SHALL display a warning that Bluetooth adds 100-200ms latency.

#### Scenario: Bluetooth selection
- **WHEN** user selects a Bluetooth audio device as output
- **THEN** audio SHALL route through that device
- **AND** a message SHALL be shown: "Bluetooth may add delay; for best responsiveness use wired headphones"
