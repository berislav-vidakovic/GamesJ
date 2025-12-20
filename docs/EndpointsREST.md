## REST API Endpoints

### GET /api/users/all

<details>
<summary>
Get all users, new client GUID and tech stack
</summary>

### Request

GET (no parameters)

### Response

```json
{
  id, 
  techstack: [],
  users: [{
    userId, login, fullName, isOnline, pwd
  }]
}
```

- id: backend-generated GUID
- techstack: String array with tech stack image URLs
- users: hashed password in pwd field

</details>

### POST /api/users/new

<details>
<summary>
Register new user
</summary>

### Request

POST request
- Parameter: id=clientGUID      
- Header: Authorization: Bearer accessToken 
- Body
  ```json
  { 
    register {
      login,
      fullname
      password
    } 
  }
  ```


### Response
- OK
  ```json
  {
    acknowledged: true, 
    user {
      userId, login, fullName, pwd, isOnline: false
    }
  }  
  ```

- Missing credentials, user exists or server error:
  ```json
  {
    acknowledged: false, error
  }
  ```

- WebSocket broadcast push
  ```json
  {
    type: userRegister,
    status: WsStatus.OK,
    data: restResponseOK
  }
  ```
</details>

