# sama-service

Backend service for the Sama app.

## API specification

```
[POST] /api/auth/google-authorize

Begin OAuth2 ritual via Google. Caller must open the `authorizationUrl` in a browser to continue
with the authorization process.

Request:
{}

Response [200]:
{
    "authorizationUrl": "https://accounts.google.com/o/oauth2/auth?access_type=offline&client_id=..."
}

Response [500]:
{
}
```

```
[GET] /api/auth/google-oauth2?code=xyz&error=xyz

Callback for the Google OAuth2 ritual. Must include either `code` or `error` request parameter.
If successful, user credentials are stored and a pair of Sama JWTs are issued for further requests.


Response [200]:
{
    "accessToken": "some-jwt",
    "refreshToken": "some-jwt",
}

Response [500]:
{
}
```