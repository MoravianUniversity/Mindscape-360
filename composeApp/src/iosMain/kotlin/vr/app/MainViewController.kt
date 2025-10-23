package vr.app

import App
import androidx.compose.ui.window.ComposeUIViewController
import cardboard.MainViewControllerWrapper

fun MainViewController() = MainViewControllerWrapper(ComposeUIViewController { App() })