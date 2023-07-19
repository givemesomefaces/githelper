# GitHelper

<a href="https://www.jetbrains.com"><img src="https://resources.jetbrains.com/storage/products/company/brand/logos/jb_beam.svg" width = "10%" /></a>
<a href="https://www.jetbrains.com/idea"><img src="https://resources.jetbrains.com/storage/products/company/brand/logos/IntelliJ_IDEA_icon.svg" width = "10%" /></a>

<!--- ![Build](https://github.com/Lv-lifeng/GitHelper/workflows/Build/badge.svg) --->
[![Version](https://img.shields.io/jetbrains/plugin/v/18328.svg)](https://plugins.jetbrains.com/plugin/18328)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/18328.svg)](https://plugins.jetbrains.com/plugin/18328)

<!-- Plugin description -->

## GitHelper is a plugin for JetBrains IDEs, such as IntelliJ IDEA, that enables Git or GitLab operations on multiple repositories simultaneously.

## Features:

- Supports checking out the same branch for multiple local or remote repositories simultaneously.
- Supports deleting branches for multiple local or remote repositories simultaneously.
- Supports cloning multiple repositories from GitLab simultaneously.
- Supports creating Gitlab merge requests for the same branch across multiple repositories simultaneously.
- Supports closing Gitlab merge requests for multiple repositories simultaneously.
- Supports merging Gitlab merge requests for multiple repositories simultaneously.
- Supports tagging multiple GitLab repositories with the same tag simultaneously.

### Git operation in right toolwindow is called GitHelper.

### GitLab operation:

1. Configure Gitlab server in Settings -> Version Control -> GitHelper.
2. Use batch operations in top menu Git -> GitLab.
3. Or in git menu of right mouse button.[Create Merge Request..., Merge Request...],supports select multiple projects.

---

## 功能:

- 支持从本地、远程同时为多个项目checkout同一分支。
- 支持从本地、远程同时为多个项目删除同一分支。
- 支持从GitLab同时clone多个项目。
- 支持同时为多个项目创建相同分支的GitLab合并请求。
- 支持同时关闭多个项目的GitLab合并请求。
- 支持同时合并多个项目的GitLab合并请求。
- 支持同时为多个Gitlab项目创建相同的标签。

### Git批操作在右侧菜单GitHelper中

### GitLab操作:

1. 在Settings -> Version Control-> GitHelper中配置
2. 顶部菜单Git -> GitLab中使用
3. 或选中多个项目，在右键git菜单中(Create Merge Request...,Merge Request...)

<img src="https://s1.ax1x.com/2022/03/13/bbC7od.gif">  
<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "GitHelper"</kbd> >
  <kbd>Install Plugin</kbd>

- Manually:

  Download the [latest release](https://github.com/Lv-lifeng/GitHelper/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
