-- Initialize db_games MySQL Database
START TRANSACTION;

SET NAMES utf8mb4;  

DELETE FROM healthcheck;
INSERT INTO healthcheck (msg) VALUES ('Hello world from DB!');

DELETE FROM users;
INSERT INTO users (user_id, password_hash, login, full_name) VALUES
  (1,'','shelly','Sheldon'),
  (2,'','lenny','Leonard'),
  (3,'','raj','Rajesh'),
  (4,'','howie','Howard');


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

      ("panel.game.sudoku", "Sudoku", "en"),
      ("panel.game.connect4", "Connect Four", "en"), ("panel.game.connect4", "Vier gewinnt", "de"), 
      ("panel.game.connect4", "Četiri u nizu", "hr"),  
      ("panel.game.memory", "Memory", "en"),


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
    "Munich", 2 ),
  ( "008102405310000900754000001000000240020710090000400008000047000007593000905260000",
    "698172435312654987754389621871935246426718593539426718263847159147593862985261374",
    "Dublin", 2 ),
  ( "850470000340060200900500460069100820000600035500000670100024500030010000000700090",
    "856472913341869257972531468769153824284697135513248679197324586638915742425786391",
    "Amsterdam", 2 ),


  ( "005100004200098700089600512000309000000050000000000271908760000004000080006845309",
    "635127894241598763789634512812379456467251938593486271928763145354912687176845329",
    "Zagreb", 2 );



COMMIT;
