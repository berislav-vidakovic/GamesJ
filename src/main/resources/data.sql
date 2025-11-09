-- Initialize db_games MySQL Database
START TRANSACTION;

SET NAMES utf8mb4;  

DELETE FROM users;
INSERT INTO users (user_id, password_hash, login, full_name) VALUES
  (1,'hashS','shelly','Sheldon'),
  (2,'hashL','lenny','Leonard'),
  (3,'hashR','raj','Rajesh'),
  (4,'hashH','howie','Howard');


DELETE FROM localization;
INSERT INTO localization (paramkey, paramvalue, lang)
VALUES
      ("panel.title", "Game Panel", "en"), ("panel.title", "Spielpanel", "de"), ("panel.title", "Nadzorna Ploča", "hr"),
      ("panel.users", "Users", "en"), ("panel.users", "Benutzer", "de"), ("panel.users", "Korisnici", "hr"),
      ("panel.connect", "Connect", "en"), ("panel.connect", "Verbinden", "de"), ("panel.connect", "Poveži se", "hr"),

      ("panel.invite", "Invite", "en"), ("panel.invite", "Einladen", "de"), ("panel.invite", "Pozovi", "hr"),
      ("panel.cancel", "Cancel", "en"), ("panel.cancel", "Absagen", "de"), ("panel.cancel", "Poništi", "hr"),
      ("panel.accept", "Accept", "en"), ("panel.accept", "Annehmen", "de"), ("panel.accept", "Prihvati", "hr"),
      ("panel.reject", "Reject", "en"), ("panel.reject", "Ablehnen", "de"), ("panel.reject", "Odbij", "hr"),

      ("panel.run", "Run", "en"), ("panel.run", "Starten", "de"), ("panel.run", "Pokreni", "hr"),

      
      ("panel.game.sudoku", "Sudoku", "en"), ("panel.game.sudoku", "Sudoku", "de"), 
      ("panel.game.sudoku", "Sudoku", "hr"),
      ("panel.game.connect4", "Connect Four", "en"), ("panel.game.connect4", "Vier gewinnt", "de"), 
      ("panel.game.connect4", "Četiri u nizu", "hr"),


      ("panel.loginmsg1", "Logged in as: ", "en"), ("panel.loginmsg1", "Angemeldet als: ", "de"), 
      ("panel.loginmsg1", "Prijavljen si kao: ", "hr"),
      ("panel.loginmsg2", " user(s) online]", "en"), ("panel.loginmsg2", " Benutzer online]", "de"), 
      ("panel.loginmsg2", " korisnik(a) online]", "hr"),
      ("panel.loginmsg3", "You are not logged in", "en"), ("panel.loginmsg3", "Du bist nicht angemeldet", "de"), 
      ("panel.loginmsg3", "Nisi prijavljen", "hr"),
      
      ("panel.invitemsg", "You invited user:", "en"), ("panel.invitemsg", "Eingeladener Benutzer: ", "de"), 
      ("panel.invitemsg", "Pozvao si korisnika: ", "hr"),
      ("panel.invitedmsg", "You have invitation from: ", "en"), ("panel.invitedmsg", "Du hast eine Einladung von: ", "de"), 
      ("panel.invitedmsg", "Imaš poziv od korisnika: ", "hr"),
      ("panel.acceptmsg", "You accepted invitation!", "en"), ("panel.acceptmsg", "Du hast die Einladung angenommen!", "de"), 
      ("panel.acceptmsg", "Prihvatio si poziv!", "hr"),
      ("panel.acceptedmsg", "Invitation accepted!", "en"), ("panel.acceptedmsg", "Einladung angenommen!", "de"), 
      ("panel.acceptedmsg", "Poziv je prihvaćen!", "hr"),

      ("panel.signin", "Sign In", "en"), ("panel.signin", "Anmelden", "de"), ("panel.signin", "Prijava", "hr"),
      ("panel.signup", "Sign Up", "en"), ("panel.signup", "Registrieren", "de"), ("panel.signup", "Registracija", "hr"),
      ("panel.signout", "Sign Out", "en"), ("panel.signout", "Abmelden", "de"), ("panel.signout", "Odjava", "hr");


INSERT IGNORE INTO sudokuboards (board, solution,name,level) 
VALUES
      ( "080003000009150273000904100007649800000070300000030054703206400020000900000010530",
        "185723649649158273372964185537649821814572396296831754753296418421385967968417532",
        "Munich", 2 );


COMMIT;
