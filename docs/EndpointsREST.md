# REST API Endpoints


There are following Frontend projects and Request Endpoints:
- [Panel](#panel)
  - [GET /api/users/all](#get-apiusersall)
  - [POST /api/users/new](#post-apiusersnew)
- [Sudoku](#sudoku)
  - [GET /api/sudoku/board](#get-apisudokuboard)
  - [POST /api/sudoku/addgame](#post-apisudokuaddgame)
  - [POST /api/sudoku/setname](#post-apisudokusetname)
  - [POST /api/sudoku/solution](#post-apisudokusolution)
  - [POST /api/sudoku/tested](#post-apisudokutested)
- [Connect4](#connect4)

## Panel

### GET /api/users/all

<details>
<summary>
Get all users, new client GUID and tech stack
</summary>

### Request

GET 
- Parameter: id=(empty)      
- Header: Authorization: Bearer accessToken 

### Response

```js
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

POST 
- Parameter: id=clientGUID      
- Header: Authorization: Bearer accessToken 
- Body
  ```js
  { 
    register {
      login, fullname, password
    } 
  }
  ```


### Response
- OK - HttpStatus.CREATED (201)
  ```js
  {
    acknowledged: true, 
    user {
      userId, login, fullName, pwd, isOnline: false
    }
  }  
  ```

- Error
  - Missing credentials - HttpStatus.BAD_REQUEST (400)
  - User exists - HttpStatus.CONFLICT (409)
  - Server error - HttpStatus.INTERNAL_SERVER_ERROR (500)
    ```js
    { error }
    ```

- WebSocket broadcast push
  ```js
  {
    type: userRegister,
    status: WsStatus.OK,
    data: <restResponseOK>
  }
  ```
</details>

### POST /api/auth/refresh

<details>
<summary>
Login with refresh token. <br />
Called in Frontend <br />
  - on mount (browser refresh) - auto Login, if failed no Login Dialog (Login button click required) <br />
  - on Login button click - first try with refresh token, if failed Login Dialog raised to provide credentials <br />
On success: <br />
  - new accessToken and refreshToken issued
</summary>

### Request

POST 
- Parameter: id=clientGUID      
- Header: Authorization: Bearer accessToken 
- Body
  ```js
  { refreshToken }
  ```


### Response
- OK - HttpStatus.OK (200)
  ```js
  {
    accessToken,
    refreshToken,
    userId,
    isOnline: true
  }  
  ```

- Error
  - Missing clientGUID or Refresh token - HttpStatus.BAD_REQUEST (400)
  - Refresh token missing, invalid or expired, User not found - HttpStatus.UNAUTHORIZED (401)
  - Server error - HttpStatus.INTERNAL_SERVER_ERROR (500)
    ```js
    { error }
    ```

- WebSocket broadcast push
  ```js
  {
    type: userSessionUpdate,
    status: WsStatus.OK,
    data: <restResponseOK>
  }
  ```
</details>

### POST /api/auth/login

<details>
<summary>
Login with credentials. <br />
Called in Frontend <br />
  - on Login Dialog confirm click - after failed refreshLogin Login Dialog raised to provide credentials<br />
On success: <br />
  - new accessToken and refreshToken issued
</summary>

### Request

POST 
- Parameter: id=clientGUID      
- Header: Authorization: Bearer accessToken 
- Body
  ```js
  { userId, password }
  ```


### Response
- OK - HttpStatus.OK (200)
  ```js
  {
    accessToken,
    refreshToken,
    userId,
    isOnline: true
  }  
  ```

- Error
  - Missing clientGUID, userId or password - HttpStatus.BAD_REQUEST (400)
  - User not found - HttpStatus.NOT_FOUND (404)
  - Invalid password - HttpStatus.UNAUTHORIZED (401)
  - Server error - HttpStatus.INTERNAL_SERVER_ERROR (500)
    ```js
    { error }
    ```

- WebSocket broadcast push
  ```js
  {
    type: userSessionUpdate,
    status: WsStatus.OK,
    data: <restResponseOK>
  }
  ```
</details>


### POST /api/auth/logout

<details>
<summary>
Logout user
</summary>

### Request

POST 
- Parameter: id=clientGUID      
- Header: Authorization: Bearer accessToken 
- Body
  ```js
  { userId }
  ```


### Response
- OK - HttpStatus.OK (200)
  ```js
  {
    userId,
    isOnline: false
  }  
  ```

- Error
  - Missing clientGUID - HttpStatus.BAD_REQUEST (400)
  - Missing userId or User not found - HttpStatus.UNAUTHORIZED (401)
  - Server error - HttpStatus.INTERNAL_SERVER_ERROR (500)
    ```js
    { error }
    ```

- WebSocket broadcast push
  ```js
  {
    type: userSessionUpdate,
    status: WsStatus.OK,
    data: <restResponseOK>
  }
  ```
</details>

### GET /api/localization/get

<details>
<summary>
Get localization keys and values for languages currently present in DB
</summary>

### Request

GET 
- Parameter: id=clientGUID      
- Header: Authorization: Bearer accessToken 

### Response

```js
{
  locales: [{
    paramKey, paramValue, languange
  }]
}
```

</details>





## Sudoku


### GET /api/sudoku/board

<details>
<summary>
Get all valid sudoku boards
</summary>

### Request

GET 
- Parameter: id=clientGUID      
- Header: Authorization: Bearer accessToken 

### Response

```js
{ boards, valid, tested, allNames } 
```

- boards: array of all valid boards 
- valid: valid board count and percentage 
- tested: tested board count and percentage 
- allNames: array of all board names in DB


</details>


### POST /api/sudoku/addgame

<details>
<summary>
Add new game
</summary>

### Request

POST 
- Parameter: id=clientGUID      
- Header: Authorization: Bearer accessToken 
- Body
  ```js
  { board, name }
  ```
  - Field name is optional


### Response
- OK - HttpStatus.CREATED (201)
  ```js
  { name }
  ```
  - if name field not provided or empty - backend generates new GUID as name

- Error
  - Missing board in Request  - HttpStatus.BAD_REQUEST (400)
  - Existing board or existing name (if name provided in Request) - HttpStatus.CONFLICT (409)
    ```js
    { error }
    ```

</details>

### POST /api/sudoku/setname

<details>
<summary>
Set name for existing game
</summary>

### Request

POST 
- Parameter: id=clientGUID      
- Header: Authorization: Bearer accessToken 
- Body
  ```js
  { board, name }
  ```


### Response
- OK - HttpStatus.OK (200)
  ```js
  { board, name }
  ```

- Error
  - Missing board or name in Request - HttpStatus.BAD_REQUEST (400)
  - Board not found in DB - HttpStatus.NOT_FOUND (404)
  - Existing game with the same name - HttpStatus.CONFLICT (409)

    ```js
    { error }
    ```

</details>


### POST /api/sudoku/solution

<details>
<summary>
Update solution and name for existing game
</summary>

### Request

POST 
- Parameter: id=clientGUID      
- Header: Authorization: Bearer accessToken 
- Body
  ```js
  { board, solution }
  ```


### Response
- OK - HttpStatus.OK (200)
  ```js
  { board, solution, name }
  ```

- Error
  - Missing board and/or solution in Request - HttpStatus.BAD_REQUEST (400)
  - Board not found in DB - HttpStatus.NOT_FOUND (404)
    ```js
    { error }
    ```

</details>


### POST /api/sudoku/tested

<details>
<summary>
Update testedOK status and name after testing game 
</summary>

### Request

POST 
- Parameter: id=clientGUID      
- Header: Authorization: Bearer accessToken 
- Body
  ```js
  { board }
  ```


### Response
- OK - HttpStatus.OK (200)
  ```js
  { board, name }
  ```

- Error
  - Missing board in Request - HttpStatus.BAD_REQUEST (400)
  - Board not found in DB - HttpStatus.NOT_FOUND (404)
    ```js
    { error }
    ```

</details>


## Connect4


### POST /api/games/init

<details>
<summary>
Update testedOK status and name after testing game 
</summary>

### Request

POST 
- Parameter: id=clientGUID      
- Header: Authorization: Bearer accessToken 
- Body
  ```js
  { gameId, userId }
  ```


### Response
- OK - HttpStatus.OK (200)
  ```js
  { gameId, id, userName, user2Id, user2Name }
  ```

- Error
  - Missing board in Request - HttpStatus.BAD_REQUEST (400)
  - Board not found in DB - HttpStatus.NOT_FOUND (404)
    ```js
    { error }
    ```

</details>












