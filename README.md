# ScalaTwitter - Team Sierra #

## Build & Run ##
Prerequisites:
Scala, Scalatra, sbt should be installed.

```sh
$ cd scalatraproject
$ sbt
> jetty:start
```

Manually open [http://localhost:8080/](http://localhost:8080/) in your browser.


## Postman script ##
https://www.getpostman.com/collections/55f8a9063d7867c067f8

There are number of requests that can be executed one by one or independently. 
Registration and singin should be executed first, after executing signin postman local variable with token will be created - token1 for user1 and token2 for user2. Then those tokens will be available among the other requests.


## Functionality ##

●	post(“/register”) – pass to this method email, nickname, password of a User to be registered. Email and nickname should be unique.

●	post(“/signin”) – pass to this method email and password. User should be registered.

●	post(“/signout”) – sign out, user should be logged in.

●	post(“/messages”) – the server anticipated to receive the message encoded in JSON with “text”. It will add the message to the collection of messages and will return 200 HTTP code.

●	put(“/messages/:id”) – the server anticipates to receive the message encoded in JSON with one field “text”. If the message with the specified id already exists in the collection, the server will update the text of this message with the new one, if not – return 404 HTTP code.

●	delete(“/messages/:id”) – the server will delete from the collection the message with the specified id, if it exists, if not – it will return 404 HTTP code.

●	post(“/retweet”) – the server anticipated to receive the message encoded in JSON with “id”. It will add the message to the collection of retweets and will return 200 HTTP code.

●	delete("/retweet/:id") - the server will delete from the collection the retweet with the specified id, if it exists, if not – it will return 404 HTTP code.

●	post(“/like”) – the server anticipated to receive the message encoded in JSON with “id”. It will add the message to the collection of retweets and will return 200 HTTP code. Dislike form this message will be removed if it was there.

●	post(“/dislike”) – the server anticipated to receive the message encoded in JSON with “id”. It will add the message to the collection of retweets and will return 200 HTTP code. Like form this message will be removed if it was there.

●	post(“/removelike/:id”) - remove user’s like/dislike of the twit with the specified id.

●	post(“/subscribe”) – the server anticipated to receive the message encoded in JSON with “id” of user to subscribe. Corresponding tuple will be added to the subscriptions.

●	get("/feed") - returns feed of the user.

●	post("/mention") - pass “twit_id” and “nickname” to this method.

●	get("/mentions_feed") - returns list of messages in which user was mentioned when the corresponding setting was on.

●	post("/mentions_setting/:value") - value should be “on” or “off” to disable or enable mentioning of user in messages.



## Requirements
You can store everything in memory, since it could be a trouble to use persistent storage (but if you
want and can use it)

● User

  &nbsp;&nbsp;&nbsp;❍ User has: id, email, nickname, password
  
  &nbsp;&nbsp;&nbsp;❍ id and email should be unique
  
● Twit

&nbsp;&nbsp;&nbsp;❍ Twit should have: id, text, author, submission time

&nbsp;&nbsp;&nbsp;❍ id should be unique

&nbsp;&nbsp;&nbsp;❍ [Optional] Twit has likes, dislikes, see Functionality section too

## Functionality

● A guest can register and then sign in (he is User from that moment), sign out

&nbsp;&nbsp;&nbsp;❍ For sessions use JWT-tokens, since it is common to use them now, cookies is option too, but
JWT is better for reasons

● User can create twits and only edit and destroy his twits

&nbsp;&nbsp;&nbsp;❍ [Optional] Implement mentioning of other people by his nickname

● User can subscribe to other users

● User can get his feed that consists of other users twits

● User can get other users feed (to decide if he want to subscribe to that user)

● User can re-tweet other users twits, so subscribers will get this twit (author should be mentioned)
in their feeds

&nbsp;&nbsp;&nbsp;❍ He also should be able to remove his re-twit

&nbsp;&nbsp;&nbsp;❍ If author will remove his twit, all re-twits of that twit should not be visible too

● [Optional] User can like or dislike a twit

&nbsp;&nbsp;&nbsp;❍ If he likes a twit and then dislike it, like is removed and dislike is added. This should works vice
versa too

&nbsp;&nbsp;&nbsp;❍ User can also just remove his like or dislike

## Criteria of done

● Code on github

● Project could be built using instructions in your README.md in repository

● Make a small report that points out what was done (which functionality is working). Report must
be no more than one page A4 (pdf).

&nbsp;&nbsp;&nbsp;❍ If you haven't managed something to work please write why, so we can cover this material
during the course.

&nbsp;&nbsp;&nbsp;❍ Use Postman to test your API and publish your collection, add link in your report

&nbsp;&nbsp;&nbsp;❍ [Optional] If you used some additional tools/materials/guides, please mention them in the
report, it could be helpful for other students and try to connect the problem you was solving
and material that you was using.
