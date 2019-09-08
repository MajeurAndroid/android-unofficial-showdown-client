# Android PS Client
![Android PS Client Icon](https://github.com/MajeurAndroid/Android-Unoffical-Showdown-Client/blob/master/web_hi_res_512.png)
## Intro
This repo contains source code for the **unofficial** **P**okemon**S**howdown Client app. 
This client is written in native java and works through android/java framework.
> **Why developing a native app when _play.pokemonshowdown.com_ is supporting mobile devices ?**
*Well, web client does its best to support small screen sizes (and it does it pretty well) but the user experience is far away from what a native app is able to provide.*
#### What Android PS Client is ?
The goal of this app is to provide a client with a convenient and suitable UI for mobile devices without loosing any part of the experience you would have on official client (eg. tips, types, stats etc...). 

#### What Android PS Client is not ?
This client is not, and will never be, a replacement for the desktop/web client. It's more like a companion app to be able to play PS everywhere, without needing your desktop computer.

## Contributing
Any help is very welcomed! Please make sure you are respecting the coding patterns and please strongly test your modifications before PR!

Here I will briefly describe packages and *[only]* their main components to help you finding out how each components are interacting together to make this app works.
#### Structure:
 - *`(root)`*
	- `.MainActivity.java`
	*In charge of setting up fragments and establishing connection with ShowdownService. Also allows fragments to register their MessageObserver to ShowdownService.*
	- `.*Fragment.java`
	*Responsible of UI management by implementing callbacks defined by their associate `.*MessageObserver.java`*
	- `.*Dialog.java`
	*Self explanatory name.*
	- *[...]*
 - `.service`
	*Showdown's protocol related logic must be handled exclusively in this package (exception obviously made for .io and .model)*
	 - `.ShowdownService.java`
		*Responsible of all interactions with showdown server, including authentication.*
	- `.MessageObserver.java`
		*Base definition for a component that would handle messages from showdown server.*
	- `.GlobalMessageObserver.java`
		*In charge of handling global server messages such as '|challstr|', '|popup|', '|formats|' etc... and rooms initialization.*
	- `.RoomMessageObserver.java`
		*Handles everything for a chat room to work*
	- `.BattleMessageObserver.java`
		*Extends from RoomMessageObserver and adds support for all battle commands.*
	- *[...]*
 - `.io`
	*Contains everything involving loading content (eg. from disk for dex/move/poke details and dex icons or from web for sprites).*
	- `.BattleTextBuilder.java`
		*Provides formatted text according to an action and its effect (and/or its origin).*
	- `.DataLoader.java`
		*Base helper class for loading content easily, with caching etc...*
	- *[...]*
 - `.model`
	*Contains every class representing data.*
	- *[...]*
 - `.widget`
	*Contains every custom android UI components.*
	- *[...]*
- `.util`
	*Contains various utility classes.*
	- `.ShowdownTeamParser.java`
		*Responsible for parsing teams exported as text blocks formatted with Smogon standards*
	- *[...]*
## Notes
### Limitations
#### Multiple battle at same time
For now, this client is designed to handle only one battle at a time. This because I think it makes more sense in app's UI design :), but mainly because I think it's not that relevant on a mobile device to run multiple battle like you would do on desktop. 
Anyway, I wrote code in a way that leaves the option to implement such feature in the future.
#### Doubles, Triples and other types
For now, only single battles are implemented. The code is kept flexible enough to allow easy double/triple implementation. If double battles are implemented at the time you're reading this, it means I underestimated my motivation.
### Data files
For maintainability reasons and to keep app binary file size as low as possible, I tried to retrieve data from showdown server as much as possible. But, for heavily/recurrent accessed data, such as poke/move details, json files are kept locally to ensure a very low loading time. Dex icons are also stored locally to allow region only icon decoding, avoiding us to load the entire icon sheet.
A download of these files at first launch might be implemented in the future.
### Web protocols
Every single http connection established by this client is using secured http protocol (`https:`). For WebSocket, I tried to use `wss:` protocol but it didn't worked right away. In the future switching to `wss:` would be nice, but for now unsecured `ws:` is ok.
### Android string resources
~~I was a bit lazy on this one...~~ Most of UI strings are hard-coded and aren't placed in the regular `res/values/string.xml`. This because Showdown is only available in English and has no localization implementation planned for now. So a Showdown client with localization would be completely pointless, and would lead into a partially 'translated' application.
## Credits
 - [Zarel](https://github.com/Zarel): For PokemonShowdown itself.
 - [NamTThai](https://github.com/NamTThai): For some piece of java code translated from js I reuse here (such as team parser).
 - [http://pokemonshowdown.com/credits](http://pokemonshowdown.com/credits)


