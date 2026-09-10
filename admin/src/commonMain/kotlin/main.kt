import app.centrexcursionistalcoi.admin.App
import dev.kilua.BootstrapCssModule
import dev.kilua.BootstrapModule
import dev.kilua.TabulatorModule
import dev.kilua.startApplication

fun main() {
    startApplication(::App, BootstrapModule, BootstrapCssModule, TabulatorModule)
}
