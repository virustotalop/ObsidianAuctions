<div align="center">
<h1>ObsidianAuctions</h1>

<p>An auction plugin for spigot 1.12.2-1.19.3, you can find it <a href="https://www.spigotmc.org/resources/obsidianauctions.5595/">here on spigot!</a></p>
</div>

## FAQ
* The plugin doesn't work on my server?
  * What version is your server on? Is it one of the supported versions listed above. We also only support the lastest version of any major version for example we support 1.13.2 but we do not support 1.13.1 or 1.13.
* What is ObsidianAuctions?
  * Obsidian auctions is a chat based auction plugin, and it is fork of the FloAuctions plugin.
* I have a bug can you fix it?
  * Please file an [issue](https://github.com/virustotalop/ObsidianAuctions/issues/new/choose)
* I have a feature can you make it?
  * File an [issue](https://github.com/virustotalop/ObsidianAuctions/issues/new/choose). 

 ## Differences
 
Below are some differences between ObsidianAuctions and the old FloAuctions plugin
 
* Auction queue GUI
* Full UUID support
* Configurable language support
* Spawner & Mob Egg Support
* Support for modern Minecraft 1.12-1.19
* Easier to configure language file
  * Support for [minimessage](https://docs.adventure.kyori.net/minimessage.html#format)
* Configurable disallowed auction areas 
 
 
## How to file a bug report
Please post all relevant stacktraces to either [pastebin.com](pastebin.com) or [hastebin.com](hastebin.com).
Provide as much detail as possible to resolve the bug the fastest.
If you have any programming experience if you could take a look at the code and see if you can find the possible fix it would be appreciated, as the project is just maintained by myself virustotalop and contributions code or otherwise are very much appreciated.

## How to submit a pull request
Make a fork of master and submit a pull request into the refactor branch making sure that the code compiles against spigot 1.8+.
If any of the code does not compile the pull request will either be rejected or you will be asked to update the code.
For any new classes follow the style of other classes follow. Make sure to sign the [contributors file](CONTRIBUTORS.md) when you submit the pull request. Need help with markdown? Refer to [this project for help.](https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet)


## Development

### Eclipse

1. Git clone the project
2. Generate eclipse files with `gradlew eclipse`
3. Import project

### Intellij

1. Git clone the project
2. Generate intellij files with `gradlew idea`
3. Import project

### Building

`gradlew build`
