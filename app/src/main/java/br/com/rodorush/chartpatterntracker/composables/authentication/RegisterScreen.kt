package br.com.rodorush.chartpatterntracker.composables.authentication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.rodorush.chartpatterntracker.R

@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    // Estados para os campos
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var acceptTerms by remember { mutableStateOf(false) }

    // Layout principal
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Ícones de navegação e logo no topo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.baseline_arrow_back_24), // Substitua pelo seu ícone de voltar
                    contentDescription = "Back"
                )
            }
            // Logo à direita
            Icon(
                painter = painterResource(id = R.drawable.logo), // Substitua pelo seu recurso de logo
                contentDescription = "Logo",
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(48.dp)
            )
        }

        // Coluna central com conteúdo
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Create your account",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Campo: Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("ex: jon smith") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Campo: Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                placeholder = { Text("ex: jon.smith@email.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default
            )

            // Campo: Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                placeholder = { Text("********") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            // Campo: Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm password") },
                placeholder = { Text("********") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            // Checkbox + Texto: Terms & policy
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = acceptTerms,
                    onCheckedChange = { acceptTerms = it }
                )
                Text(
                    text = "I understood the terms & policy.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Botão SIGN UP
            Button(
                onClick = {
                    // Aqui você pode tratar a validação e registro
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = acceptTerms // Exemplo: habilita o botão somente se aceitar os termos
            ) {
                Text(text = "SIGN UP")
            }

            // Texto "or sign up with"
            Text(
                text = "or sign up with",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .alpha(0.7f)
            )

            // Seção de ícones (Google, Facebook, Twitter)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Substituir pelos ícones adequados
                Icon(
                    painter = painterResource(id = R.drawable.google_g_logo),
                    contentDescription = "Google",
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { /* Ação de login Google */ },
                    tint = Color.Unspecified // Caso queira manter as cores originais
                )
                Icon(
                    painter = painterResource(id = R.drawable.f_facebook),
                    contentDescription = "Facebook",
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { /* Ação de login Facebook */ },
                    tint = Color.Unspecified
                )
                Icon(
                    painter = painterResource(id = R.drawable.x_logo),
                    contentDescription = "Twitter",
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { /* Ação de login Twitter */ },
                    tint = Color.Unspecified
                )
            }
        }

        // Texto no rodapé para já ter conta
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Have an account?")
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "SIGN IN",
                style = TextStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.Underline
                ),
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }
    }
}