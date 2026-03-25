# BlockCalls - Android Call Blocker

An Android application that blocks incoming calls based on pattern matching. Block entire number ranges using wildcard patterns like `0850 *****`.

## Features

- **Pattern-Based Blocking**: Block numbers by prefix (e.g., `0850 *****` blocks all numbers starting with 0850)
- **Manual Block List**: Add/remove specific numbers or patterns
- **Blocked Calls Log**: View history of all blocked calls with timestamps
- **Swipe to Delete**: Easy pattern management with swipe gestures
- **Modern Material Design UI**: Clean and intuitive interface

## Requirements

- Android 9.0 (API 28) or higher
- Phone permissions (READ_PHONE_STATE, READ_CALL_LOG, ANSWER_PHONE_CALLS)
- Call Screening role (app will request this on first launch)

## How It Works

1. **CallScreeningService**: Intercepts incoming calls before they ring
2. **Pattern Matching**: Checks incoming numbers against your block list
3. **Auto-Reject**: Automatically rejects matching calls
4. **Logging**: Records all blocked calls with the pattern that blocked them

## Installation

1. Open the project in Android Studio
2. Build and run on a physical Android device (Android 9+)
3. Grant all requested permissions
4. Enable Call Screening role when prompted

## Usage

1. **Add a Pattern**: Tap the + button and enter a pattern
   - Use `*` to match any digit
   - Example: `0850 *****` blocks all numbers starting with 0850
   - Example: `0850 222 22 22` blocks this specific number

2. **View Blocked Calls**: Tap the history icon in the toolbar

3. **Delete a Pattern**: Swipe left or right on any pattern

## Testing

**Note**: Call blocking can only be fully tested on a physical device with an active SIM card. Emulators have limited call testing capabilities.

## License

This project is open source and available for educational purposes.
