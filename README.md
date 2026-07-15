# SkarvexAuthentication 

Skarvex is a lightweight authentication system for Minecraft networks running on Velocity and Paper.

The project is split into multiple modules, such as:
- **auth-core** - contains shared business logic and interfaces
- **auth-velocity** - contains authentication proxy plugin with command and database integration (MySQL only)
- **auth-paper** - contains gameplay restrictions before authentication (world protection, lobby location, and player visibility)


## Features
- Player registration (/register)
- Player login (/login)
- BCrypt password hashing
- Session management (/logout)
- Auto-login based on trusted IP
- Login timeout protection
- IP-based brute-force protection with Caffeine Caching to reduce database load
- Password change support (/changepass)
- MySQL storage and HikariCP as a connection pooling

## Screenshots

<table>
  <tr>
    <td align="center">
      <b>Login</b><br>
      <a href="docs/login_skarvex.gif">
        <img src="docs/login_skarvex.gif" width="450">
      </a>
    </td>
    <td align="center">
      <b>Register</b><br>
      <a href="docs/registration_skarvex.gif">
        <img src="docs/registration_skarvex.gif" width="450">
      </a>
    </td>
  </tr>
  <tr>
    <td align="center">
      <b>Logout</b><br>
      <a href="docs/logout_skarvex.gif">
        <img src="docs/logout_skarvex.gif" width="450">
      </a>
    </td>
    <td align="center">
      <b>Change Password</b><br>
      <a href="docs/change_password_skarvex.gif">
        <img src="docs/change_password_skarvex.gif" width="450">
      </a>
    </td>
  </tr>
</table>

## Structure

```
auth-core 
├─ services 
├─ repositories 
├─ security 
├─ utils
└─ models 

auth-velocity 
├─ commands 
├─ listeners 
├─ database 
└─ schedulers 

auth-paper 
├─ listeners 
├─ managers 
└─ services (gameplay restrictions)
```

## Commands

- **/register [password]** - Register a new account
- **/login [password]** - Login to the server
- **/logout** - Revoke trusted session
- **/changepass [old] [new]** - Change the password

## Security
[//]: # (- OUT OF ORDER!!!!! Login attempts are limited and can be configured in config.yml. )
- Temporary IP blocking after too many failed attempts (600 seconds/10 minutes default).
- Sessions are stored in memory and cleared on disconnect.
- Passwords are hashed using BCrypt.

## Requirements

- Java 21
- Velocity 3.x (and higher)
- Paper 1.20+
- MySQL

## Building

```./gradlew build```

Compiled jars will be available in:
```
auth-paper/build/libs/
auth-velocity/build/libs/
```

## License

This project is licensed under the MIT License.

You are free to use, modify, distribute, and contribute to the project in accordance with the license terms.

[See the LICENSE file for details.](/LICENSE)

## Author

Developed and maintained by nobodyinvited.

GitHub: https://github.com/nobodyinvited

## Contributing

Contributions, bug reports, and feature suggestions are welcome.

If you find an issue or would like to propose an improvement, feel free to open an issue or submit a pull request.
