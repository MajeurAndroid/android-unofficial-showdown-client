# <img alt="Unofficial PS client icon" src="https://github.com/MajeurAndroid/Android-Unoffical-Showdown-Client/blob/master/web/ic_launcher-web.png" width="48"></img>Android PS Client

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
This client is written in native java and works through android/java framework.
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
 - *`(root)`*
	- `.MainActivity.java`
	*In charge of setting up fragments and managing connection with ShowdownService.*
	- `.*Fragment.java`
	*Responsible of UI management by implementing their associate `.*MessageObserver.java` callbacks.*
	- `.*Dialog.java`
	*Self explanatory name.*
	- *[...]*
 - `.service`
	*Everything related to Showdown's protocol will be found in this package. From shodown server communication to handling and processing of incoming data.*
	 - `.ShowdownService.java`
		*Responsible of all interactions with showdown server, including authentication.*
	- `.MessageObserver.java`
		*Base definition for a component that would handle messages from showdown server.*
	- `.GlobalMessageObserver.java`
		*In charge of handling global server messages such as '|challstr|', '|popup|', '|formats|' etc... and rooms initialization.*
	- `.RoomMessageObserver.java`
		*Handles everything for a chat room to work.*
	- `.BattleMessageObserver.java`
		*Extends from `.RoomMessageObserver.java` and adds support for battle commands.*
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
		*Responsible for parsing raw text teams formatted with Smogon standards.*
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
For now, only single battles are implemented. The code is kept flexible enough to allow easy double/triple implementation. If double battles are implemented at the time you're reading this, it means I underestimated my motivation.
### Data files
For maintainability reasons and to keep app binary file size as low as possible, I tried to retrieve data from showdown server as much as possible. But, for heavily/recurrent accessed data, such as poke/move details, json files are kept locally to ensure a very low loading time. Dex icons are also stored locally to allow region only icon decoding, avoiding us to load the entire icon sheet.
A download of these files at first launch might be implemented in the future.
### Web protocols
Every single http connection established by this client is using secured http protocol (`https:`). For WebSocket, I tried to use `wss:` protocol but it didn't worked right away. In the future switching to `wss:` would be nice, but for now unsecured `ws:` is ok.
### Android string resources
~~I was a bit lazy on this one...~~ Most of UI strings are hard-coded and aren't placed in the regular `res/values/string.xml`. This because Showdown is only available in English and has no localization implementation planned for now. So a Showdown client with localization would be completely pointless, and would lead into a partially 'translated' application.
## Contributing
Any help is very welcomed! Please make sure you are respecting the coding patterns and please strongly test your modifications before PR!
### Report a bug
 - Submit a bug report: [here](https://forms.gle/tqSeeZ9De3ik97CK8)
 - Reported bugs tracking: [here](https://docs.google.com/spreadsheets/d/1oC0m5SJEqx9HMXOAIHcgoa92B2CP69SmSuwHKR7v-X0/edit?usp=sharing)
## Credits
 - [Zarel](https://github.com/Zarel): For PokemonShowdown itself.
 - [NamTThai](https://github.com/NamTThai): For some piece of java code translated from js I reuse here (such as team parser).
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
