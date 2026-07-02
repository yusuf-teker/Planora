import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.plugin

fun test(client: HttpClient) {
    val auth = client.plugin(Auth)
    println(auth::class.java.methods.joinToString("\n") { it.name })
}
