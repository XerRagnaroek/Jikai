# Jikai (次回)
Jikai is a discord bot made using [JDA](https://github.com/DV8FromTheWorld/JDA) and parsing all its anime data from [AniList](https://anilist.co/) (*not affiliated in any way!*) using my own api wrapper (probably on github soon).  
Her main purpose is to allow users to subscribe to currently airing anime and be notified when a new episode releases or anything changes, while also syncing your AniList account!

Join [here!](https://discord.gg/Q42Aesn)  
Jikai will start a short setup process with you, after which you'll be set to use her service.
## Features
### Centralised lists for the three title languages
You can change you subscriptions here, simply by clicking the respective button!

**Romaji**

![List example Romaji](https://raw.githubusercontent.com/XerRagnaroek/Jikai/dev/doc/listRomajiExample.jpg)
___
**English**

![List example English](https://raw.githubusercontent.com/XerRagnaroek/Jikai/dev/doc/listEnglishExample.jpg)
___
**Native**

![List example Native](https://raw.githubusercontent.com/XerRagnaroek/Jikai/dev/doc/listNativeExample.jpg)  

I'm slowly moving away from reactions and towards using buttons to handle user input, which is why these images are outdated.

### Notifications at configurable times
Exemplary notifications:

1 hour before the next episode airs

![1 hour prior notification](https://raw.githubusercontent.com/XerRagnaroek/Jikai/dev/doc/nextEpHourMsg.jpg)

At release

![release message](https://raw.githubusercontent.com/XerRagnaroek/Jikai/dev/doc/releaseExample.jpg)

### Links to streaming sites
So far only Gogoanime links are supported (because it's the site I use). Other services such as Proxer and KickAssAnime will be added in future updates.

### Tracking of unwatched episodes
Never lose track of episodes you haven't watched yet! Jikai can tell you which eps you're still missing, including links to their release messages and streaming links.  
When you've watched an episode, just click on 👀 to mark is as such!

![ep watched example](https://raw.githubusercontent.com/XerRagnaroek/Jikai/dev/doc/epWatchedExample.gif)

### Daily overview of your anime
![daily overview](https://raw.githubusercontent.com/XerRagnaroek/Jikai/dev/doc/dailyExample.jpg)

### Weekly release schedule
![weekly schedule](https://raw.githubusercontent.com/XerRagnaroek/Jikai/dev/doc/weeklyExample.jpg)

### Integration with your AniList account
You can link your AniList account with Jikai! She'll read your *Planning* and *Watching* list and sync your subs with them.  
Jikai can also be authorized to edit your profile: update anime progress and add or remove animes from lists!

### Sharing and linking subscriptions with other users
Every user has a unique export key that can be used to share their subs or link them to other users.  
Linking subs means that whenever the link target subscribes, you'll also be subscribed to that anime. This can be done both ways, perfect for watching and planning a season with a friend!  
The subscription list also allows you to sub and unsub directly, no need to go to the big list!  
![sub list](https://raw.githubusercontent.com/XerRagnaroek/Jikai/dev/doc/importSubsExample.gif)

### Lots of configurable options
* Timezone for localised dates and times
* Your prefered way how anime titles are displayed (Romaji, English or Native)
* Your notification times up to a week
* Jikai's language (only english atm cause but more can easily be added!)
* Enable or disable any feature you don't like