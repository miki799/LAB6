package com.example.lab6

import android.content.Context
import android.net.http.SslError
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.lab6.ui.theme.LAB6Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/** Instantiate the interface and set the context.  */
class WebAppInterface(private val mContext: Context) {
    /** Show a toast from the web page.  */
    @JavascriptInterface
    fun showToast(toast: String) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show()
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Uncomment to use interface written in JavaScript
        // create WebView
        val myWebView = WebView(applicationContext)
        // enable JS
        myWebView.settings.javaScriptEnabled = true
        // Inject Java class to the WebView
        myWebView.addJavascriptInterface(WebAppInterface(this), "Android")
        // Create html page
        val unencodedHtml = """
            <html>
                <body>
                    <input type="button" value="Click me!" onClick="showAndroidToast('Hacked!')" />
                    <script type="text/javascript">
                        function showAndroidToast(toast) {
                            Android.showToast(toast);
                        }
                    </script>
                </body>
            </html>
        """
        val encodedHtml = Base64.encodeToString(unencodedHtml.toByteArray(), Base64.NO_PADDING)
        myWebView.loadData(encodedHtml, "text/html", "base64")
        setContentView(myWebView)


        // Uncomment to use interface written in Kotlin
//        setContent {
//            LAB6Theme {
//                // A surface container using the 'background' color from the theme
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    App({
//                        sendGetRequest()
//                    })
//
//                }
//            }
//        }
    }

    private var trustAllCerts: Array<TrustManager> = arrayOf<TrustManager>(
        object : X509TrustManager {
            override fun checkClientTrusted(
                certs: Array<X509Certificate?>?, authType: String?
            ) {
            }

            override fun checkServerTrusted(
                certs: Array<X509Certificate?>?, authType: String?
            ) {
            }

            override fun getAcceptedIssuers():
                    Array<X509Certificate> {
                return emptyArray()
            }
        }
    )

    private fun sendGetRequest() {
        GlobalScope.launch {
            // uncomment this to use TrustManager
//            val sc = SSLContext.getInstance("SSL")
//            sc.init(null, trustAllCerts, SecureRandom())
//            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)

            val url = URL("http://jsonplaceholder.typicode.com/posts")
            val con: HttpURLConnection = url.openConnection() as HttpURLConnection
            con.requestMethod = "GET"

            val inputStream = BufferedReader(
                InputStreamReader(con.inputStream)
            )
            var inputLine: String?
            val content = StringBuffer()
            while (inputStream.readLine().also { inputLine = it } != null) {
                content.append(inputLine)
            }
            inputStream.close()

            con.disconnect()

            Log.d("send_get_request", content.toString())
        }
    }


}

@Composable
fun App(sendPostRequest: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Click the button and check logcat!",
            modifier = modifier
        )
        Button(
            modifier = modifier,
            onClick = {
                sendPostRequest()
            }
        ) {
            Text(stringResource(R.string.post_request))
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    // Konfiguracja klienta WebViewClient do ignorowania błędów SSL
                    webViewClient = object : WebViewClient() {
                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: SslErrorHandler,
                            error: SslError?
                        ) {
                            handler.proceed()
                            // Ignorowanie błędów certyfikatu SSL
                        }
                    }
                }
            },
            update = { webView ->
                // Ładowanie strony Google w WebView
                webView.loadUrl("https://www.google.com")
            }
        )
    }
}