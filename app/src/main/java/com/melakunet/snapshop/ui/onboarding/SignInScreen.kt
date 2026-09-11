package com.melakunet.snapshop.ui.onboarding

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melakunet.snapshop.R
import com.melakunet.snapshop.ui.theme.Brand
import com.melakunet.snapshop.ui.theme.Spacing

@Composable
fun SignInScreen(onSignedIn: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brand.backgroundLight)
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = "App Icon",
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brand.accentDark.copy(alpha = 0.1f))
        )
        
        Spacer(Modifier.height(Spacing.xxl))

        Text(
            "Sign In to Continue",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            ),
            color = Brand.textPrimaryLight,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(Spacing.lg))

        Text(
            "Your scan history and saved items stay on your device. Sign in lets us verify your identity — nothing more.",
            style = MaterialTheme.typography.bodyLarge,
            color = Brand.textSecondaryLight,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.md)
        )

        Spacer(Modifier.height(Spacing.xxxl))

        Button(
            onClick = { 
                Toast.makeText(context, "Google Sign-In arrives with Play Store release", Toast.LENGTH_SHORT).show()
                onSignedIn()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Brand.textPrimaryLight, contentColor = Color.White),
            shape = RoundedCornerShape(Spacing.md)
        ) {
            Text("Sign in with Google", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(Spacing.md))

        OutlinedButton(
            onClick = onSignedIn,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
            shape = RoundedCornerShape(Spacing.md)
        ) {
            Text("Continue in demo mode", color = Brand.textPrimaryLight)
        }

        Spacer(Modifier.height(Spacing.xxl))

        Text(
            "Your data is never sold.",
            style = MaterialTheme.typography.labelSmall,
            color = Brand.textSecondaryLight
        )
    }
}
