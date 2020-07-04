# <img alt="Unofficial PS client icon" src="web/ic_launcher-web.png" width="48"></img>Android PS Client
[![Actions Status](https://github.com/MajeurAndroid/Android-Unofficial-Showdown-Client/workflows/Build/badge.svg)](https://github.com/MajeurAndroid/Android-Unofficial-Showdown-Client/actions)

## Content
* [Introduction](#introduction)
* [Technical overview](#technical-overview)
* [Notes](#notes)
* [Contributing](#contributing)
  * [**Report a bug**](#report-a-bug)
* [Credits](#credits)
* [Licence](#licence)

## Introduction
This repo contains source code for **unofficial** **P**okemon**S**howdown Client app. 
This client is now written mostly in kotlin and works through android framework. It was formerly written in java so you'll still encounter some objects written in java.
> **Why developing a native app when _play.pokemonshowdown.com_ is supporting mobile devices ?**
*Well, web client does its best to support small screen sizes (and it does it pretty well) but the user experience is far away from what a native app is able to provide.*
#### What Android PS Client is ?
The goal of this app is to provide a client with a convenient and suitable UI for mobile devices without loosing any part of the experience you would have on official client (eg. tips, types, stats etc...). 

#### What Android PS Client is not ?
This client is not, and will never be, a replacement for the desktop/web client. It's more like a companion app to be able to play PS everywhere, without needing your desktop computer.

## Technical overview
### Code structure
Here I will briefly describe packages along with their main components to help you finding out how each components are interacting together to make this app work.

#### Tree:
`com.majeur.psclient`
 - [`.ui`](psclient/src/main/java/com/majeur/psclient/ui)
 	- [`.teambuilder`](psclient/src/main/java/com/majeur/psclient/ui/teambuilder)
	 	- [`.TeamBuilderActivity.kt`](psclient/src/main/java/com/majeur/psclient/ui/teambuilder/TeamBuilderActivity.kt)
			*Regular android activity in charge of setting up navigation framework.*
	 	- [`.*Fragment.kt`](psclient/src/main/java/com/majeur/psclient/ui/teambuilder)
			*Self explanatory name.*
	- [`.MainActivity.kt`](psclient/src/main/java/com/majeur/psclient/ui/MainActivity.kt)
		*In charge of setting up fragments and managing connection with ShowdownService.*
	- [`.*Fragment.kt`](psclient/src/main/java/com/majeur/psclient/ui)
		*Responsible of UI management by implementing their associate [`*MessageObserver`](psclient/src/main/java/com/majeur/psclient/service) callbacks (except for [`TeamsFragment`](psclient/src/main/java/com/majeur/psclient/ui/TeamsFragment.kt) that works without registering any observer to [`ShowdownService`](psclient/src/main/java/com/majeur/psclient/service/ShowdownService.kt)).*
	- [`.*Dialog.kt`](psclient/src/main/java/com/majeur/psclient)
	*Self explanatory name.*
 - [`.service`](psclient/src/main/java/com/majeur/psclient/service)
	*Everything related to Showdown's protocol will be found in this package. From shodown server communication to handling and processing of incoming data.*
	- [`.ShowdownService.kt`](psclient/src/main/java/com/majeur/psclient/service/ShowdownService.kt)
		*Responsible of all interactions with showdown server, including authentication.*
	- [`.AbsMessageObserver.kt`](psclient/src/main/java/com/majeur/psclient/service/AbsMessageObserver.kt)
		*Base definition for a component that would handle messages from showdown server.*
	- [`.GlobalMessageObserver.kt`](psclient/src/main/java/com/majeur/psclient/service/GlobalMessageObserver.kt)
		*In charge of handling global server messages such as '|challstr|', '|popup|', '|formats|' etc... and rooms initialization.*
	- [`.RoomMessageObserver.kt`](psclient/src/main/java/com/majeur/psclient/service/RoomMessageObserver.kt)
		*Handles everything for a chat room to work.*
	- [`.BattleMessageObserver.kt`](psclient/src/main/java/com/majeur/psclient/service/BattleMessageObserver.kt)
		*Extends from `.RoomMessageObserver.kt` and adds support for battle commands.*
	- *[...]*
 - [`.io`](psclient/src/main/java/com/majeur/psclient/io)
	*Contains everything involving loading content (eg. from disk for dex/move/poke details and dex icons or from web for sprites).*
	- [`.AssetLoader.kt`](psclient/src/main/java/com/majeur/psclient/io/AssetLoader.kt)
		*Helper class for loading content easily, with caching etc... Defines suspend functions for loading any type of asset using Kotlin coroutines.*
	- [`.BattleTextBuilder.java`](psclient/src/main/java/com/majeur/psclient/io/BattleTextBuilder.java)
		*Provides formatted text according to an action and its effect (and/or its origin).*
	- [`.BattleAudioManager.java`](psclient/src/main/java/com/majeur/psclient/io/BattleAudioManager.java)
		*Handles everything related to audio playback during a battle.*
	- *[...]*
 - [`.model`](psclient/src/main/java/com/majeur/psclient/model)
	*Contains every classes representing data.*
	- *[...]*
 - [`.widget`](psclient/src/main/java/com/majeur/psclient/widget)
	*Contains every custom android UI components.*
	- *[...]*
- [`.util`](psclient/src/main/java/com/majeur/psclient/util)
	*Contains various utility classes.*
	- [`.html`](psclient/src/main/java/com/majeur/psclient/util/html)
		*Utilities for displaying basic html using native android text framework. Mainly used for |html| and |raw| room messages.*
		- [`.Html.java`](psclient/src/main/java/com/majeur/psclient/util/html/Html.java)
			*Entry point for using html utilities*
	- [`.SmogonTeamParser.kt`](psclient/src/main/java/com/majeur/psclient/util/SmogonTeamParser.kt)
		*Responsible for parsing raw text teams formatted with Smogon standards.*
	- [`.SmogonTeamBuilder.kt`](psclient/src/main/java/com/majeur/psclient/util/SmogonTeamBuilder.kt)
		*Responsible for building text teams formatted with Smogon standards.*
	- *[...]*
### Core mechanisms
I'll describe here some of the core mechanisms through quick flow graphs.
#### Incomming data flow
![Basic incomming data flow](https://g.gravizo.com/svg?%20digraph%20G%20%7B%0A%20%20%20%20rankdir%3DLR%3B%0A%20%20%20%20node%20%5Bshape%3Dbox%5D%3B%0A%20%20%20%20n1%20%5Blabel%3D%22Showdown%5Cnserver%22%20style%3Ddotted%5D%3B%0A%20%20%20%20n2%20%5Blabel%3D%22Showdown%20service%5Cn%E2%80%94%5CnReads%20raw%20data%20from%20socket%2C%20then%5Cncreates%20ServerMessage%20object%20and%5Cndispatch%20it%20to%20observers%22%5D%20%3B%0A%20%20%20%20n3%20%5Blabel%3D%22Message%20observers%5Cn%E2%80%94%5CnProcesses%20server's%20messages%20and%20define%5Cnabstract%20methods%20for%20UI%20updates%22%5D%20%3B%0A%20%20%20%20n4%20%5Blabel%3D%22Fragments%5Cn%E2%80%94%5CnImplements%20observer's%20abstract%5Cnmethods%20to%20update%20UI%20state%22%5D%20%3B%0A%0A%20%20%20%20n1%20-%3E%20n2%20%5Blabel%3D%22ws%5Cnprotocol%22%20dir%3Dboth%20style%3Ddotted%5D%3B%0A%20%20%20%20n2%20-%3E%20n3%20%5Blabel%3D%22registration%5Cnmechanism%22%5D%3B%0A%20%20%20%20n3%20-%3E%20n4%20%5Blabel%3D%22abstract%5Cnmethods%22%5D%3B%0A%7D)
#### Outcomming data flow
![Basic outcomming data flow](https://g.gravizo.com/svg?%20digraph%20G%20%7B%0Arankdir%3DLR%3B%0Anode%20%5Bshape%3Dbox%5D%3B%0An1%20%5Blabel%3D%22Showdown%5Cnserver%22%20style%3Ddotted%5D%3B%0An2%20%5Blabel%3D%22Showdown%20service%5Cn%E2%80%94%5CnFormats%20and%20sends%5Cndata%20to%20server%20through%20the%20socket%22%5D%20%3B%0An3%20%5Blabel%3D%22Fragments%5Cn%E2%80%94%5CnCreates%20appropriate%20command%5Cnwith%20its%20arguments%22%5D%20%3B%0An4%20%5Blabel%3D%22UI%5Cn%E2%80%94%5CnReacts%20to%20user's%20inputs%22%5D%20%3B%0An5%20%5Blabel%3D%22Initialization%5Cn%E2%80%94%5CnMakes%20authentication%20related%5Cncommands%20at%20launch%22%5D%20%3B%0An5%20-%3E%20n3%20%5Blabel%3D%22%22%5D%3B%0An4%20-%3E%20n3%20%5Blabel%3D%22view%20callbacks%22%5D%3B%0An3%20-%3E%20n2%20%5Blabel%3D%22direct%20call%5Cn(bound%20service)%22%5D%3B%0An2%20-%3E%20n1%20%5Blabel%3D%22ws%5Cnprotocol%22%20dir%3Dboth%20style%3Ddotted%5D%3B%0A%7D)
## Notes
### Limitations
#### Multiple battles at the same time
For now, this client is designed to handle only one battle at a time. This because I think it makes more sense in app's UI design :), but mainly because I think it's not that relevant on a mobile device to run multiple battle like you would do on desktop. 
Anyway, I wrote code in a way that leaves the option to implement such feature in the future.
#### Doubles, Triples and other types
For now, only single and double battles are implemented. The code allows triple battle but no testing has been done, so it works 'as is'.
### Data files
For maintainability reasons and to keep app binary file size as low as possible, I tried to retrieve data from showdown server as much as possible. But, for heavily/recurrent accessed data, such as poke/move details, json files are kept locally to ensure a very low loading time. Dex icons are also stored locally to allow region only icon decoding, avoiding us to load the entire icon sheet.
A download of these files at first launch might be implemented in the future.
### Web protocols
Every single http connection established by this client is using secured http protocol (`https:`). For WebSocket, `wss:` is now used since alpha3.
### Android string resources
~~I was a bit lazy on this one...~~ Most UI strings are hard-coded and aren't placed in the regular `res/values/string.xml`. This because Showdown is only available in English and has no localization implementation planned for now. So a Showdown client with localization would be completely pointless, and would lead into a partially 'translated' application.
## Contributing
Any help is very welcomed! Please make sure you are respecting the coding patterns and please strongly test your modifications before PR!
### Report a bug
 - Submit a bug report: [here](https://forms.gle/tqSeeZ9De3ik97CK8)
 - Reported bugs tracking: [here](https://docs.google.com/spreadsheets/d/1oC0m5SJEqx9HMXOAIHcgoa92B2CP69SmSuwHKR7v-X0/edit?usp=sharing)
## Credits
 - Everybody on our [Smogon thread](https://www.smogon.com/forums/threads/alpha02-need-testers-unofficial-showdown-android-client.3654298): For the huge help with bug reporting
 - [Zarel](https://github.com/Zarel): For PokemonShowdown itself.
 - [NamTThai](https://github.com/NamTThai): For some piece of java code translated from js ~~I reuse here~~.
 - [http://pokemonshowdown.com/credits](http://pokemonshowdown.com/credits)
 - Type icons: [DevianArt](https://www.deviantart.com/majeur01/art/Pokemon-Types-Icons-819866719)
## Licence
> Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file or any in this project except in compliance with the License.
   You may obtain a copy of the License at
> http://www.apache.org/licenses/LICENSE-2.0
> Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
