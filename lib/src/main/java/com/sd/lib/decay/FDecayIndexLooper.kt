package com.sd.lib.decay

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FDecayIndexLooper(
   private val coroutineScope: CoroutineScope = MainScope(),
   /** 匀速间隔 */
   private val linearInterval: Long = 100,
   /** 当减速间隔大于[maxDecayInterval]时停止循环 */
   private val maxDecayInterval: Long = linearInterval * 10,
   /** 计算减速增加的间隔 */
   private val decayIncreasedInterval: (interval: Long) -> Long = { (it * 0.3f).toLong() },
) {
   init {
      require(linearInterval > 0)
      require(maxDecayInterval > linearInterval)
   }

   data class UiState(
      /** 状态 */
      val state: State,
      /** 当前位置 */
      val currentIndex: Int,
   )

   enum class State {
      None,
      Linear,
      Decay,
      Finish,
   }

   /** 循环大小 */
   private var _size = 0
   /** 要停留的位置 */
   private var _stopIndex = -1

   private val _uiStateFlow = MutableStateFlow(
      UiState(
         state = State.None,
         currentIndex = 0,
      )
   )

   val uiStateFlow = _uiStateFlow.asStateFlow()

   val uiState: UiState
      get() = uiStateFlow.value

   /**
    * 开始在[0至(size-1)]之间无限循环
    * @return 本次调用是否有效
    */
   fun startLoop(
      /** 循环大小 */
      size: Int,
      /** 初始位置 */
      initialIndex: Int = 0,
   ) {
      coroutineScope.launch {
         val state = uiState.state
         if (state == State.None || state == State.Finish) {
            startInternal(size = size, initialIndex = initialIndex)
         }
      }
   }

   /**
    * 开始减速，并停留在[stopIndex]位置
    */
   fun startDecay(stopIndex: Int) {
      coroutineScope.launch {
         if (uiState.state == State.Linear) {
            _uiStateFlow.update {
               _stopIndex = stopIndex.coerceIn(0, _size - 1)
               it.copy(state = State.Decay)
            }
         }
      }
   }

   private suspend fun startInternal(
      /** 循环大小 */
      size: Int,
      /** 初始位置 */
      initialIndex: Int = 0,
   ) {
      if (size <= 0) return

      _uiStateFlow.update {
         _size = size
         UiState(
            state = State.Linear,
            currentIndex = initialIndex.coerceIn(0, size - 1),
         )
      }

      try {
         performLinear()
         performDecay()
         _uiStateFlow.update {
            it.copy(state = State.Finish)
         }
      } catch (e: Throwable) {
         _uiStateFlow.update {
            UiState(
               state = State.None,
               currentIndex = 0,
            )
         }
         if (e is CancellationException) throw e
      }
   }

   /**
    * 匀速
    */
   private suspend fun performLinear() {
      loop(
         loop = { uiState.state == State.Linear },
         delay = { delay(linearInterval) }
      )
   }

   /**
    * 减速
    */
   private suspend fun performDecay() {
      if (uiState.state != State.Decay) return

      val list = generateIntervalList()
      if (list.isEmpty()) {
         _uiStateFlow.update {
            it.copy(
               state = State.Finish,
               currentIndex = _stopIndex,
            )
         }
         return
      }

      var step = calculateStartDecayStep(
         size = _size,
         decayCount = list.size,
         currentIndex = uiState.currentIndex,
         stopIndex = _stopIndex,
      )
      loop(
         loop = { step > 0 },
         delay = {
            step--
            delay(linearInterval)
         }
      )

      var index = 0
      loop(
         loop = { index < list.size },
         delay = {
            val interval = list[index]
            index++
            delay(interval)
         },
      )
   }

   /**
    * 生成减速间隔
    */
   private fun generateIntervalList(): List<Long> {
      val list = mutableListOf<Long>()
      var interval = linearInterval
      while (true) {
         interval += decayIncreasedInterval(interval).also { check(it > 0) }
         if (interval > maxDecayInterval) {
            break
         } else {
            list.add(interval)
         }
      }
      return list
   }

   /**
    * 计算可以开始减速的步数
    */
   private fun calculateStartDecayStep(
      size: Int,
      decayCount: Int,
      currentIndex: Int,
      stopIndex: Int,
   ): Int {
      val appendIndex = currentIndex + (decayCount % size)
      val futureIndex = appendIndex.takeIf { it < size } ?: (appendIndex % size)
      return if (futureIndex <= stopIndex) {
         stopIndex - futureIndex
      } else {
         size - (futureIndex - stopIndex)
      }
   }

   /**
    * 循环
    */
   private inline fun loop(
      loop: () -> Boolean,
      delay: () -> Unit,
   ) {
      while (loop()) {
         delay()
         val nextIndex = (uiState.currentIndex + 1).takeIf { it < _size } ?: 0
         _uiStateFlow.update {
            it.copy(currentIndex = nextIndex)
         }
      }
   }
}