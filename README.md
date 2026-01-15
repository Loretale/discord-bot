## Loretale Discord Bot

This is the main Loretale discord bot. It handles tickets, applications, and logging for moderation purposes.
It also handles some other small things such as blocking certain irrelevant emotes from being reacted to messages.

### Developer setup

The discord bot uses a small number of environment variables:

```
DATABASE_USER=dev
DATABASE_PASSWORD=devpassword
DATABASE_URL=jdbc:postgresql://localhost:5432/loretale
DISCORD_TOKEN=YOURTESTINGTOKENHERE
```

The database used is PostgreSQL. For more information on the development setup, please consult
https://github.com/Loretale/dev-setup.