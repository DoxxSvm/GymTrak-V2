package `in`.gym.trak.studio.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import gym.composeapp.generated.resources.*
import org.jetbrains.compose.resources.Font

object AppTextTheme {

    @Composable
    fun BricolageGrotesque() = FontFamily(
        Font(Res.font.bricolage_grotesque_light, FontWeight.Light),
        Font(Res.font.bricolage_grotesque_regular, FontWeight.Normal),
        Font(Res.font.bricolage_grotesque_medium, FontWeight.Medium),
        Font(Res.font.bricolage_grotesque_semi_bold, FontWeight.SemiBold),
        Font(Res.font.bricolage_grotesque_bold, FontWeight.Bold)
    )

    val bold: TextStyle
        @Composable get() = TextStyle(
            fontFamily = BricolageGrotesque(),
            fontWeight = FontWeight.Bold,
            color = Black,
            fontSize = 16.sp
        )

    val light: TextStyle
        @Composable get() = TextStyle(
            fontFamily = BricolageGrotesque(),
            fontWeight = FontWeight.Light,
            color = Black,
            fontSize = 16.sp
        )

    val medium: TextStyle
        @Composable get() = TextStyle(
            fontFamily = BricolageGrotesque(),
            fontWeight = FontWeight.Medium,
            color = Black,
            fontSize = 16.sp
        )

    val regular: TextStyle
        @Composable get() = TextStyle(
            fontFamily = BricolageGrotesque(),
            fontWeight = FontWeight.Normal,
            color = Black,
            fontSize = 16.sp
        )

    val semiBold: TextStyle
        @Composable get() = TextStyle(
            fontFamily = BricolageGrotesque(),
            fontWeight = FontWeight.SemiBold,
            color = Black,
            fontSize = 16.sp
        )

    val BrandingTitle: TextStyle
        @Composable get() = bold.copy(
            fontSize = 38.sp,
            letterSpacing = 1.2.sp,
            color = PrimaryColor
        )

    val PageHeadline: TextStyle
        @Composable get() = bold.copy(
            fontSize = 32.sp,
            lineHeight = 38.sp,
            color = White
        )

    val FeatureCardTitle: TextStyle
        @Composable get() = bold.copy(
            fontSize = 14.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.5.sp,
            color = White
        )
}
