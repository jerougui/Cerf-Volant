## ADDED Requirements

### Requirement: System detects available Bluetooth audio devices
The system SHALL query the Android BluetoothManager for paired A2DP or LE Audio devices and present them in the audio output selector.

#### Scenario: Bluetooth device appears in output list
- **WHEN** a Bluetooth speaker/headset is paired and currently connected
- **THEN** the device name SHALL appear in the audio output dropdown

### Requirement: System routes audio to selected Bluetooth device
Upon user selection, the system SHALL set the Android AudioManager's active Bluetooth device or use Oboe's device enumeration to open the audio stream on that device.

#### Scenario: Audio output switches to Bluetooth
- **WHEN** user selects "Bluetooth Speaker XYZ" from list
- **THEN** subsequent audio SHALL play through that device within 2 seconds

### Requirement: System reports estimated Bluetooth latency
The system SHALL either estimate Bluetooth audio latency based on known codec profiles (SBC ~180ms, AAC ~100ms, aptX Low Latency ~40ms) or measure round-trip, and display this to the user.

#### Scenario: Latency guidance shown
- **WHEN** a Bluetooth device is active
- **THEN** the UI SHALL show "Bluetooth latency: ~120ms (AAC)"
- **AND** a tooltip or help text SHALL explain impact on responsiveness

### Requirement: System handles Bluetooth disconnection gracefully
If the selected Bluetooth device disconnects during use, the system SHALL automatically fall back to the phone's built-in speaker or last-wired output without crashing.

#### Scenario: Bluetooth dropout recovery
- **WHEN** Bluetooth device turns off or moves out of range
- **THEN** audio SHALL continue through wired/mono speaker within 500ms
- **AND** a notification SHALL inform user: "Bluetooth disconnected, using phone speaker"

### Requirement: System supports reconnection without resetting audio engine
If the Bluetooth device reconnects within 30 seconds, the system SHALL detect it and optionally prompt the user to switch back.

#### Scenario: Bluetooth returns
- **WHEN** Bluetooth device reconnects after a brief dropout
- **THEN** the device reappears in the active device list
- **AND** user may switch back with one tap
