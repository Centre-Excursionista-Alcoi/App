import ComposeApp
import Foundation

/// Launch arguments for driving the app from a script (a simulator can't be tapped from the command line). They
/// exist in debug builds only:
///
/// - `-CEASkipNotificationPrompt`: doesn't ask for the notification permission at start.
/// - `-CEADebugLogin email:password`: signs in with them on the login screen.
/// - `-CEADebugTab home|lendings|lending|activities|management|profile`: the tab to start on.
/// - `-CEADebugOpen settings|security`: a screen to open over the tabs.
/// - `-CEADebugSync`: syncs with the server as soon as the tabs are up.
enum DebugLaunch {
    private static func value(of argument: String) -> String? {
        #if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        guard let index = arguments.firstIndex(of: argument), arguments.indices.contains(index + 1) else { return nil }
        return arguments[index + 1]
        #else
        return nil
        #endif
    }

    static var skipsNotificationPrompt: Bool {
        #if DEBUG
        return ProcessInfo.processInfo.arguments.contains("-CEASkipNotificationPrompt")
        #else
        return false
        #endif
    }

    static var syncsAtStart: Bool {
        #if DEBUG
        return ProcessInfo.processInfo.arguments.contains("-CEADebugSync")
        #else
        return false
        #endif
    }

    static var login: (email: String, password: String)? {
        guard let value = value(of: "-CEADebugLogin"), let colon = value.firstIndex(of: ":") else { return nil }
        return (String(value[..<colon]), String(value[value.index(after: colon)...]))
    }

    static var tab: MainTab? {
        switch value(of: "-CEADebugTab") {
        case "home": .home
        case "lendings": .lendings
        case "lending": .lending
        case "activities": .activities
        case "management": .management
        case "profile": .profile
        default: nil
        }
    }

    static var destination: (any Destination)? {
        switch value(of: "-CEADebugOpen") {
        case "settings": DestinationSettings.shared
        case "security": DestinationSecurity.shared
        default: nil
        }
    }
}
