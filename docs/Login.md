## Login and Logout workflow

- Backend on startup sets all users to offline in DB (refresh tokens remain)

- On Login user and refresh Login
  - delete refresh token before renewd entry inserted
