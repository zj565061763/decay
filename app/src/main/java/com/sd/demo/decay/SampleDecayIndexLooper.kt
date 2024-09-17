package com.sd.demo.decay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sd.demo.decay.theme.AppTheme
import com.sd.lib.decay.FDecayIndexLooper

private const val SIZE = 5

class SampleDecayIndexLooper : ComponentActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContent {
         AppTheme {
            Content()
         }
      }
   }
}

@Composable
private fun Content(
   modifier: Modifier = Modifier,
) {
   val coroutineScope = rememberCoroutineScope()
   val looper = remember(coroutineScope) { FDecayIndexLooper(coroutineScope) }
   val uiState by looper.uiStateFlow.collectAsStateWithLifecycle()

   Column(
      modifier = modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
   ) {
      Buttons(
         onClickStart = {
            looper.startLoop(
               size = SIZE,
               initialIndex = 0,
            )
         },
         onClickDecay = {
            looper.startDecay(3)
         },
      )

      Spacer(modifier = Modifier.height(20.dp))

      ListView(selectedIndex = uiState.currentIndex)
   }

   LaunchedEffect(uiState.state) {
      logMsg { "state:${uiState.state}" }
   }
}

@Composable
private fun Buttons(
   modifier: Modifier = Modifier,
   onClickStart: () -> Unit,
   onClickDecay: () -> Unit,
) {
   Row(
      modifier = modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
   ) {
      Button(onClick = onClickStart) {
         Text(text = "开始")
      }
      Spacer(modifier = Modifier.width(10.dp))
      Button(onClick = onClickDecay) {
         Text(text = "减速")
      }
   }
}

@Composable
private fun ListView(
   modifier: Modifier = Modifier,
   selectedIndex: Int?,
) {
   LazyColumn(
      modifier = modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
   ) {
      items(SIZE) { index ->
         Button(
            onClick = { },
            enabled = index == selectedIndex,
         ) {
            Text(text = index.toString())
         }
      }
   }
}