// App.tsx 
import sudokuImg from '../assets/sudoku.jpg';
import connect4Img from '../assets/connect4.png';
import memoryImg from '../assets/memory.png';
import tictactoeImg from '../assets/tictactoe.png';
import blackjackImg from '../assets/blackjack.png';
import mmImg from '../assets/mm.jpg';
import enImg from '@common/assets/en.png';
import deImg from '@common/assets/de.png';
import hrImg from '@common/assets/hr.png';

import './App.css';
import "@common/style.css";
import '@common/style-mobile.css';

import { URL_SUDOKU } from '@common/config';
import { loadCommonConfig, getTitle, getLocalization } from '@common/config';
import { useState, useEffect } from 'react';
import { connectWS } from '@common/webSocket';
import type { User } from '@common/interfaces';
import { setStateFunctionRefs, handleResponseGetAllUsers, handleWsMessage } from './messageHandlers';
import { getAllUsers, logoutUser, inviteUser, runGame } from './utils';
import RegisterDialog from './components/RegisterDialog.tsx' 
import LoginDialog from './components/LoginDialog.tsx' 
import InviteDialog from './components/InviteDialog.tsx' 
 

function App() {
  const [currentLang, setCurrentLangState] = useState<'en' | 'de' | 'hr' | null>(null);
  const [usersRegistered, setUsersRegistered] = useState<User[]>([]);
  const [isConfigLoaded, setConfigLoaded] = useState<boolean>(false);
  const [isInitialized, setInitialized] = useState<boolean>(false);
  const [isWsConnected, setWsConnected] = useState<boolean>(false);
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const [onlineUsers, setOnlineUsers] = useState<number>(0);
  const [showRegisterDialog, setShowRegisterDialog] = useState<boolean>(false);
  const [showLoginDialog, setShowLoginDialog] = useState<boolean>(false);
  const [showInviteDialog, setShowInviteDialog] = useState<boolean>(false);
  const [callerUserId, setCallerUserId] = useState<number | null>(null);
  const [calleeUserId, setCalleeUserId] = useState<number | null>(null);
  const [invitationState, setInvitationState] = useState<"init" | "sent" | "pending" | "paired">("init");
  const [selectedGame, setSelectedGame] = useState<"panel.game.sudoku" | "panel.game.connect4" | null>(null);
  const [localesLoaded, setLocalesLoaded] = useState(false);
  const [techStack, setTechStack] = useState<string[]>([]);


  useEffect( () => { 
    loadCommonConfig(setConfigLoaded);     
  }, []);

  useEffect( () => { if( isConfigLoaded){
      setStateFunctionRefs(setInitialized, setUsersRegistered, 
        setCurrentUserId, setOnlineUsers, setCallerUserId, setCalleeUserId,
        setInvitationState, setSelectedGame, setTechStack );      
      
      getAllUsers(handleResponseGetAllUsers );
      getLocalization().then(() => {
        setLocalesLoaded(true); // mark locales as loaded
      });
   }      
  }, [isConfigLoaded]); 

  useEffect( () => { if( isConfigLoaded && isInitialized){
      connectWS( setWsConnected, handleWsMessage );
   }      
  }, [isConfigLoaded, isInitialized]);

  //const handleSignIn = () => //console.log("Sign In clicked");
  const handleSignOut = () => { 
    logoutUser( currentUserId as number); 
    clearInvitations();
    setSelectedGame(null);
  }

  const handleInvite = () => { if( currentUserId && onlineUsers > 1) setShowInviteDialog(true); }
  
  
  const handleRespond = (accept: boolean) => {    
    //console.log("Respond to Invitation clicked: ", accept);
    if( accept )
      inviteUser( callerUserId as number, calleeUserId as number, "accept" );
    else
      inviteUser( callerUserId as number, calleeUserId as number, "reject" );
  }


  const handleRun = () => {
    //console.log("Run clicked");
    if( selectedGame == 'panel.game.connect4')
    {
      runGame(callerUserId as number, calleeUserId as number, selectedGame, currentUserId as number);
    }
    else if( selectedGame == 'panel.game.sudoku' )
      handleSelectGame( URL_SUDOKU);
    setInvitationState("init");
    setSelectedGame(null);
  }

  const handleSelectGame = (url: string) => {
    window.open(url, '_blank');
  };

  const handleCancelInvitation = () => {
    inviteUser(callerUserId as number, calleeUserId as number, "cancel");
  };

  

  const clearInvitations = (): void => {
    setCalleeUserId(null);  
    setCallerUserId(null);
    setInvitationState("init");
    //console.log(callerUserId, "called ", calleeUserId);
  }

  const isBtnVisibleSignIn = (): boolean => {
    return currentUserId == null && isWsConnected && 
      usersRegistered.some(u=>!u.isonline);
  }

  const isBtnVisibleSignOut = (): boolean => {
    return currentUserId != null && isWsConnected;
  }

  const isBtnVisibleSignUp = (): boolean => {
    return currentUserId == null && isWsConnected;
  }

  const isBtnVisibleRun = (): boolean => {
    return (invitationState == "paired" || selectedGame == "panel.game.sudoku")  
      && currentUserId != null && isWsConnected;
  }

  const isBtnVisibleInvite = (): boolean => {
    return invitationState == "init" && selectedGame == "panel.game.connect4" && 
          currentUserId != null && isWsConnected;
  }

  const isBtnVisibleCancel = (): boolean => {
    return invitationState == "sent" && isWsConnected;
  }

  const isBtnVisibleResponse = (): boolean => {
    return invitationState == "pending" && isWsConnected;
  }

  const isBtnVisibleConnect = (): boolean => {
    return !isWsConnected && isInitialized;
  }

  const setCurrentLanguage = (lang: 'en' | 'de' | 'hr'): void => {
    sessionStorage.setItem('currentLang', lang);
    console.log("Set current language to ", lang);
    setCurrentLangState(lang);
  }

  // TODO: Pass lang to each call of  getTitle("panel.users", currentLang) 

  return (
   <div className="app-container">
    {/* --- Users Box on the left --- */}
    <div className="users-box">  
      <h2>{localesLoaded ? getTitle("panel.users", currentLang) : "..."}:</h2>
        <ul>
        { usersRegistered.map((u) => ( 
          <li key={u.userId} className="user-item">               
            {u.fullname} 
            <span
              className={`status-dot ${
                u.isonline ? "status-online" : "status-offline"
              }`}
            ></span>
          </li>
          ))
        }        
      </ul>
    </div>

    {/* --- Right main content --- */}
    <div className="main-content">
      <div className="auth-buttons">        
        <img 
          src={enImg} 
          className = "flaglocales" 
          alt="English" 
          onClick={ ()=> setCurrentLanguage('en') }
        />
        <img 
          src={deImg} 
          className = "flaglocales" 
          alt="Deutsch" 
          onClick={ ()=> setCurrentLanguage('de') }
        />
        <img 
          src={hrImg} 
          className = "flaglocales" 
          alt="Hrvatski" 
          onClick={ ()=> setCurrentLanguage('hr') }
        />
      </div>
      {/* Top auth buttons */}
      <div className="auth-buttons">
        {
          !isInitialized && <p>
            System loading ...
          </p>
        }
        {isBtnVisibleConnect() && 
        <button
          onClick={()=>{ window.location.reload(); }}
        >
          { localesLoaded ? getTitle("panel.connect") : "..." }
        </button>}
        {isBtnVisibleSignUp() && <button 
          //onClick={handleSignUp}
          id="btnRegister" 
          onClick={() => setShowRegisterDialog(true)}
        >
          { localesLoaded ? getTitle("panel.signup") : "..." }
        </button>}

        {isBtnVisibleSignIn() && <button 
          id="btnLogin" 
          onClick={() => setShowLoginDialog(true)}          
        > 
          { localesLoaded ? getTitle("panel.signin"): "..." } 
        </button>}
        

        {isBtnVisibleSignOut() && <button 
          onClick={handleSignOut}
        > 
          { localesLoaded ? getTitle("panel.signout") : "..."}
        </button>}

        {isBtnVisibleRun() &&
        <button 
          onClick={handleRun}
        > { localesLoaded ? getTitle("panel.run") : "..."}  </button>}
          
        {isBtnVisibleInvite() &&
        <button 
          onClick={handleInvite}
        >{ localesLoaded ? getTitle("panel.invite") : "..."} </button>}

        {isBtnVisibleCancel() &&  <button 
          onClick={handleCancelInvitation}
        >{ localesLoaded ? getTitle("panel.cancel") : "..."} </button>}

        {isBtnVisibleResponse() && ( <>
          <button onClick={() => handleRespond(true)}>{ localesLoaded ? getTitle("panel.accept") : "..."} </button>
          <button onClick={() => handleRespond(false)}>{ localesLoaded ? getTitle("panel.reject") : "..."} </button>
        </>)}
      </div>
      

      <div className="tech-stack">
         Tech stack: &nbsp;
         {techStack && 
            techStack[0] == "text" 
            ? techStack[1]
            : techStack.map((img,idx)=>
              <img 
                key={idx} 
                src={img} 
                className = "flaglocales" 
              />
         )
        
         }
       
      </div>

      
      
      
    {selectedGame  
     ? <h2>{ localesLoaded ? getTitle( selectedGame) : "..."}</h2>
     : <h2 key={currentLang}>{ localesLoaded ? getTitle("panel.title") : "..."}</h2>
    }
      <div className='status-box'>
        { currentUserId != null  
           ? <p><b>{ localesLoaded ? getTitle("panel.loginmsg1") : "..."} {
                usersRegistered.find(u=>u.userId==currentUserId)!.fullname
              } </b>[{onlineUsers}{ localesLoaded ? getTitle("panel.loginmsg2") : "..."}</p>
           : <p>{ 
            localesLoaded ? getTitle("panel.loginmsg3") : "..."} 
            [{onlineUsers}{ localesLoaded ? getTitle("panel.loginmsg2") : "..."}</p>
        }
        {selectedGame && isWsConnected &&(
          <>
            { currentUserId && calleeUserId && callerUserId == currentUserId && 
              <p style={{fontWeight:"700", color:"#090"}}>
                  { localesLoaded ? getTitle("panel.invitemsg"): "..." } {usersRegistered.find(u=>u.userId==calleeUserId)!.fullname}
              </p> }
              { currentUserId && callerUserId && calleeUserId == currentUserId && 
              <p style={{fontWeight:"700", color:"#e00"}}>
                  { localesLoaded ? getTitle("panel.invitedmsg") : "..."} {usersRegistered.find(u=>u.userId==callerUserId)!.fullname}
              </p> }
              { currentUserId && invitationState == "paired" && calleeUserId == currentUserId && 
              <p style={{fontWeight:"700", color:"#00e"}}>
                  { localesLoaded ? getTitle("panel.acceptmsg"): "..." } 
              </p> }
              { currentUserId && invitationState == "paired" && callerUserId == currentUserId && 
              <p style={{fontWeight:"700", color:"#00e"}}>
                  { localesLoaded ? getTitle("panel.acceptedmsg") : "..."} 
              </p> } 
          </>              
          )}
          
        {/*<p>Game selected: Connect4</p>*/}
      </div>

      {/* Game buttons */}
      <div className="buttons-container">      
        <button 
          onClick={() => {
              if( !isConfigLoaded || !currentUserId ) {
                //console.log("Config not loaded (or no user logged in)");
                return;
              }
              if( selectedGame == 'panel.game.sudoku') handleSelectGame(URL_SUDOKU);
              else setSelectedGame('panel.game.sudoku');
              
            }}
          title={localesLoaded ? getTitle(selectedGame as string) : "..." }
          className={selectedGame === 'panel.game.sudoku' ? 'selected-button' : ''}
        >
          <img src={sudokuImg} alt="Sudoku" />
        </button>

        <button 
          onClick={() => {
            if( !isConfigLoaded || !currentUserId ) {
              //console.log("Config not loaded (or no user logged in)");
              return;
            }            
            else {
              //console.log('SELECTED Connect Four');
              setSelectedGame('panel.game.connect4');
            }
          }} 
          title={localesLoaded ? getTitle('panel.game.connect4') : "..."}
          className={selectedGame === 'panel.game.connect4' ? 'selected-button' : ''}
        >
          <img src={connect4Img} alt="Connect 4" />
        </button>
        <button 
          onClick={() => { //console.log("Memory is under construction..."); setSelectedGame(null);
            }            } 
          title="Memory">
          <img src={memoryImg} alt="Memory" />
        </button>
        <button 
          onClick={() => { //console.log("Master Mind is under construction..."); setSelectedGame(null);
            }            }
          title="Master Mind">
          <img src={mmImg} alt="Master Mind" />
        </button>
        <button 
          onClick={() => { //console.log("Tic Tac Toe is under construction..."); setSelectedGame(null);
            }}
          title="Tic Tac Toe">
          <img src={tictactoeImg} alt="Tic Tac Toe" />
        </button>
        <button onClick={() => { //console.log("Black Jack is under construction..."); setSelectedGame(null);
        }}
          title="Black Jack">
          <img src={blackjackImg} alt="Black Jack" />
        </button>
      </div>
      
    </div>
    {showRegisterDialog && (
      <RegisterDialog
        isWsConnected={isWsConnected}  
        setShowRegisterDialog={setShowRegisterDialog}
      />
    )}
    {showLoginDialog && usersRegistered.some(u=>!u.isonline) && (
      <LoginDialog
        setShowLoginDialog={setShowLoginDialog}
        usersRegistered={usersRegistered}  
        isWsConnected={isWsConnected}  
      />
    )}
     {showInviteDialog && onlineUsers > 1 && 
      currentUserId != null &&
     (
      <InviteDialog
        setShowInviteDialog={setShowInviteDialog}
        usersRegistered={usersRegistered}  
        isWsConnected={isWsConnected}  
        currentUserId={currentUserId}
        selectedGame={selectedGame}
      />
    )}

  </div>
  );
}

export default App;
