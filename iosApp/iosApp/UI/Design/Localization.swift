import SwiftUI

/// Strings with placeholders (`%1$@`, `%1$lld`), which `Text("key \(value)")` can't look up: SwiftUI would look for
/// a key with the value's placeholder in its name, and ours are named after the Compose resource.
///
/// The text follows the environment's locale, which is how the app's own language setting reaches it.
enum L10n {
    static func string(_ key: String, _ args: [CVarArg], locale: Locale) -> String {
        let format = bundle(for: locale).localizedString(forKey: key, value: key, table: nil)
        return String(format: format, locale: locale, arguments: args)
    }

    private static func bundle(for locale: Locale) -> Bundle {
        guard let code = locale.language.languageCode?.identifier,
              let path = Bundle.main.path(forResource: code, ofType: "lproj"),
              let bundle = Bundle(path: path)
        else { return .main }
        return bundle
    }
}

/// A localized string with placeholders: `LText("register_code_message", email)`.
struct LText: View {
    private let key: String
    private let args: [CVarArg]
    @Environment(\.locale) private var locale

    init(_ key: String, _ args: CVarArg...) {
        self.key = key
        self.args = args
    }

    var body: Text {
        Text(verbatim: L10n.string(key, args, locale: locale))
    }
}
