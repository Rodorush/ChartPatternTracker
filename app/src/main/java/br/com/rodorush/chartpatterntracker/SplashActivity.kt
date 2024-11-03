package br.com.rodorush.chartpatterntracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.rodorush.chartpatterntracker.ui.theme.ChartPatternTrackerTheme
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChartPatternTrackerTheme {
                SplashScreen {
                    navigateToNextScreen()
                }
            }
        }
    }

    private fun navigateToNextScreen() {
        val intent = if (userIsLoggedIn()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, AuthenticationActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    private fun userIsLoggedIn(): Boolean {
        return false
    }
}

val poppinsBold = FontFamily(
    Font(R.font.poppins_bold, FontWeight.Bold)
)

val poppinsSemiBold = FontFamily(
    Font(R.font.poppins_semibold, FontWeight.SemiBold)
)

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3000)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_image),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(top = 380.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = stringResource(id = R.string.app_name).uppercase(),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = poppinsBold,
                color = Color.White,
                lineHeight = 37.4.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(80.dp))
            Text(
                text = stringResource(id = R.string.splash_slogan),
                fontSize = 30.sp,
                fontFamily = poppinsSemiBold,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = Color(0xFF1A5A99),
                lineHeight = 33.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    ChartPatternTrackerTheme {
        SplashScreen {}
    }
}