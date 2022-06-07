# Twitch chat bot vanilla java example

In order to try to find out how to read twitch chat messages with java,
I've created for myself this repo.

I'm happy to share it here.

## Prerequirements
Make sure to install
* maven
* java 11+
* an access_token
  * You can get one by calling [link](https://id.twitch.tv/oauth2/authorize?response_type=token&client_id=f1sjgf9y0ytdx2re1oapwfs7l11lh3&redirect_uri=http://localhost&scope=chat%3Aread+chat%3Aedit) and copying it from the url after pressing "Authorize"

```
https://id.twitch.tv/oauth2/authorize?response_type=token&client_id=f1sjgf9y0ytdx2re1oapwfs7l11lh3&redirect_uri=http://localhost&scope=chat%3Aread+chat%3Aedit
```

## Usage

When you check out this repo, run 
```
mvn clean install
```

Afterwards you can run the programm from commandline

```bash
cd target/classes/
java at.mlem.twitch.chatreader.Main https://www.twitch.tv/mlem86 <putInYourAccessTokenHere> someone

```