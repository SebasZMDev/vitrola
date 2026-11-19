package com.sebas.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Brass = Color(0xFFB8924A)

/** Firma ZM: letras serif cursiva entre un filete grueso arriba y uno fino abajo, en latón. Discreta, para pies. */
@Composable
fun ZmSignature(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.width(56.dp).height(1.5.dp).background(Brass))
            Text(
                "ZM",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    fontSize = 24.sp,
                    color = Color(0xFFEDE6D3),
                ),
                modifier = Modifier.padding(vertical = 4.dp),
            )
            Box(Modifier.width(56.dp).height(0.6.dp).background(Brass))
        }
    }
}
