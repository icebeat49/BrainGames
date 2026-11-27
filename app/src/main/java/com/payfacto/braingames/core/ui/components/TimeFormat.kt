package com.payfacto.braingames.core.ui.components

/** Play time as `mm:ss`; minutes keep counting past 59. */
fun formatElapsed(seconds: Long): String = "%02d:%02d".format(seconds / 60, seconds % 60)
