import SwiftUI

/// The club's colours, from the Material scheme of the Compose app (`ui/theme/Colors.kt`), so both UIs look alike.
enum Brand {
    /// Gold/olive: `#6D5E0F` in light mode, `#DBC66E` in dark mode.
    static let primary = Color(light: 0x6D5E0F, dark: 0xDBC66E)
}

extension Color {
    init(light: UInt32, dark: UInt32) {
        self.init(uiColor: UIColor { traits in
            let value = traits.userInterfaceStyle == .dark ? dark : light
            return UIColor(
                red: CGFloat((value >> 16) & 0xFF) / 255,
                green: CGFloat((value >> 8) & 0xFF) / 255,
                blue: CGFloat(value & 0xFF) / 255,
                alpha: 1
            )
        })
    }
}
