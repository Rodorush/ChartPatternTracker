package br.com.rodorush.chartpatterntracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.rodorush.chartpatterntracker.R

@Composable
fun ChartPatternDetailScreen(
    onNavigateBack: () -> Unit = {},
    patternId: String = ""
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White)
    ) {
        // Title
        Text(
            text = stringResource(R.string.chart_pattern_description),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Candlestick Illustration Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray.copy(alpha = 0.1f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                // Black Candlestick (First)
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(80.dp)
                        .background(Color.Black)
                )
                Spacer(modifier = Modifier.width(16.dp))
                // Gray Candlestick (Second)
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(80.dp)
                        .background(Color.Gray.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Plus Sign
                Text(
                    text = "+",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pattern Description
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = "Reversão Altista",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Blue
            )
            Text(
                text = "Doji Estrela da Manhã",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Confiança: Média",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Durante uma tendência de baixa, forma-se um longo candlestick preto, com gap de abertura no seguinte. No entanto, forma-se um pequeno candle de consolidação no segundo candle, cujo fechamento se dá no preço de abertura ou próximo disso. Esse cenário geralmente mostra que é possível haver um rali, já que muitas posições foram encerradas. A confirmação seria uma abertura mais alta no candlestick seguinte.",
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Back Button
        Button(
            onClick = { /* Handle back navigation */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(
                text = "Voltar",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}

// Preview function for Compose
@Preview(showBackground = true)
@Composable
fun ChartPatternDetailScreenPreview() {
    ChartPatternDetailScreen()
}