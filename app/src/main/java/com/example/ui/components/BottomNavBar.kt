package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BarraTab
import com.example.ui.theme.BarraCyanPrimary
import com.example.ui.theme.BarraTextSecondary

@Composable
fun BarraBottomNavBar(
    activeTab: BarraTab,
    onTabSelected: (BarraTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        val navItems = listOf(
            Triple(BarraTab.HOME, "HOME", Icons.Default.Home),
            Triple(BarraTab.PHOTO, "FOTO", Icons.Default.PhotoLibrary),
            Triple(BarraTab.VIDEO, "VIDEO", Icons.Default.VideoLibrary),
            Triple(BarraTab.FILE, "FILE", Icons.Default.Folder)
        )

        navItems.forEach { (tab, label, icon) ->
            val isSelected = activeTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BarraCyanPrimary,
                    selectedTextColor = BarraCyanPrimary,
                    indicatorColor = BarraCyanPrimary.copy(alpha = 0.15f),
                    unselectedIconColor = BarraTextSecondary,
                    unselectedTextColor = BarraTextSecondary
                ),
                modifier = Modifier.testTag("nav_${label.lowercase()}")
            )
        }
    }
}
