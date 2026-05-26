# 诸葛期指 (ZhuGe Futures)

**[English](#english) | [中文](#chinese)**

<a name="chinese"></a>
## 简介 (Introduction in Chinese)
**诸葛期指** 是一款使用 Kotlin 和 Jetpack Compose 开发的现代原生 Android 商业级应用。它旨在为期货交易者提供直观的大盘行情查看、模拟交易、实时价格预警以及自选持仓管理功能。通过简洁大气、高度扁平化的原生视觉设计，为用户提供沉浸式的移动端金融交易体验。

**本项目是一个完全开源的项目 (Open Source Project)**，欢迎任何形式的代码贡献和使用体验反馈。

### 核心功能体系
- **行情追踪 (Market Tracking)**: 支持按图表类型 (5分钟、15分钟、日K等) 查看实时绘制的K线图。
- **自定义自选 (Watchlist)**: 内置搜索功能快速查找标的（如纳指、原油、黄金等）并加入自选列表。
- **模拟交易 (Paper Trading)**: 支持进行做多/做空模拟开仓，可以自定义手数和杠杆倍率并实时计算浮亏浮盈 (PnL)。
- **智能预警 (Price Alerts)**: 可对指定合约设定价格警报，在涨破或跌破特定价格时进行提示。

---

<a name="english"></a>
## Introduction (in English)
**ZhuGe Futures** is a modern, native Android application built entirely with Kotlin and Jetpack Compose. It aims to provide futures traders with an intuitive platform for monitoring market trends, executing paper trades, setting up real-time price alerts, and managing a personalized portfolio. It features a clean, highly flat, and atmospheric UI design to ensure an immersive financial tracking experience.

**This is a fully Open-Source project.** We welcome contributions, feedback, and forks from the community.

### Key Features
- **Real-Time K-Line Charts**: Interactive candlestick charts with multiple timeframes (5m, 15m, Hourly, Daily).
- **Custom Watchlist**: Search and add various assets (like NASDAQ, Crude Oil, Gold, etc.) to your customized watchlist.
- **Paper Trading Simulator**: Execute simulated Long and Short positions. Select your order size and leverage margin to analyze real-time Profit and Loss (PnL).
- **Smart Alerts**: Configure conditional triggers (above/below targets) to get notified when assets break through key price levels.

## Tech Stack
- **Language**: Kotlin 
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM
- **Navigation**: Navigation Compose
