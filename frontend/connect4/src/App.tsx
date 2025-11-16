// App.tsx
import Board from "./Board";
import { sendPOSTRequest } from '@common/restAPI';
import { loadCommonConfig } from '@common/config';
import { useEffect, useState } from "react";
import { StatusCodes } from "http-status-codes";
import { handleWsMessage, setStateFunctionRefs, updateUserIds } from "./gameLogic";
import { connectWS } from '@common/webSocket';

function App() {
  const [isConfigLoaded, setConfigLoaded] = useState<boolean>(false);
  const [isGameInitialized, setGameInitialized] = useState<boolean>(false);
  const [isWsConnected, setWsConnected] = useState<boolean>(false);
  const [boardString, setBoardString] = useState<string>("YRY---------------------------------------");
  const [gameId, setGameId] = useState<string | null>(null);
  const [userId, setUserId] = useState<number | null>(null);
  const [userName, setUserName] = useState<string | null>(null);
  const [user2Name, setUser2Name] = useState<string | null>(null);
  const [myColor, setMyColor] = useState<"Red" | "Yellow" | null>(null); 
  const [gameState, setGameState] = 
    useState<"init" | "myMove" | "theirMove" | "draw" | "myWin" | "theirWin" | null>(null); 
  
  // Common config
  useEffect( () => { // After opening new Browser window
    loadCommonConfig(setConfigLoaded);     
    //console.log( "Loading Config ... ", isConfigLoaded);
    let gameID;
    let senderID;
    let refreshToken: string | null;
    let accessToken: string | null;
    const bReconnecting = sessionStorage.getItem("reconnecting") === "true";
    const params = new URLSearchParams(window.location.search);
    console.log( "Params:", params.get('gameId'), params.get('senderId'), "Reconnecting:", bReconnecting, 
     "refreshToken:", params.get('refreshToken'), "accessToken:", params.get('accessToken') );
    if( !bReconnecting) {
      gameID = params.get('gameId');
      senderID = params.get('senderId');
      refreshToken = params.get('refreshToken');
      accessToken = params.get('accessToken');
      sessionStorage.setItem("gameId", String(gameID));
      sessionStorage.setItem("senderId", String(senderID));
      sessionStorage.setItem("refreshToken", String(refreshToken) );
      sessionStorage.setItem("accessToken", String(accessToken) );
    }
    else {
      gameID = sessionStorage.getItem("gameId");
      senderID = sessionStorage.getItem("senderId");
      sessionStorage.setItem("reconnecting", "false");
    }
    console.log('Game ID:', gameID, "UserID:", senderID);
    setGameId(gameID);
    setUserId(Number(senderID));
    updateUserIds(Number(senderID), null, gameID);    
    setBoardString("YRY-----------------------------------YY--");
  }, []);

  // Common init
  useEffect( () => { 
    if( !isConfigLoaded || gameId == null ) return;
    const body = JSON.stringify({gameId, userId});
    console.log("SENDING POST to /api/games/init:", body);
    sendPOSTRequest( 'api/games/init', body, handleResponseInit);
    setStateFunctionRefs(setMyColor, setGameState);
  }, [isConfigLoaded, gameId]);
  // Req:  { gameId, userId } 
  // Resp: { gameId, id, userName, user2Id, user2Name }  
  async function handleResponseInit( jsonResp: any, status: number ) {
    console.log("POST init response:", jsonResp);
    if( status == StatusCodes.OK ){
      setGameState("init");
      setUserName( jsonResp.userName);
      setUser2Name(jsonResp.user2Name);
      updateUserIds(userId, Number(jsonResp.user2Id));    

      sessionStorage.setItem("myID", jsonResp.id);
      setGameInitialized(true);
      connectWS( setWsConnected, handleWsMessage );
    }
    else 
      alert(`Error: ${jsonResp.error} STATUS: ${status}`);
      //console.log( user2Id, user2Name, userName );
  }

  // Game-specific init
  useEffect( () => { 
    if( isGameInitialized && isWsConnected ) {
      //console.log( "--------Ready for Connect4 initilization");
      const body = JSON.stringify({gameId, userId});
      sendPOSTRequest( 'api/games/connect4/init', body, handleResponseConnect4Init);
    }
  }, [isGameInitialized, isWsConnected]);
  
  async function handleResponseConnect4Init( jsonResp: any, status: number ) {
    //console.log("POST init response:", jsonResp);
    // Req: {gameId, userId} Resp: {gameId, id, userName, user2Id, user2Name}
    if( status == StatusCodes.OK ){
      //console.log("My COLOR:", jsonResp.color);
      setMyColor(jsonResp.color);
    }
    else 
      alert(`Error: ${jsonResp.error} STATUS: ${status}`);
  }

  return (
    <div className = "connect4-container">
      <h2>Connect Four</h2>
       <div className="info-connect4">
        <p>
          You: <b>{userName}</b> 
        </p>
        <p>
          Against: <b>{user2Name}</b> 
        </p>
      </div>
     
      {<Board 
        boardString={boardString} 
        myColor={myColor}
        gameId={gameId}
        gameState={gameState}
        isWsConnected={isWsConnected}
      />
      }
    </div>
  );
}

export default App;